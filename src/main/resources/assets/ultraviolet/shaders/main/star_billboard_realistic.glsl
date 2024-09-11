#stages vertex fragment

VARYING_V2F vec4 vertexColor;
flat VARYING_V2F int billboardID;

VARYING_V2F float brightnessFactor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec4 aPos;
in vec4 aColor;
//in vec2 aTexCoord0;

uniform float uStarSize;
uniform float uStarLuminosityScale;
uniform float uStarLuminosityMax;
uniform float uStarBrightnessScale;
uniform float uStarBrightnessMax;

void main() {
	//float starLuminosityLsol = aTexCoord0.x;
	float starLuminosityLsol = aPos.w;
	vec4 viewPos = uViewMatrix * vec4(aPos.xyz, 1.0);
	float distanceFromCamera_pc = length(viewPos.xyz) * (uMetersPerUnit / 3.086e16);

	starLuminosityLsol = min(uStarLuminosityScale * starLuminosityLsol, uStarLuminosityMax);

	float d = 1000.0 * starLuminosityLsol * pow(distanceFromCamera_pc, -2.0) / (4.0 * PI);
	d *= pow(aColor.a, 4.0);

	float brightnessRaw = uStarBrightnessScale * d;
	float leftover = max(0.0, brightnessRaw - 0.0 * uStarBrightnessMax);
	leftover = max(0.0, log(leftover) / log(20.0));

	brightnessFactor = min(brightnessRaw, uStarBrightnessMax);

	gl_Position = uProjectionMatrix * viewPos;
	gl_PointSize = uStarSize + min(leftover, 30.0 * uStarSize);
	billboardID = gl_VertexID;

	vertexColor = vec4(aColor.rgb, 1.0);

	// if (brightnessFactor <= 0.01) {
	// 	brightnessFactor = 0.0;
	// 	// vertexColor = vec4(0.0, 0.0, 1.0, 1.0);
	// } else {
	// 	brightnessFactor = 10.0;
	// 	vertexColor = vec4(vec3(max(0.0, 300.0 - distanceFromCamera_pc) / 300.0), 1.0);
	// }

}

#endif

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/util.glsl]
#include [ultraviolet:lib/noise.glsl]

uniform sampler2D uBillboardTexture;

out vec4 fColor;

vec2 scaleUv(vec2 uv, float scale) {
	uv = 2.0 * uv - 1.0;
	uv *= scale;
	return 0.5 + uv * 0.5;
}

void main() {
	vec3 col = vec3(0.0);

    //vec4 s1 = vec4(vec3(pow(max(0.0, 1.0 - (2.0 * length(gl_PointCoord - 0.5))), 2.0)), 1.0);
	float n1 = max(0.0, 1.0 - (2.0 * length(gl_PointCoord - 0.5)));
    col += vec3(pow(n1, 2.0));
	col *= brightnessFactor;

	// if (brightnessFactor > 0.5) {
	// 	float d2 = max(0.0, length(gl_PointCoord - 0.5) - 0.05);
	// 	float n2 = max(0.0, 1.0 - (2.0 * d2));
	// 	col += 0.1 * vec3(pow(n2, 5.0)) * min(1.0, brightnessFactor - 0.5);
	// }

	col *= vertexColor.rgb;

	// very slight twinkle, like how atmospheric distortion causes stars to flicker a little bit
	//s1.a *= mix(0.95, 1.05, noiseSimplex(3.0 * uTime, float(billboardID)) * 0.5 + 0.5);

    fColor = vec4(col * vertexColor.a, 1.0);
	// fColor.rgb = pow(fColor.rgb, vec3(2.0));
    // fColor = vec4(1.0, 0.0, 1.0, 1.0);
}

#endif
