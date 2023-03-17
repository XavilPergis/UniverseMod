#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

uniform mat4 ModelViewMat;

uniform vec4 LightPos0;
uniform vec4 LightColor0;
uniform vec4 LightPos1;
uniform vec4 LightColor1;
uniform vec4 LightPos2;
uniform vec4 LightColor2;
uniform vec4 LightPos3;
uniform vec4 LightColor3;
uniform float MetersPerUnit;

uniform int IsGasGiant;
uniform float RenderingSeed;
uniform float Time;

in vec2 texCoord0;
in vec4 vertexPos;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
vec3 acesTonemap(vec3 color){	
	mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
	);
	mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
	);
	vec3 v = m1 * color;    
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return pow(clamp(m2 * (a / b), 0.0, 1.0), vec3(1.0 / 2.2));	
}

// Simplex 2D noise
//
vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ));
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

vec2 snoise2(vec2 v, float seed) {
	return vec2(snoise(v + 1.0) + seed, snoise(v - 1.0) - seed);
}

float saturate(float n) {
	return clamp(n, 0.0, 1.0);
}

vec3 contribution(vec4 color, vec4 pos) {
	if (color.a >= 0) {
		vec3 p0 = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;
		vec3 d0 = normalize(p0 - vertexPos.xyz);

		float distanceMeters = distance(vertexPos.xyz, p0) * MetersPerUnit;
		float d2 = 1.0 / pow(distanceMeters / 1e14, 2.0);
		float light = min(18.0, color.a * d2);

		return color.rgb * light * saturate(dot(normal.xyz, d0)) + 0.0025 * color.rgb;
	}
	return vec3(0);
}

float nfbm(vec2 v, int octaves, float baseAmplitude, float amplitudeFalloff, float baseFrequency, float frequencyFalloff) {
	float maxValue = 0.0;
	float currentValue = 0.0;
	float currentFrequency = baseFrequency;
	float currentAmplitude = baseAmplitude;
	for (int i = 0; i < octaves; ++i) {
		currentValue += currentAmplitude * snoise(currentFrequency * v);
		maxValue += currentAmplitude;
		currentFrequency *= frequencyFalloff;
		currentAmplitude *= amplitudeFalloff;
	}
	return currentValue / maxValue;
}

vec2 nfbm2(vec2 v, float seed, int octaves, float baseAmplitude, float amplitudeFalloff, float baseFrequency, float frequencyFalloff) {
	return vec2(
		nfbm(v + 1.2, octaves, baseAmplitude, amplitudeFalloff, baseFrequency, frequencyFalloff) + seed,
		nfbm(v - 1.7, octaves, baseAmplitude, amplitudeFalloff, baseFrequency, frequencyFalloff) - seed
	);
}

float sampleGasGiantBands(float t) {
	float amplitude = 1.4;
	float frequency = 5.0;
	float foo = floor(amplitude * snoise(vec2(frequency * t, 0.0)));
	return 0.5 + 0.5 * (foo / amplitude);
}

float gasGiantWarped(vec2 p, float seed) {
	vec2 q0 = nfbm2(      p,  seed, 5, 1.0, 0.5, 2.0, 1.4);
    vec2 q1 = nfbm2(4.0 * q0, seed, 5, 1.0, 0.5, 0.9, 2.1);
    vec2 q2 = nfbm2(0.5 * q1, seed, 5, 1.0, 0.5, 1.0, 1.4);
	return 0.04 * q2.x;
}

#define PI 3.141592653589

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / (PI / 2.0);
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

vec3 gasGiantBaseColor(vec2 uv) {
	float warp = gasGiantWarped(uv.yx, 1.0);
	float noiseVal = sampleGasGiantBands(uv.y + (1.0 - pow(abs(uv.y), 4.0)) * warp);

	// return vec3(normCylCam.x, 0, normCylCam.y);
	return vec3(noiseVal);
	// return vec3(0.5 + 0.5 * normCylCam.x, 0, 0.5 + 0.5 * normCylCam.y);
	// return vec3(0.5 + 0.5 * equator, 0, 0);
}

void main() {
	float gamma = 2.2;

	vec4 baseColor = vec4(0);
	if (IsGasGiant != 0) {
    	baseColor = vec4(gasGiantBaseColor(uvFromNormal(normal)), 1.0);
	} else {
    	baseColor = texture(Sampler0, texCoord0);
	}
	baseColor *= vertexColor;
	// baseColor.rgb = pow(baseColor.rgb, vec3(gamma));

	vec3 res = vec3(0);
	res += contribution(LightColor0, LightPos0);
	res += contribution(LightColor1, LightPos1);
	res += contribution(LightColor2, LightPos2);
	res += contribution(LightColor3, LightPos3);
	vec3 finalColor = res * baseColor.rgb;
	// vec3 finalColor = baseColor.rgb;

    // finalColor = pow(finalColor, vec3(1.0 / gamma));
	finalColor = acesTonemap(finalColor);

    fragColor = vec4(finalColor, 1);
}
