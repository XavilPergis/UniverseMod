#version 150

#moj_import <ultraviolet_filtering.glsl>

// in VertexOut {
// 	vec2 uv0;
// } input;

in vec2 texCoord0;

uniform sampler2D uPreviousSampler;
uniform sampler2D uAdjacentSampler;
uniform ivec2 uSrcSize;
uniform ivec2 uDstSize;
uniform int uLevel;
uniform int uQuality;

out vec4 fragColor;

void main() {
	vec3 color = vec3(0.0);
	// color += upsampleFilter9Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize).rgb;
	// color += upsampleFilter9Tap(uAdjacentSampler, texCoord0, uSrcSize, uDstSize).rgb;
	vec3 a = texture2D(uAdjacentSampler, texCoord0).rgb;
	vec3 b = texture2D(uPreviousSampler, texCoord0).rgb;
	color = vec3(distance(a, b));
	fragColor = vec4(color, 1.0);
}