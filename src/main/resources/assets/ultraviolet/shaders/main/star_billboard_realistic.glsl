#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;
flat VARYING_V2F int billboardID;

VARYING_V2F float brightnessFactor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;

vec2 uvFromVertex(int id) {
	id = id % 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(0.0, 1.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(1.0, 0.0);
}

uniform float uStarSize;
uniform float uStarLuminosityScale;
uniform float uStarLuminosityMax;
uniform float uStarBrightnessScale;
uniform float uStarBrightnessMax;
uniform float uMagnitudeBase;
uniform float uMagnitudePower;
uniform float uReferenceMagnitude;

void emitPoint(vec4 viewPos, float pointSize) {
	vec2 uv = uvFromVertex(gl_VertexID);
	vec2 off = vec2(2.0 * uv - 1.0);

	gl_Position = uProjectionMatrix * viewPos;
	gl_Position /= gl_Position.w;
	gl_Position.xy += pointSize * off / uScreenSize;

	texCoord0 = uv;
	vertexPos = viewPos.xyz;
	billboardID = gl_VertexID / 4;
}

void main() {
	float starLuminosityLsol = aTexCoord0.x;
	vec4 viewPos = uViewMatrix * vec4(aPos, 1.0);
	float distanceFromCamera_pc = length(viewPos.xyz) * (uMetersPerUnit / 3.086e16);

	float L_L0 = 3.827 / 3.0128e2;
	starLuminosityLsol = min(uStarLuminosityScale * starLuminosityLsol, uStarLuminosityMax);
	float appMag = 2.5 * (log(pow(distanceFromCamera_pc, 2.0) / (starLuminosityLsol * L_L0)) / log(uMagnitudeBase)) - 5.0;

	float d = pow(uMagnitudePower, -0.4 * appMag + 0.4 * uReferenceMagnitude);

	brightnessFactor = min(uStarBrightnessScale * d, uStarBrightnessMax);
	if (starLuminosityLsol == 0.0) {
		// brightnessFactor = 1.0;
	}

	emitPoint(viewPos, 2.0 * uStarSize);
	vertexColor = vec4(aColor.rgb, 1.0);
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
	// fColor = vertexColor;
    vec4 s1 = texture(uBillboardTexture, saturate(scaleUv(texCoord0, 2.0)));
	s1 *= vertexColor;
	// s1 *= 5.0;
	s1 *= brightnessFactor;
    // vec4 s2 = texture(uBillboardTexture, saturate(scaleUv(texCoord0, 2.0 / 0.7)));

	vec4 color = vec4(0.0);
	// color = (1.0 * color) + (1.0 * s1.a * s1);
	color += s1.a * s1;
	// color = (1.0 * color) + (1.0 * s2.a * s2);

    fColor = color * vertexColor.a;
}

#endif
