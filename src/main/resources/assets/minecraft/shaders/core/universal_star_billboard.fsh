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

// void main() {

// 	vec2 uv = texCoord0;
// 	uv -= 0.5;
// 	float n1 = 0.5; // - smoothstep(0.2, 0.5, length(uv));
// 	float n2 = 0.5; // - smoothstep(0.1, 0.2, length(uv));

// 	// vec4 s1 = vertexColor;
// 	vec4 s1 = vec4(vertexColor.rgb, n1);
// 	vec4 s2 = vec4(vec3(1.0), n2);

// 	vec4 hdrColor = vec4(0.0);
// 	hdrColor = (1.0 * hdrColor) + (1.0 * s1.a * s1);
// 	hdrColor = (1.0 * hdrColor) + (1.0 * s2.a * s2);

// 	// float alpha = max(0.0, pow(vertexColor.a, 1.5));
// 	// float alpha = max(0.0, n * vertexColor.a);
//     fragColor = hdrColor;
// }

void main() {
    vec4 s1 = texture(Sampler0, texCoord0);
	// if (s1.a < 0.2) discard;
	// s1 *= (vertexColor / 2.0);
	s1 *= (vertexColor);
    vec4 s2 = texture(Sampler0, saturate((texCoord0 - 0.5) / 0.5 + 0.5));

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (1.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (1.0 * s2.a * s2);

	float alpha = max(0.0, pow(vertexColor.a, 1.0));
    // fragColor = pow(hdrColor * vertexColor.a, vec4(1.0 / 2.2));
    // fragColor = hdrColor * alpha + vec4(0.3, 0.0, 0.0, 1.0);
    fragColor = hdrColor * alpha;
}
