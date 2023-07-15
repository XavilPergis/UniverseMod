#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/filtering.glsl]

uniform sampler2D uPreviousSampler;
uniform sampler2D uAdjacentSampler;
uniform ivec2 uSrcSize;
uniform ivec2 uDstSize;
uniform int uLevel;
uniform int uQuality;

out vec4 fColor;

void main() {
	vec3 color = vec3(0.0);
	// color += upsampleFilter9Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize).rgb;
	// color += upsampleFilter9Tap(uAdjacentSampler, texCoord0, uSrcSize, uDstSize).rgb;
	vec3 a = texture2D(uAdjacentSampler, texCoord0).rgb;
	vec3 b = texture2D(uPreviousSampler, texCoord0).rgb;
	color = vec3(distance(a, b));
	fColor = vec4(color, 1.0);
}

#endif
