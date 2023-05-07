#ifndef UNIVERSAL_UTIL_H_
#define UNIVERSAL_UTIL_H_

#define PI      (3.1415926535897932384626433832795)
#define HALF_PI (PI / 2.0)
#define TAU     (PI * 2.0)

float saturate(in float n) { return clamp(n, 0.0, 1.0); }
vec2  saturate(in  vec2 n) { return vec2(saturate(n.x), saturate(n.y)); }
vec3  saturate(in  vec3 n) { return vec3(saturate(n.x), saturate(n.y), saturate(n.z)); }
vec4  saturate(in  vec4 n) { return vec4(saturate(n.x), saturate(n.y), saturate(n.z), saturate(n.w)); }

// The dot product of a and b, clamped between [0,inf)
float pdot(vec3 a, vec3 b) {
	return max(dot(a, b), 0.0);
}

float fresnelFactor(vec3 viewDir, vec3 normal, float exponent) {
    //get the dot product between the normal and the view direction
    float fresnel = dot(normal, viewDir);
    //invert the fresnel so the big values are on the outside
    fresnel = saturate(1 - fresnel);
    //raise the fresnel value to the exponents power to be able to adjust it
    fresnel = pow(fresnel, exponent);
	return fresnel;
}

// assumes coordinates are in view space
float fresnelFactor(vec3 normal, float exponent) {
	return fresnelFactor(vec3(0.0, 0.0, 1.0), normal, exponent);
}

#endif
