#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec4 vertexColor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;
in int aBatchId;

struct BatchingInfo {
    mat4 modelMatrix;
    vec4 color;
};

buffer bBatchingInfos {
    BatchingInfo uBatchingBuffers[];
};

void main() {
    BatchingInfo info = uBatchingBuffers[aBatchId];
	gl_Position = uProjectionMatrix * uViewMatrix * info.modelMatrix * vec4(aPos, 1.0);
	texCoord0 = aTexCoord0;
	vertexColor = info.color * aColor;
}

#endif

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uFontAtlas;
uniform vec4 uColor;

out vec4 fColor;

void main() {
    vec4 atlasColor = texture(uFontAtlas, texCoord0);
    fColor = uColor * vertexColor * atlasColor;
}

#endif
