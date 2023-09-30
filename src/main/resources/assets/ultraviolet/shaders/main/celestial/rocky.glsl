#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_ROCKY_H_
#define ULTRAVIOLET_CELESTIAL_ROCKY_H_

struct RockyWorld {
	int seed;
	float roughness;
};

vec3 shadeCelestialObject(inout FragmentInfo frag, in RockyWorld node) {
	Material mat = DEFAULT_MATERIAL;

	float colnoise = 0.0;

	float maxAmplitude = 0.0;
	float amp = 1.0;
	float freq = 1.0;
	for (int i = 0; i < 8; ++i) {
		colnoise += amp * noiseSimplex(float(node.seed % 1000) + freq * frag.normalM);
		maxAmplitude += amp;
		freq *= 2.0;
		amp *= 0.6;
	}

	colnoise /= maxAmplitude;
	colnoise = colnoise * 0.5 + 0.5;

	mat.albedo = mix(vec3(0.4), vec3(0.8), smoothstep(0.3, 0.6, colnoise));

	mat.roughness = node.roughness;
	mat.metallic = 0.0;

	return applyLighting(mat, frag);
	// return mat.albedo;
}

#endif