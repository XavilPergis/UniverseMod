#stages vertex

VARYING_V2F vec2 vTexCoord0;
VARYING_V2F vec4 vVertexColor;
VARYING_V2F vec4 vVertexPosV;
VARYING_V2F vec4 vVertexPosW;
VARYING_V2F vec4 vVertexNormalV;
VARYING_V2F vec4 vVertexNormalW;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]

in vec3 aPos;
in vec2 aTexCoord0;
in vec4 aColor;
in vec3 aNormal;

mat4 getModelMatrix() {
#ifdef USE_MODEL_MATRIX
	return uModelMatrix;
#else
	return mat4(1.0);
#endif
}

void main() {
	vVertexPosW = getModelMatrix() * vec4(aPos, 1.0);
	vVertexPosV = uViewMatrix * vVertexPosW;
    gl_Position = uProjectionMatrix * vVertexPosV;

	vVertexNormalW = getModelMatrix() * vec4(aNormal, 0.0);
    vVertexNormalV = uViewMatrix * vVertexNormalW;

    vTexCoord0 = aTexCoord0;
    vVertexColor = aColor;
}

#endif