#stages fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]
#include [ultraviolet:common_uniforms.glsl]

uniform vec4 uStarColor;

out vec4 fColor;

vec2 uvFromNormal(vec4 normWorld) {
	vec3 normCam = (inverse(uViewMatrix) * normWorld).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / HALF_PI;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

void main() {
	vec3 normWorld = (inverse(uViewMatrix) * normalize(normal)).xyz;
	vec3 posWorld = (inverse(uViewMatrix) * normalize(vertexPos)).xyz;

	float frequency = 2.0;
	float warpScale = 0.8;
	float a = fbm(DEFAULT_FBM, normWorld + 20.0);
	float b = fbm(DEFAULT_FBM, normWorld - 20.0);
	float n = abs(fbm(DEFAULT_FBM, frequency * normWorld + warpScale * vec3(a, 0.0, b) + vec3(uTime / 1000)));
	n = pow(n, 0.2);
	n = 1.0 - n;

	vec3 starColor = uStarColor.rgb + 0.05;
	vec3 col = 4.0 * starColor * n;
	vec3 toEye = normalize(uCameraPos - posWorld);
	col += 3.0 * starColor * fresnelFactor(toEye, normWorld, 6.0);
	col += 1.0 * starColor;
    fColor = vec4(col, 1.0);
}

#endif
