#version 150

#moj_import <ultraviolet_noise.glsl>
#moj_import <ultraviolet_util.glsl>
// #moj_import <ultraviolet_misc.glsl>

// uniform sampler2D Sampler0;

// uniform vec4 ColorModulator;
uniform mat4 ModelViewMat;
// uniform float MetersPerUnit;
// uniform float RenderingSeed;
uniform float Time;
uniform vec3 StarColor;

// in vec2 texCoord0;
// in vec4 vertexPos;
// in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / HALF_PI;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

void main() {
	vec3 norm = (inverse(ModelViewMat) * normalize(normal)).xyz;
	//vec2 uv = uvFromNormal(norm);
	float a = fbm(DEFAULT_FBM, norm + 10.0);
	float b = fbm(DEFAULT_FBM, norm - 10.0);
	float n = 1. - abs(fbm(DEFAULT_FBM, 4.0 * norm + vec3(a, 0.0, b)));
	vec3 col = 10.0 * (StarColor + 0.1) * n;
	col += 70.0 * (StarColor + 0.1) * fresnelFactor(normalize(normal).xyz, 2.0);
	col = col / (1.0 + col);
    fragColor = vec4(col, 1.0);
}
