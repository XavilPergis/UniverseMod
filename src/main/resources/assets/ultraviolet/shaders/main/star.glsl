#stages fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]

uniform mat4 ModelViewMat;
uniform float Time;
uniform vec4 StarColor;

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
	vec3 col = 10.0 * (StarColor.rgb + 0.1) * n;
	col += 70.0 * (StarColor.rgb + 0.1) * fresnelFactor(normalize(normal).xyz, 2.0);
	col = col / (1.0 + col);
    fragColor = vec4(col, 1.0);
}

#endif
