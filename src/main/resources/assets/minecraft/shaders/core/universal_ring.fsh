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

in vec2 texCoord0;
in vec4 vertexPos;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
vec3 aces_tonemap(vec3 color){	
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

float saturate(float n) {
	return clamp(n, 0.0, 1.0);
}

vec3 contribution(vec4 color, vec4 pos) {
	if (color.a >= 0) {
		vec3 p0 = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;

		float distanceMeters = distance(vertexPos.xyz, p0) * MetersPerUnit;
		float d2 = 1.0 / pow(distanceMeters / 1e14, 2.0);
		float light = min(10.0, color.a * d2);

		// NOTE: rings are made of a bunch of dust and debris and stuff, and that stuff can
		// reflect light back even at grazing angles, so we don't do a dot product here.
		// TODO: its more complicated than this
		return color.rgb * light;
	}
	return vec3(0);
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

float fbm(float t) {
	float res = 0.0;
	float a = 1.0;
	float f = 1.0;
	float maxValue = 0.0;
	for (int i = 0; i < 8; ++i) {
		res += a * snoise(vec2(f * t, float(i)));
		maxValue += a;
		a /= 2.0;
		f *= 1.5;
	}
	return res / maxValue;
}

vec4 shadeRing(float t) {
	float k = 0.6;
	float intensity = 4.0 * k * fbm(t) - k + 1.0;
	return vec4(vec3(0.5), saturate(intensity));
}

void main() {
	float gamma = 2.2;
    // vec4 baseColor = texture(Sampler0, texCoord0) * vertexColor;
	// baseColor.rgb = pow(baseColor.rgb, vec3(gamma));

    // if (baseColor.a < 0.1) {
    //     discard;
    // }

	vec3 res = vec3(0);

	res += contribution(LightColor0, LightPos0);
	res += contribution(LightColor1, LightPos1);
	res += contribution(LightColor2, LightPos2);
	res += contribution(LightColor3, LightPos3);

	vec4 baseColor = shadeRing(texCoord0.y);
	vec3 finalColor = res * baseColor.rgb;

    // finalColor = pow(finalColor, vec3(1.0 / gamma));
	finalColor = aces_tonemap(finalColor);

    fragColor = vec4(finalColor, baseColor.a);
}
