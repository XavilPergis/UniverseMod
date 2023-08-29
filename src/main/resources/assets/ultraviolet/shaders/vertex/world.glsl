#stages vertex

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec4 vertexColor;
VARYING_V2F vec4 vertexPos;
VARYING_V2F vec4 normal;
VARYING_V2F vec4 normalRaw;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]

in vec3 aPos;
in vec2 aTexCoord0;
in vec4 aColor;
in vec3 aNormal;

void main() {
    gl_Position = uProjectionMatrix * uViewMatrix * vec4(aPos, 1.0);
	vertexPos = uViewMatrix * vec4(aPos, 1.0);
    texCoord0 = aTexCoord0;
    vertexColor = aColor;
    normal = uViewMatrix * vec4(aNormal, 0.0);
    normalRaw = vec4(aNormal, 0.0);
}

#endif