#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float MetersPerUnit;

in vec4 vertexPos;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
vec3 acesTonemap(vec3 color){	
	mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
	);
	mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
	);
	vec3 v = m1 * color;    
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return pow(clamp(m2 * (a / b), 0.0, 1.0), vec3(1.0 / 2.2));	
}

vec3 tonemap(vec3 hdrColor) {
	return acesTonemap(hdrColor);
}

float saturate(float n) {
	return clamp(n, 0.0, 1.0);
}

vec2 saturate(vec2 n) {
	return vec2(
		clamp(n.x, 0.0, 1.0),
		clamp(n.y, 0.0, 1.0)
	);
}

void main() {
    vec4 s1 = texture(Sampler0, texCoord0) * vertexColor;
    vec4 s2 = texture(Sampler0, (texCoord0 - 0.5) / 0.6 + 0.5);

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (2.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (2.0 * s2.a * s2);
	// vec4 tonemapped = vec4(tonemap(hdrColor.rgb), hdrColor.a);

    fragColor = hdrColor * vertexColor.a;
    // fragColor = s1;
}
