#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;

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

uniform float uStarMinSize;
uniform float uStarMaxSize;
uniform float uStarSizeSquashFactor;
uniform float uStarBrightnessFactor;
uniform float uDimStarMinAlpha;
uniform float uDimStarExponent;

void emitPoint(vec4 viewPos, float pointSize) {
	vec2 uv = uvFromVertex(gl_VertexID);
	vec2 off = vec2(2.0 * uv - 1.0);

	gl_Position = uProjectionMatrix * viewPos;
	gl_Position /= gl_Position.w;
	gl_Position.xy += pointSize * off / uScreenSize;

	texCoord0 = uv;
	vertexPos = viewPos.xyz;
}

void main() {
	vec4 viewPos = uViewMatrix * vec4(aPos, 1.0);
	float distanceFromCameraTm = length(viewPos.xyz) * (uMetersPerUnit / 1e12);

	float size = 300000.0 / length(viewPos.xyz);
	size = clamp(size, 3.0, 15.0);

	float alpha = 1.0;

	emitPoint(viewPos, size);
	vertexColor = vec4(aColor.rgb, alpha);
}

#endif

#ifdef IS_FRAGMENT_STAGE

out vec4 fColor;

void main() {
	if (length(texCoord0 - 0.5) > 0.5)
		discard;

	vec3 color = vertexColor.rgb;

	// desaturate a bit
	color += 0.1;
	color *= 0.9;

    fColor = vec4(color, 1.0);
	// fColor = vec4(1.0, 0.0, 1.0, 1.0);
}

#endif
