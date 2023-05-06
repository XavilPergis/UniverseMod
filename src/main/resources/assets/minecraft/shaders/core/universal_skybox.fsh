#version 150

uniform samplerCube SkyboxSampler;

in vec3 texCoord0;

out vec4 fragColor;

void main() {
	fragColor = texture(SkyboxSampler, texCoord0);
	// fragColor = vec4(1.0);
}
