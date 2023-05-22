#ifndef ULTRAVIOLET_TONEMAP_H_
#define ULTRAVIOLET_TONEMAP_H_

vec3 tonemapClamp(vec3 hdrColor) {
	return clamp(hdrColor, 0.0, 1.0);
}

vec3 tonemapReinhardSimple(vec3 hdrColor) {
	return hdrColor / (1.0 + hdrColor);
}

vec3 tonemapReinhardModified(vec3 hdrColor, float whitePoint) {
	return hdrColor * (1.0 + hdrColor / pow(whitePoint, 2.0)) / (1.0 + hdrColor);
}

// https://knarkowicz.wordpress.com/2016/01/06/aces-filmic-tone-mapping-curve/
vec3 tonemapACESSimple(vec3 hdrColor) {
	return saturate((hdrColor * (2.51 * hdrColor + 0.03)) / (hdrColor * (2.43 * hdrColor + 0.59) + 0.14));
}

vec3 tonemapACESFull(vec3 hdrColor) {
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
	vec3 v = m1 * hdrColor;
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return clamp(m2 * (a / b), 0.0, 1.0);
}

#endif
