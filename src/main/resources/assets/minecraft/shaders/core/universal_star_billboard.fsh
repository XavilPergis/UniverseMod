#version 150

#moj_import <universal_util.glsl>

uniform sampler2D Sampler0;

in VertexOut {
	vec4 color;
	vec2 uv0;
} input;

out vec4 fragColor;

void main() {
    vec4 s1 = texture(Sampler0, input.uv0);
	s1 *= input.color;
    vec4 s2 = texture(Sampler0, saturate((input.uv0 - 0.5) / 0.5 + 0.5));

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (1.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (1.0 * s2.a * s2);

	float alpha = max(0.0, pow(input.color.a, 1.0));
    fragColor = hdrColor * alpha;
}
