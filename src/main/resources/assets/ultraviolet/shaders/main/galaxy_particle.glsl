#stages fragment

//#define BILLBOARD_KIND_TOWARDS_CAMERA
//#define BILLBOARD_RANDOM_ORIENTATION
//#include [ultraviolet:vertex/billboard.glsl]

#stages vertex

VARYING_V2F vec2 vTexCoord0;
VARYING_V2F vec4 vVertexColor;
VARYING_V2F vec4 vVertexPosV;
VARYING_V2F vec4 vVertexPosW;
VARYING_V2F vec4 vVertexPosM;

#ifdef IS_VERTEX_STAGE
#include [ultraviolet:common_uniforms.glsl]

in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;

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

    vTexCoord0 = aTexCoord0;
    vVertexColor = aColor;
}

#endif
#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uBillboardTexture;
uniform float uParticleMultiplier;

out vec4 fColor;

void main() {
    vec4 s1 = texture(uBillboardTexture, vTexCoord0) * vVertexColor;
	// vec4 s1 = vec4(vec3(length(vTexCoord0)), 1.0) * vVertexColor;
	// float d = max(0, 1.0 - length(2.0 * vTexCoord0 - 1.0));
	// d = pow(d, 4.0);
	// vec4 s1 = vec4(vec3(d), 1.0) * vVertexColor;
	// vec4 s1 = vVertexColor;
    // fColor = vec4(s1.a * vVertexColor.rgb * vVertexColor.a, 1.0);
	s1.a *= uParticleMultiplier;
    fColor = s1;
	//fColor = vec4(1.0 * uParticleMultiplier);
	//fColor = vec4(vTexCoord0, 1.0, 1.0);
	//fColor = texture(uBillboardTexture, vTexCoord0);
}

#endif
