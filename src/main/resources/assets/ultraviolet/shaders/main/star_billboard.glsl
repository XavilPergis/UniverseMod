#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;
flat VARYING_V2F int billboardID;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 Position;
in vec4 Color;
in vec2 UV0;

#define STAR_MIN_SIZE (5.0)
#define STAR_MAX_SIZE (8.0)
#define STAR_SIZE_SQUASH_FACTOR (250.0)

vec2 uvFromVertex(int id) {
	id = id % 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(0.0, 1.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(1.0, 0.0);
}

// void emitPoint(vec4 clipPos, float pointSize) {
// 	vec2 uv = uvFromVertex(gl_VertexID);
// 	vec2 off = vec2(2.0 * uv - 1.0);

// 	// 2 * ((c * 0.5 + 0.5) * s + k) / s - 1
// 	// 2 * ((c * 0.5 + 0.5) + k / s) - 1
// 	// c + (2 * k / s)

// 	clipPos.xy += 20.0 * pointSize * off / uScreenSize;

//     gl_Position = clipPos;
// 	texCoord0 = uv;
// }

uniform float uStarMinSize; // 5
uniform float uStarMaxSize; // 8
uniform float uStarSizeSquashFactor; // 250
uniform float uStarBrightnessFactor; // 2e10
uniform float uDimStarMinAlpha; // 0.1
uniform float uDimStarExponent; // 0.1

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
	float starLuminosityLsol = UV0.x;

	vec4 viewPos = uViewMatrix * vec4(Position, 1.0);
	float distanceFromCameraTm = length(viewPos.xyz) * (uMetersPerUnit / 1e12);
	float apparentBrightness = uStarBrightnessFactor * starLuminosityLsol / (4.0 * PI * distanceFromCameraTm * distanceFromCameraTm);

	// map [0,inf] to [0,1]
	float size = uStarMaxSize * (2.0 / PI) * atan(apparentBrightness / uStarSizeSquashFactor);
	// float size = uStarMaxSize * log(1.0 + apparentBrightness / uStarSizeSquashFactor);

	// fade star out if it's too small
	float alpha = 1.0;
	if (size < uStarMinSize) {
		float oldSize = size;
		size = uStarMinSize;
		alpha = mix(uDimStarMinAlpha, 1.0, pow(oldSize / uStarMinSize, uDimStarExponent));
	}

	emitPoint(viewPos, 2.0 * size);
	vertexColor = vec4(Color.rgb, alpha);
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
    vec4 s1 = texture(uBillboardTexture, saturate(scaleUv(texCoord0, 2.0)));
	s1 *= vertexColor;
    vec4 s2 = texture(uBillboardTexture, saturate(scaleUv(texCoord0, 2.0 / 0.7)));

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (1.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (1.0 * s2.a * s2);

    fColor = 0.9 * hdrColor * vertexColor.a;
    // fColor = 0.9 * hdrColor;
}

#endif
