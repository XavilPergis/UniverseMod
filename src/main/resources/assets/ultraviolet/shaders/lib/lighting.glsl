#ifndef ULTRAVIOLET_LIGHTING_H_
#define ULTRAVIOLET_LIGHTING_H_

#include [ultraviolet:lib/util.glsl]

struct Material {
	vec3 emissiveFlux;
	vec3 albedo;
	float roughness;
	float metallicity;
};

#define LIGHT_TYPE_POINT 0
#define LIGHT_TYPE_SPHERE 1

struct Light {
	int type;
	vec3 pos;
	vec3 radiantFlux;
	// type:SPHERE
	float lightRadius;
};

struct LightingContext {
	Material material;
	vec3 totalRadiance;
	float metersPerUnit;
	vec3 eyePos;
	vec3 fragPos;
	vec3 toEye;
	vec3 normal;
};

LightingContext makeLightingContext(in Material material, in float metersPerUnit, in vec3 eyePos, in vec3 fragPos, in vec3 normal) {
	vec3 toEye = normalize(eyePos - fragPos);
	return LightingContext(material, vec3(0.0), metersPerUnit, eyePos, fragPos, toEye, normal);
}

Light makePointLight(vec3 pos, vec3 radiantFlux) {
	return Light(LIGHT_TYPE_POINT, pos, radiantFlux, 0.0);
}

// GGX - Normal Distribution Factor
float microfacetOrientationFactor(in LightingContext ctx, in Light light, in vec3 halfway) {
    float a2 = pow(ctx.material.roughness, 4.0);
    float NdotH2 = pow(pdot(ctx.normal, halfway), 2.0);
	return a2 / (PI * pow(NdotH2 * (a2 - 1.0) + 1.0, 2.0));
}

float geometrySchlickGGX(in float NdotV, in float roughness) {
    float k = pow(roughness + 1.0, 2.0) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

// smith
float microfacetOcclusionFactor(in LightingContext ctx, in vec3 toLight) {
	float res = 1.0;
    res *= geometrySchlickGGX(pdot(ctx.normal, ctx.toEye), ctx.material.roughness);
    res *= geometrySchlickGGX(pdot(ctx.normal, toLight), ctx.material.roughness);
    return res;
}

// Schlick
vec3 fresnelFactor(in float cosTheta, in vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

vec3 brdfCookTorrance(in LightingContext ctx, in Light light, in vec3 toLight) {
	vec3 halfway = normalize(ctx.toEye + toLight);

	// diffuse reflections happen when incoming light penetrates the surface of a material and
	// scatters internally, before making its way out. This orients the outgoing rays in random
	// directions.
	vec3 diffuse = ctx.material.albedo / PI;
	// when light scatters diffusely within a conductor, that light is absorbed. because of energy
	// conservation, absorbed light cannot be emitted diffusely.
	diffuse *= 1.0 - ctx.material.metallicity;

	// specular reflections happen when incoming light is reflected completely by the surface of a
	// material. Surfaces are made up of lots and lots of "microfacets", which are something like
	// tiny mirrors that reflect light perfectly across their normal. The scale of these
	// microfacets are very small in relation to the size of a pixel, so the effect of all these
	// microfacets in total is approximated. Rough surfaces have their microfacets oriented in
	// random directions that scatter incoming light in essentially all directions, while perfectly
	// smooth surfaces have their microfacets all pointing in the same (or at least very similar)
	// directions.
	vec3 specular = vec3(1.0);
	specular *= microfacetOrientationFactor(ctx, light, halfway);
	specular *= microfacetOcclusionFactor(ctx, toLight);
	specular /= max(4.0 * pdot(ctx.toEye, ctx.normal) * pdot(toLight, ctx.normal), 1e-6);

    vec3 F0 = mix(vec3(0.04), ctx.material.albedo, ctx.material.metallicity);
	// vec3 fresnel = fresnelFactor(pdot(ctx.normal, ctx.toEye), F0);
	vec3 fresnel = F0;
	// return fresnel;
	return mix(diffuse, specular, fresnel);
}

vec3 lightContribution(in LightingContext ctx, in Light light) {
	// if (length(light.radiantFlux) == 0) return vec3(0.0);

	vec3 toLight = normalize(light.pos - ctx.fragPos);

	float d = (2.0 * ctx.metersPerUnit / 1e12) * distance(light.pos, ctx.fragPos);
	// vec3 radiance = light.radiantFlux / (d * d);
	vec3 radiance = light.radiantFlux / d;
	// vec3 radiance = light.radiantFlux;

	vec3 lightContribution = vec3(1.0);
	lightContribution *= brdfCookTorrance(ctx, light, toLight);
	lightContribution *= radiance;
	lightContribution *= pdot(ctx.normal, toLight);

	return lightContribution;
}

#endif
