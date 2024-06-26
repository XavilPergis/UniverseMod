#stages vertex

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]
#include [ultraviolet:lib/noise.glsl]

in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;

vec2 uvFromVertex(int id) {
	id %= 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(0.0, 1.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(1.0, 0.0);
}

float billboardSize() {
	return aTexCoord0.x;
}

vec3 offsetViewAligned(vec2 vertexOffset) {
	return billboardSize() * vec3(vertexOffset, 0.0);
}

vec3 offsetFacingCamera(vec3 posView, vec2 vertexOffset) {
	vec3 facing = normalize(posView);
	vec3 up = normalize(cross(facing, vec3(1.0, 0.0, 0.0)));
	vec3 right = normalize(cross(facing, up));
	return billboardSize() * (vertexOffset.x * right + vertexOffset.y * up);
}

vec2 rotate(vec2 v, float a) {
	float s = sin(a);
	float c = cos(a);
	mat2 m = mat2(c, s, -s, c);
	return m * v;
}

void main() {
	vec4 posView = uViewMatrix * vec4(aPos, 1.0);
	vec2 uv = uvFromVertex(gl_VertexID);
	vec2 off = vec2(2.0 * uv - 1.0);

#if defined(BILLBOARD_RANDOM_ORIENTATION)
	off = rotate(off, rand(float(gl_VertexID)));
#endif

#if defined(BILLBOARD_KIND_VIEW_ALIGNED)
	posView.xyz += offsetViewAligned(off);
#elif defined(BILLBOARD_KIND_TOWARDS_CAMERA)
	posView.xyz += offsetFacingCamera(posView.xyz, off);
#else
#error No billboard mode was specified.
#endif

    gl_Position = uProjectionMatrix * posView;

	texCoord0 = uv;
	vertexPos = posView.xyz;
    vertexColor = aColor;
}

#endif