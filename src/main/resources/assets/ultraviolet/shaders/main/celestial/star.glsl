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

const float NOISE_DETAIL_RIDGE_OFFSET = 0.035;

float curveStarBrightness(in float brightness) {
	float t = brightness / (brightness + STAR_REFERENCE_BRIGHTNESS);
	return mix(STAR_MIN_BRIGHTNESS, STAR_MAX_BRIGHTNESS, t);
}

vec3 shadeCelestialObject(inout FragmentInfo frag, in Star node) {
	// domain warped noise
	vec4 p = vec4(frag.normalW, float(node.seed % 100000) / 100000.0);

	float a1 = mix(-1f, 1f, noiseFbm(FbmInfo(3, 1.0, 1.5, 0.4, 2.5), p + vec4(0.0, 0.0, 0.0, 1.0)));
	float b1 = mix(-1f, 1f, noiseFbm(FbmInfo(3, 1.0, 1.5, 0.4, 2.5), p + vec4(0.0, 0.0, 0.0, 0.0)));
	p.xyz += 1.6 * vec3(a1, 0.0, b1) + vec3(uTime / 1000000.0);

	float a2 = mix(-1f, 1f, noiseFbm(FbmInfo(3, 1.0, 0.6, 0.7, 3.5), p + vec4(0.0, 0.0, 0.0, 1.0)));
	float b2 = mix(-1f, 1f, noiseFbm(FbmInfo(3, 1.0, 0.6, 0.7, 3.5), p + vec4(0.0, 0.0, 0.0, 0.0)));
	p.xyz += 0.5 * vec3(a2, 0.0, b2) + vec3(uTime / 100000.0);

	float n = abs(mix(-1f, 1f, noiseFbm(FbmInfo(4, 1.0, 1.0, 0.7, 3.0), p)));

	// shape noise into ridges
	n *= 1.0 + NOISE_DETAIL_RIDGE_OFFSET;
	n -= NOISE_DETAIL_RIDGE_OFFSET;
	n = abs(n);
	n = pow(n, 0.7);
	n *= 2.0;

	vec3 res = vec3(0.0);

	// base emissive modulated by the noise field
	res += curveStarBrightness(node.brightness) * node.color;
	res *= n;

	// make the outer rim of the star look all shiny
	res += 50.0 * node.color * fresnelFactor(normalize(frag.posV), frag.normalV);

	return res;
}

#endif