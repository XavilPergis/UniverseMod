#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_STAR_H_
#define ULTRAVIOLET_CELESTIAL_STAR_H_

struct Star {
	int seed;
	vec3 color;
	float brightness;
};

const float STAR_REFERENCE_BRIGHTNESS = 1.0;
const float STAR_MIN_BRIGHTNESS = 15.0;
const float STAR_MAX_BRIGHTNESS = 25.0;

float curveStarBrightness(in float brightness) {
	float t = brightness / (brightness + STAR_REFERENCE_BRIGHTNESS);
	return mix(STAR_MIN_BRIGHTNESS, STAR_MAX_BRIGHTNESS, t);
}

vec3 shadeCelestialObject(inout FragmentInfo frag, in Star star) {
	// domain warped noise
	float a = mix(-1f, 1f, noiseFbm(DEFAULT_FBM, frag.normalW, 0.0));
	float b = mix(-1f, 1f, noiseFbm(DEFAULT_FBM, frag.normalW, 1.0));
	vec3 offset = 0.8 * vec3(a, 0.0, b) + vec3(uTime / 1000);
	float n = abs(mix(-1f, 1f, noiseFbm(DEFAULT_FBM, 2.0 * frag.normalW + offset)));

	// shape noise into ridges
	n = pow(n, 0.2);
	n = 1.0 - n;

	vec3 res = vec3(0.0);

	// base emissive modulated by the noise field
	res += curveStarBrightness(star.brightness) * star.color;
	res *= n;

	// make the outer rim of the star look all shiny
	res += 12.0 * star.color * fresnelFactor(frag.normalV, 1.0);

	return res;
}

#endif