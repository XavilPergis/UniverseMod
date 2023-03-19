#version 150

#moj_import <universal_noise.glsl>
#moj_import <universal_util.glsl>
#moj_import <universal_misc.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float MetersPerUnit;

in vec4 vertexPos;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 s1 = texture(Sampler0, texCoord0) * vertexColor;
    vec4 s2 = texture(Sampler0, saturate((texCoord0 - 0.5) / 0.5 + 0.5));

	// s1 *= pow(vertexColor.a, 0.7);
	// s2 *= pow(vertexColor.a, 0.7);

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (2.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (2.0 * s2.a * s2);
	// vec4 tonemapped = vec4(acesTonemap(hdrColor.rgb), hdrColor.a);
	// tonemapped *= 1.4;

    fragColor = hdrColor * vertexColor.a;
    // fragColor = tonemapped;
}
