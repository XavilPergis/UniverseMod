#stages vertex

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]

in vec3 Position;
in vec4 Color;
in vec2 UV0;

vec2 uv() {
	int id = gl_VertexID % 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(0.0, 1.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(1.0, 0.0);
}

float billboardSize() {
	return UV0.x;
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

void main() {
	vec4 posView = uViewMatrix * vec4(Position, 1.0);
	vec2 uv = uv();
	vec2 off = vec2(2.0 * uv - 1.0);

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
    vertexColor = Color;
}

#endif