#version 150

// in VertexOut {
// 	vec2 texCoord0;
// } input;

in vec2 texCoord0;

uniform sampler2D uSampler;

out vec4 fragColor;

void main() {
	// fragColor = texture2D(uSampler, input.texCoord0);
	fragColor = texture2D(uSampler, texCoord0);
	// fragColor = vec4(input.texCoord0, 0.0, 0.0);
	// fragColor = vec4(texCoord0, 0.0, 0.0);
}