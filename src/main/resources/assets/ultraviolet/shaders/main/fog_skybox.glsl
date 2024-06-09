#stages vertex fragment

#stages vertex

VARYING_V2F vec2 vTexCoord0;
VARYING_V2F vec4 vVertexColor;
VARYING_V2F vec4 vVertexPosV;
VARYING_V2F vec4 vVertexPosW;
VARYING_V2F vec4 vVertexPosM;
VARYING_V2F vec4 vVertexNormalV;
VARYING_V2F vec4 vVertexNormalW;
VARYING_V2F vec4 vVertexNormalM;

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
	vVertexPosM = vec4(aPos, 1.0);
	vVertexPosW = getModelMatrix() * vVertexPosM;
	vVertexPosV = uViewMatrix * vVertexPosW;
    gl_Position = uProjectionMatrix * vVertexPosV;

	vVertexNormalM = vec4(aNormal, 0.0);
	vVertexNormalW = getModelMatrix() * vVertexNormalM;
    vVertexNormalV = uViewMatrix * vVertexNormalW;

    vTexCoord0 = aTexCoord0;
    vVertexColor = aColor;
}

#endif

#ifdef IS_FRAGMENT_STAGE

out vec4 fColor;

void main() {
    vec4 colorSrgb = texture(uSampler, texCoord0);
	if (colorSrgb.a >= 0.5) colorSrgb.a = 1.0;
    fColor = vec4(pow(colorSrgb.rgb, vec3(2.0)), colorSrgb.a);
}

#endif
