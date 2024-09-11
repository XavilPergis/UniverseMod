#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec4 vertexColor;

#include [ultraviolet:common_uniforms.glsl]
#include [ultraviolet:lib/noise.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;
in int aBatchId;

struct BatchingInfo {
    mat4 modelMatrix;
    uint color;
};

layout (std430) readonly buffer bBatchingInfos {
    BatchingInfo uBatchingBuffers[];
};

void main() {
    BatchingInfo info = uBatchingBuffers[aBatchId];

    int rng = aBatchId;
    //vec4 batchColor = vec4(nextFloat3(rng), 1.0);
    vec4 batchColor = vec4(1.0);

    floatFromInt(hash(aBatchId));
	gl_Position = uProjectionMatrix * uViewMatrix * info.modelMatrix * vec4(aPos, 1.0);
	//gl_Position = uProjectionMatrix * uViewMatrix * vec4(aPos, 1.0);
	texCoord0 = aTexCoord0;
    float r = float((info.color & 0xFF000000) >> 24) / 255.0;
    float g = float((info.color & 0x00FF0000) >> 16) / 255.0;
    float b = float((info.color & 0x0000FF00) >> 8)  / 255.0;
    float a = float((info.color & 0x000000FF) >> 0)  / 255.0;
	vertexColor = batchColor * vec4(r, g, b, a) * aColor;
	//vertexColor = aColor;
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
