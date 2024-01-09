#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:post/bloom/filtering.glsl]
#include [ultraviolet:lib/util.glsl]

uniform sampler2D uPreviousSampler;
uniform sampler2D uAdjacentSampler;
uniform ivec2 uSrcSize;
uniform ivec2 uDstSize;
uniform int uLevel;
uniform int uQuality;
uniform float uBlendIntensity;

uniform int uUseDirtTexture;
uniform sampler2D uDirtTexture;
uniform float uDirtIntensity;

out vec4 fColor;

void main() {
	vec3 bloomColor = upsampleFilter9Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize).rgb;
	vec3 sceneColor = texture(uAdjacentSampler, texCoord0).rgb;

	vec3 color = vec3(0.0);
	if (uLevel == 0) {
		// apply lens dirt by selectively brightening the bloom buffer.
		if (uUseDirtTexture != 0) {
			vec4 dirt = texture(uDirtTexture, texCoord0);
			bloomColor *= 1.0 + uDirtIntensity * dirt.rgb;
		}
		// blend the bloom into the final image. note that the output is in hdr and will be tonemapped later.
		color = sceneColor + bloomColor * uBlendIntensity;
	} else {
		color += sceneColor;
		color += bloomColor;
	}

	fColor = vec4(color, 1.0);
}

#endif
