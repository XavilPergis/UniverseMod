#version 150

// in VertexOut {
// 	vec2 texCoord0;
// } input;

in vec2 texCoord0;

uniform sampler2D uSampler;
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

void main() {
	vec3 color = texture2D(uSampler, texCoord0).rgb;
	color = uIntensity * prefilter(color);
	fragColor = vec4(color, 1.0);
}