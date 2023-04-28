#version 150

#moj_import <universal_noise.glsl>
#moj_import <universal_util.glsl>
#moj_import <universal_misc.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform mat4 ModelViewMat;
uniform float MetersPerUnit;
uniform float RenderingSeed;
uniform float Time;

in vec2 texCoord0;
in vec4 vertexPos;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

// =============== GAS GIANT SHADING ===============

float fbm(in vec3 pos) {
    float maxValue = 0.0;
    float currentValue = 0.0;
    
    float currentAmplitude = 1.0;
    float currentFrequency = 1.0;
    for (int i = 0; i < 8; ++i) {
        maxValue += currentAmplitude;
        vec3 offset = 10.0 * vec3(rand(float(i)));
        currentValue += currentAmplitude * noiseSimplex(currentFrequency * pos + offset);
        currentAmplitude *= 0.7;
        currentFrequency *= 1.7;
    }
    return currentValue / maxValue;
}

// =============== === ===== ======= ===============

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / PI_2;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

vec3 gasGiantBaseColor(vec3 pos) {
	return field(pos, RenderingSeed / 1000.0);
}

void main() {
	vec4 baseColor = vec4(0);
	if (IsGasGiant != 0) {
		vec3 norm = (inverse(ModelViewMat) * normalize(normal)).xyz;
		// baseColor = vec4(gasGiantBaseColor(uvFromNormal(normalize(normal))), 1.0);
		baseColor = vec4(gasGiantBaseColor(norm), 1.0);
	} else {
    	baseColor = texture(Sampler0, texCoord0);
	}
	baseColor *= vertexColor;

	vec3 res = vec3(0);
	res += contribution(LightColor0, LightPos0);
	res += contribution(LightColor1, LightPos1);
	res += contribution(LightColor2, LightPos2);
	res += contribution(LightColor3, LightPos3);
	vec3 finalColor = res * baseColor.rgb;

	// finalColor = acesTonemap(finalColor);

    fragColor = vec4(finalColor, 1);
}
