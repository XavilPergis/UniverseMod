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
	// fColor = vec4(1.0, 0.0, 1.0, 1.0);
	// fColor = vec4(normal.xyz * 0.5 + 0.5, 1.0);
	// fColor = vec4(normalRaw.xyz * 0.5 + 0.5, 1.0);
	// fColor = vec4(vertexColor.rgb, 1.0);
	// fColor = vec4(texCoord0.xy, 1.0, 1.0);

	vec3 normWorld = (inverse(uViewMatrix) * normalize(normal)).xyz;
	vec3 posWorld = (inverse(uViewMatrix) * normalize(vertexPos)).xyz;

	float frequency = 2.0;
	float warpScale = 0.8;
	float a = fbm(DEFAULT_FBM, normWorld + 20.0);
	float b = fbm(DEFAULT_FBM, normWorld - 20.0);
	float n = abs(fbm(DEFAULT_FBM, frequency * normWorld + warpScale * vec3(a, 0.0, b) + vec3(uTime / 1000)));
	n = pow(n, 0.2);
	n = 1.0 - n;

	float brightness = mix(6.0, 15.0, 3.0 * uStarColor.a / (3.0 * uStarColor.a + 1.0));

	vec3 starColor = uStarColor.rgb;
	vec3 col = vec3(0.0);
	col += brightness * starColor * n;
	vec3 toEye = normalize(uCameraPos - posWorld);
	col += 6.0 * starColor * fresnelFactor(toEye, normWorld, 1.0);
	// col += 1.5 * starColor;
    fColor = vec4(col, 1.0);
}

#endif
