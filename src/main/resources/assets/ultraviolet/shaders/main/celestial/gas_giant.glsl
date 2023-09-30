#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_GAS_GIANT_H_
#define ULTRAVIOLET_CELESTIAL_GAS_GIANT_H_

struct GasGiant {
	int seed;
	sampler1D colorGradient;
};

float fbm01(in vec3 pos, in int N, in float fI, in float aI, in float fS, in float aS) {
	float freq = fI;
	float amp = aI;
	float ampTotal = 0.0;
	float total = 0.0;
	for (int i = 0; i < N; ++i) {
		total += amp * noiseSimplex(freq * pos);
		freq *= fS;
		ampTotal += amp;
		amp *= aS;
	}
	return (total / ampTotal) * 0.5 + 0.5;
}

vec2 fbm2(in vec3 pos, in int N, in float seed, in float fI, in float aI, in float fS, in float aS) {
	vec2 off = vec2(0.0);
	off.x += fbm01(pos + seed, N, fI, aI, fS, aS);
	off.y += fbm01(pos - seed, N, fI, aI, fS, aS);
	return off;
}

vec3 shadeCelestialObject(inout FragmentInfo frag, in GasGiant node) {
	int rng = uRenderingSeed;

	vec3 pos = frag.normalM;

	float n = 0.0;
	pos.y *= mix(3.0, 20.0, nextFloat(rng));
	pos.y += nextFloat(rng, -1000.0,	 1000.0);

	pos.xz *= nextFloat(rng, 1.0, 0.2);
	pos.xz += 1.0 * fbm2(pos, 3, 10.0, 5.0, 1.0, 4.0, 0.4);
	pos.y += 5.0 * fbm01(pos, 1, 0.05, 1.0, 2.0, 0.5);
	// pos.xz += 2.0 * fbm2(pos, 4, 10.0, 1.0, 1.0, 3.0, 0.5);
	pos.xz /= mix(3.0, 7.0, nextFloat(rng));
	n += fbm01(pos, 4, 1.0, 1.0, 2.0, 0.5);

	float a = nextFloat(rng, 1.0, 15.0);
	float b = nextFloat(rng, 2.0, 50.0);

	n = pow(n, a);
	n *= b;
	// n = min(1.0, n);
	n = (2.0 / PI) * atan(n);

	vec4 baseColor = texture(node.colorGradient, n);
    return applyLighting(Material(baseColor.rgb * baseColor.a, baseColor.rgb, 1.0, 0.0, false), frag);
    // return applyLighting(Material(baseColor.rgb * baseColor.a, baseColor.rgb, 1.0, 1.0, false), frag);
    // return applyLighting(Material(vec3(baseColor.r) * baseColor.a, vec3(baseColor.r), baseColor.g, baseColor.b, false), frag);
	// return vec3(n);
}

#endif