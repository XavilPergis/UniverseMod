#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_ELW_H_
#define ULTRAVIOLET_CELESTIAL_ELW_H_

struct EarthLikeWorld {
	int seed;
	float waterCoverage;
};

vec3 shadeCelestialObject(inout FragmentInfo frag, in EarthLikeWorld node) {
	Material mat = DEFAULT_MATERIAL;

	float height = 0.0;

	float maxAmplitude = 0.0;
	float amp = 1.0;
	float freq = 1.0;
	for (int i = 0; i < 8; ++i) {
		height += amp * noiseSimplex(float(node.seed % 1000) + freq * frag.normalM);
		maxAmplitude += amp;
		freq *= 2.0;
		amp *= 0.6;
	}

	height /= maxAmplitude;
	height = height * 0.5 + 0.5;

	if (height <= node.waterCoverage) {
		// water
		mat.albedo = vec3(0.0196, 0.3451, 0.6118);
		mat.roughness = 0.3;
		mat.metallic = 0.3;
	} else {
		// land ho
		mat.albedo = vec3(0.4, 0.7333, 0.0902);
		mat.roughness = 0.95;
		mat.metallic = 0.0;
	}


	return applyLighting(mat, frag);
	// return mat.albedo;
}

#endif