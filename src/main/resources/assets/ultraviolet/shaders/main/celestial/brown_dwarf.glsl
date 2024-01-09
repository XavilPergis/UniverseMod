#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_BROWN_DWARF_H_
#define ULTRAVIOLET_CELESTIAL_BROWN_DWARF_H_

struct BrownDwarf {
	int seed;
};

vec3 shadeCelestialObject(inout FragmentInfo frag, in BrownDwarf node) {
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
	n = (2.0 / PI) * atan(n);

	vec4 baseColor = mix(vec4(0.3529, 0.1137, 0.1137, 1.12), vec4(0.4863, 0.1373, 0.0314, 0.02), n);

	vec3 emissive = baseColor.rgb * baseColor.a * 3.0;
	emissive *= 1.0 + (60.0 * fresnelFactor(normalize(frag.posV), frag.normalV));

    return applyLighting(Material(emissive, baseColor.rgb, 1.0, 0.0, false), frag);
}

#endif