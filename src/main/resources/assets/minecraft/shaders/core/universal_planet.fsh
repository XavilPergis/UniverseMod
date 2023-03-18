#version 150

#moj_import <universal_noise.glsl>
#moj_import <universal_util.glsl>
#moj_import <universal_misc.glsl>

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
		currentValue += currentAmplitude * noiseSimplex(currentFrequency * v);
		maxValue += currentAmplitude;
		currentFrequency *= frequencyFalloff;
		currentAmplitude *= amplitudeFalloff;
	}
	return currentValue / maxValue;
}

vec2 nfbm2(vec2 v, float seed, int octaves, float baseAmplitude, float amplitudeFalloff, float baseFrequency, float frequencyFalloff) {
	return vec2(
		nfbm(v + 1.2, octaves, baseAmplitude, amplitudeFalloff, baseFrequency, frequencyFalloff),
		nfbm(v - 1.7, octaves, baseAmplitude, amplitudeFalloff, baseFrequency, frequencyFalloff)
	);
}

float sampleGasGiantBands(float t) {
	float amplitude = 1.4;
	float frequency = 5.0;
	float foo = floor(amplitude * noiseSimplex(vec2(frequency * t, 0.0)));
	return 0.5 + 0.5 * (foo / amplitude);
}

float gasGiantWarped(vec2 p, float seed) {
	vec2 q0 = nfbm2(      p,  seed, 5, 1.0, 0.5, 2.0, 1.4);
    vec2 q1 = nfbm2(4.0 * q0, seed, 5, 1.0, 0.5, 0.9, 2.1);
    vec2 q2 = nfbm2(0.5 * q1, seed, 5, 1.0, 0.5, 1.0, 1.4);
	return 0.04 * q2.x;
}

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / PI_2;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

vec3 gasGiantBaseColor(vec2 uv) {
	float warp = gasGiantWarped(uv.yx + vec2(0.0001 * Time, 0.0), RenderingSeed);
	float noiseVal = sampleGasGiantBands(uv.y + (1.0 - pow(abs(uv.y), 4.0)) * warp + RenderingSeed / 1000.0);

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
