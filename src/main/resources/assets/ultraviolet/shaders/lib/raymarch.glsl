#ifndef ULTRAVIOLET_RAYMARCH_H_
#define ULTRAVIOLET_RAYMARCH_H_

struct Ray {
	vec3 origin;
	vec3 dir;
};

struct Sphere {
	vec3 origin;
	float radius;
};

struct Plane {
	vec3 origin;
	vec3 normal;
};

struct HitInfo {
	// the distance from the ray origin to the surface of the object closest to the ray origin.
	float near;
	// the distance from the ray origin to the surface of the object furthest to the ray origin.
	float far;
};

HitInfo raycast(in Ray ray, in Sphere sphere) {
	// ray origin to sphere center
	vec3 sr = sphere.origin - ray.origin;

	// point on ray closest to the sphere's center (ro + t*rd)
	float t = dot(sr, ray.dir);

	// vector from closest point to sphere origin
	vec3 h = sr - ray.dir * t;

	float h2 = dot(h, h);
	float r2 = sphere.radius * sphere.radius;
	// the closest point is outside of the sphere, which means the ray completely missed
	if (h2 > r2)
		return HitInfo(-1.0, -1.0);

	// entry point/exit point, sphere center, and closest point form a right triangle,
	// use pythagorean theorem to get distance between entry point and closest point
	float p = sqrt(r2 - h2);
	return HitInfo(t - p, t + p);
}

float sdf(in vec3 pos, in Sphere sphere) {
	return distance(sphere.origin, pos) - sphere.radius;
}

float sdf(in vec3 pos, in Plane plane) {
	return dot(plane.normal, pos - plane.origin);
}

#endif