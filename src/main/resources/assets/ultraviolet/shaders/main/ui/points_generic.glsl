#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;

void main() {
	vec4 viewPos = uViewMatrix * vec4(aPos, 1.0);
	vertexColor = aColor;

	gl_Position = uProjectionMatrix * viewPos;
	// point size is encoded in texture coordinates
	// gl_PointSize = aTexCoord0.x;
	gl_PointSize = 2f;
}

#endif

#ifdef IS_FRAGMENT_STAGE

out vec4 fColor;

void main() {
	// if (length(gl_PointCoord - 0.5) > 0.5)
	// 	discard;

    fColor = vertexColor;
}

#endif
