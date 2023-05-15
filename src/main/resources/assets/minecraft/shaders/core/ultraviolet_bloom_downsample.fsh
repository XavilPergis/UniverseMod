#version 150

#moj_import <ultraviolet_filtering.glsl>

// in VertexOut {
// 	vec2 uv0;
// } input;

in vec2 texCoord0;

uniform sampler2D uPreviousSampler;
uniform int uLevel;
// downsampling
uniform ivec2 uSrcSize;
uniform ivec2 uDstSize;
uniform int uQuality;
// prefiltering
uniform float uThreshold;
uniform float uSoftThreshold;
uniform float uIntensity;

out vec4 fragColor;

#define PREFILTER_BIAS 0.00001

vec3 prefilter(vec3 color) {
	float brightness = max(color.r, max(color.g, color.b));
	float knee = uThreshold * uSoftThreshold;

	float soft = brightness - uThreshold + knee;
	soft = clamp(soft, 0.0, 2.0 * knee);
	soft = soft * soft / (4.0 * knee + PREFILTER_BIAS);

	float contribution = max(soft, brightness - uThreshold);
	contribution /= max(brightness, PREFILTER_BIAS);

	return color * contribution;
}

vec4 downsample() {
	if (uQuality == 0) {
		return downsampleFilter4Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize);
	} else {
		return downsampleFilter13Tap(uPreviousSampler, texCoord0, uSrcSize, uDstSize);
	}
}

void main() {
	vec3 color = downsample().rgb;
	if (uLevel == 0) color = uIntensity * prefilter(color);
	fragColor = vec4(color, 1.0);
}