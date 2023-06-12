#stages vertex

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec4 vertexColor;
VARYING_V2F vec4 vertexPos;
VARYING_V2F vec4 normal;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

void main() {
    gl_Position = uProjectionMatrix * uViewMatrix * vec4(Position, 1.0);
	vertexPos = uViewMatrix * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    normal = uViewMatrix * vec4(Normal, 0.0);
}

#endif