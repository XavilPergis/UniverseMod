#version 150

#moj_import <universal_filtering.glsl>

// in VertexOut {
// 	vec2 uv0;
// } input;

in vec2 texCoord0;

uniform sampler2D uPreviousSampler;
uniform ivec2 uSrcSize;
uniform ivec2 uDstSize;
uniform int uQuality;

out vec4 fragColor;

void main() {
	fragColor = 0.72 * upsampleFilter9Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize);
}