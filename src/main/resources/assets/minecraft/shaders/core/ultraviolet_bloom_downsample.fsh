#version 150

#moj_import <ultraviolet_filtering.glsl>

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
	// if (uQuality == 0) {
	// 	fragColor = downsampleFilter4Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize);
	// } else {
	// }
	vec2 uv = texCoord0;
	// uv.y = 1.0 - uv.y;
	fragColor = downsampleFilter13Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize);
}