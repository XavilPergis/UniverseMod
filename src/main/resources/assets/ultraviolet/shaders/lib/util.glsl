#ifndef ULTRAVIOLET_UTIL_H_
#define ULTRAVIOLET_UTIL_H_

float luma(vec3 color) {
	return dot(color, vec3(0.299, 0.587, 0.114));
}

// The dot product of a and b, clamped between [0,inf)
float dotClamped(vec3 a, vec3 b) {
	return max(dot(a, b), 0.0);
}

// Schlick
vec3 fresnelFactor(in vec3 dir, in vec3 normal) {
	float cosTheta = dotClamped(normal, normalize(-dir));
	vec3 F0 = vec3(0.04);
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
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
