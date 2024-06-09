#ifndef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#error guard celestial object shading impls with ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
#endif

#ifndef ULTRAVIOLET_CELESTIAL_ROCKY_H_
#define ULTRAVIOLET_CELESTIAL_ROCKY_H_

struct RockyWorld {
	int seed;
	float roughness;
};

// vec3 shadeCelestialObject(inout FragmentInfo frag, in RockyWorld node) {
// 	Material mat = DEFAULT_MATERIAL;

// 	float colnoise = 0.0;

// 	float maxAmplitude = 0.0;
// 	float amp = 1.0;
// 	float freq = 1.0;
// 	for (int i = 0; i < 8; ++i) {
// 		colnoise += amp * noiseSimplex(float(node.seed % 1000) + freq * frag.normalM);
// 		maxAmplitude += amp;
// 		freq *= 2.0;
// 		amp *= 0.6;
// 	}

// 	colnoise /= maxAmplitude;
// 	colnoise = colnoise * 0.5 + 0.5;

// 	mat.albedo = mix(vec3(0.4), vec3(0.8), smoothstep(0.3, 0.6, colnoise));

// 	mat.roughness = node.roughness;
// 	mat.metallic = 0.0;

// 	return applyLighting(mat, frag);
// 	// return mat.albedo;
// }

float rockyGetTerrainHeight(in vec3 pos, int seed) {
	float seedOffset = float(seed % 1000);

	float amplitude = 0.0;
	float maxAmplitude = 0.0;
	float currentAmp = 1.0;
	float currentFreq = 1.0;
	for (int i = 0; i < 8; ++i) {
		amplitude += currentAmp * noiseSimplex(seedOffset + currentFreq * pos);
		maxAmplitude += currentAmp;
		currentFreq *= 2.0;
		currentAmp *= 0.6;
	}

	amplitude /= maxAmplitude;
	amplitude = amplitude * 0.5 + 0.5;

	return amplitude;
}

// TODO: floating point error?

vec2 proj(vec3 P) {
	// orthographic projection for front of sphere
	if (P.z >= 0) return P.xy;

	float len = length(P.xy);

	// singularity at the very back of the sphere. Each corner approaches this point,
	// so we just pick one and call it a day.
	if (len == 0) return vec2(1, 1);

	// otherwise... unwrap the back hemisphere into the remaining space in the corners.
	vec2 N = normalize(P.xy);

	// distance along N to edge of enclosing box, the minimum of the distance between each
	// line (x=1 and y=1). Since we only care about distance here, we take the abs of the
	// normal to contrain it to the first quadrant where this math works out.
	float d = min(1 / abs(N.x), 1 / abs(N.y));

	// a measure of how close the point is to the border of the unit circle. k=0 means it's
	// directly on the unit circle, and k=1 means it's directly on the origin.
	// k=1 is a degenerate case, though, where P.xy would be 0 and a normal cannot be derived.
	float k = 1 - len;

	// this can be thought of in multiple ways:
	// a) a simplification of `mix(N, d * N, k)`, interpolating between the point on the
	//    unit circle and the point on the box edge based on k.
	// b) `d - 1` is the length between the unit circle's edge and the box's edge. multiplying
	//    by k gets us the offset of our desired point from the circle's edge, so adding this
	//    to N gets our desired point.
	return N * (1 + k * (d - 1));
}

vec3 unproj(vec2 P) {
	float len = length(P);

	// front of sphere, easy unprojection.
	if (len <= 1) {
		// we threw away the z component in the projection, but kept the x and y components
		// unchanged. since we're projecting onto a unit sphere, we have enough information
		// to recover the z component.
		float z = sqrt(1 - dot(P, P));
		return vec3(P, z);
	}

	// this projection has the property that for a point on the sphere, its corresponding
	// projected points lay on the line from the origin through its projection onto the XY plane.

	// normal and distance to box edge like we have in `proj`
	vec2 N = normalize(P);
	float d = min(1 / abs(N.x), 1 / abs(N.y));

	// note that this division may cause singularities, but only when N is cardinally aligned.
	// in this case, though, it doesn't matter since the unit circle is tangent to the box at
	// these points, meaning `len` is always <= 1, so we never get here.
	float k = (len - 1) / (d - 1);

	// the point we would have gotten if we projected a 3d point with the same xy values but on
	// the front hemisphere. we can then derive z like we did earlier in this function.
	vec2 Q = N * (1 - k);

	float z = sqrt(1 - dot(Q, Q));
	// reverse the z, we're on the back side of the sphere!
	return vec3(Q, -z);
}

vec3 shadeCelestialObject(inout FragmentInfo frag, in RockyWorld node) {
	Material mat = DEFAULT_MATERIAL;

	float noise = rockyGetTerrainHeight(frag.normalM, node.seed);

	// mat.albedo = mix(vec3(0.4), vec3(0.8), smoothstep(0.3, 0.6, noise));
	mat.albedo = vec3(noise);

	mat.roughness = node.roughness;
	mat.metallic = 0.0;

	vec3 normW = frag.normalW + 0.1 * (2.0 * noise - 1.0) * vec3(1.0, 0.0, 0.0);

	frag.lightingNormalV = (uViewMatrix * vec4(normW.xyz, 1.0)).xyz;
	frag.lightingNormalV = normalize(frag.lightingNormalV);

	return applyLighting(mat, frag);
	// return mat.albedo;
}

#endif