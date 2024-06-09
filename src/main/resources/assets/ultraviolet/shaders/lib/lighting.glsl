#ifndef ULTRAVIOLET_LIGHTING_H_
#define ULTRAVIOLET_LIGHTING_H_

#include [ultraviolet:lib/util.glsl]

struct Material {
	vec3 emission;
	vec3 albedo;
	float roughness;
	float metallic;
	bool fresnel;
};

const Material DEFAULT_MATERIAL = Material(vec3(0.0), vec3(0.0), 1.0, 0.0, true);

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
	vec3 fragPosV;
	vec3 normalV;
};

LightingContext makeLightingContext(in Material material, in float metersPerUnit, in vec3 fragPosV, in vec3 normalV) {
	return LightingContext(material, vec3(0.0), metersPerUnit, fragPosV, normalV);
}

Light makePointLight(vec3 pos, vec3 radiantFlux) {
	return Light(LIGHT_TYPE_POINT, pos, radiantFlux, 0.0);
}

Light makeSphereLight(vec3 pos, vec3 radiantFlux, float radius) {
	return Light(LIGHT_TYPE_SPHERE, pos, radiantFlux, radius);
}

// GGX - Normal Distribution Factor
float microfacetOrientationFactor(in LightingContext ctx, in Light light, in vec3 halfway) {
    float a2 = pow(ctx.material.roughness, 4.0);
    float NdotH2 = pow(dotClamped(ctx.normalV, halfway), 2.0);
	return a2 / (PI * pow(NdotH2 * (a2 - 1.0) + 1.0, 2.0));
}

// float geometrySchlickGGX(in float NdotV, in float roughness) {
//     float k = pow(roughness + 1.0, 2.0) / 8.0;
//     return NdotV / (NdotV * (1.0 - k) + k);
// }

// // smith
// float microfacetOcclusionFactor(in LightingContext ctx, in vec3 toLight) {
// 	float res = 1.0;
//     res *= geometrySchlickGGX(dotClamped(ctx.normalV, normalize(-ctx.fragPosV)), ctx.material.roughness);
//     res *= geometrySchlickGGX(dotClamped(ctx.normalV, toLight), ctx.material.roughness);
//     return res;
// }

// // Schlick
// vec3 fresnelFactor(in float cosTheta, in vec3 F0, in vec3 F90) {
//     // return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
// 	return lerp(pow(1.0 - cosTheta, 5.0), F0, F90);
// }

// Schlick
float fresnelFactor(in float cosTheta) {
	return pow(1.0 - cosTheta, 5.0);
}

// GGX - Normal Distribution Factor
float microfacetOrientationFactor(in Material material, in float NdotH) {
    float a2 = pow(material.roughness, 2.0);
	float n = (a2 - 1.0) * NdotH * NdotH + 1.0;
	return a2 / (PI * n * n);
}

float geometrySchlickGGX(in float NdotV, in float roughness) {
    float k = pow(roughness + 1.0, 2.0) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

// smith
float microfacetOcclusionFactor(in Material material, in float NdotV, in float NdotL) {
	float res = 1.0;
    res *= geometrySchlickGGX(NdotV, material.roughness);
    res *= geometrySchlickGGX(NdotL, material.roughness);
    return res;
}

vec3 brdf(in Material material, in vec3 V, in vec3 L, in vec3 N) {
	vec3 H = normalize(L + V);

	float NdotL = dotClamped(N, L);
	float NdotV = dotClamped(N, V);
	float NdotH = dotClamped(N, H);
	float LdotH = dotClamped(L, H);

	float FL = fresnelFactor(NdotL), FV = fresnelFactor(NdotV);
	float F90 = 0.5 + 2.0 * material.roughness * pow(LdotH, 2.0);
	float Fdiffuse = lerp(FL, 1.0, F90) * lerp(FV, 1.0, F90);

	vec3 diffuse = (1.0 - material.metallic) * material.albedo / PI * Fdiffuse;

	float Cdlum = luma(material.albedo);
	vec3 Ctint = Cdlum > 0.0f ? material.albedo / Cdlum : vec3(1.0);
	// vec3 Cspec0 = lerp(material.metallic, _Specular * 0.08f * lerp(_SpecularTint, 1.0, Ctint), surfaceColor);
	vec3 Cspec0 = lerp(material.metallic, 0.08 * Ctint, material.albedo);

	float specNum = 1.0;
	// attenuation due to microfacet alignment
	specNum *= microfacetOrientationFactor(material, NdotH);
	specNum *= microfacetOcclusionFactor(material, NdotV, NdotL);
	specNum *= lerp(fresnelFactor(LdotH), 1.0, 0.0);

	return diffuse + Cspec0 * vec3(specNum);
	// return vec3(specNum);
	// return vec3(NdotH);
}

// vec3 brdfCookTorrance(in LightingContext ctx, in Light light, in vec3 toLight) {
// 	vec3 halfway = normalize(normalize(-ctx.fragPosV) + toLight);

// 	// diffuse reflections happen when incoming light penetrates the surface of a material and
// 	// scatters internally, before making its way out. This orients the outgoing rays in random
// 	// directions.
// 	vec3 diffuse = ctx.material.albedo / PI;
// 	// when light scatters diffusely within a conductor, that light is absorbed. because of energy
// 	// conservation, absorbed light cannot be emitted diffusely.
// 	diffuse *= 1.0 - ctx.material.metallic;

// 	// specular reflections happen when incoming light is reflected completely by the surface of a
// 	// material. Surfaces are made up of lots and lots of "microfacets", which are something like
// 	// tiny mirrors that reflect light perfectly across their normal. The scale of these
// 	// microfacets are very small in relation to the size of a pixel, so the effect of all these
// 	// microfacets in total is approximated. Rough surfaces have their microfacets oriented in
// 	// random directions that scatter incoming light in essentially all directions, while perfectly
// 	// smooth surfaces have their microfacets all pointing in the same (or at least very similar)
// 	// directions.
// 	vec3 specular = vec3(1.0);
// 	specular *= microfacetOrientationFactor(ctx, light, halfway);
// 	specular *= microfacetOcclusionFactor(ctx, toLight);
// 	specular /= max(4.0 * dotClamped(normalize(-ctx.fragPosV), ctx.normalV) * dotClamped(toLight, ctx.normalV), 1e-6);

// 	// D(x) * F(theta) * G(x) / (4 * cos(theta_l) * cos(theta_v))

// 	vec3 F0 = mix(vec3(0.04), ctx.material.albedo, ctx.material.metallic);
// 	vec3 fresnel = F0;
// 	if (ctx.material.fresnel) {
// 		fresnel = fresnelFactor(dotClamped(ctx.normalV, normalize(-ctx.fragPosV)), F0);
// 	}
// 	// return fresnel;
// 	vec3 res = mix(diffuse, specular, fresnel);

// 	return res;
// }

const float MAX_RADIANCE = 100.0;

vec3 getIncomingLightRadiance(in LightingContext ctx, in Light light) {
	float d = (ctx.metersPerUnit / 1e12) * distance(light.pos, ctx.fragPosV);
	float lightFalloff = MAX_RADIANCE / (d * d + 1.0);
	return min(vec3(MAX_RADIANCE), light.radiantFlux * lightFalloff);
}

vec3 lightContribution(in LightingContext ctx, in Light light) {
	vec3 radiance = getIncomingLightRadiance(ctx, light);
	if (length(radiance) < 1e-6)
		return vec3(0.0);

	vec3 L = normalize(light.pos - ctx.fragPosV);
	vec3 V = normalize(-ctx.fragPosV);
	vec3 N = normalize(ctx.normalV);

	vec3 lightContribution = radiance;
	lightContribution *= dotClamped(ctx.normalV, L);
	lightContribution *= brdf(ctx.material, V, L, N);
	// lightContribution *= brdfCookTorrance(ctx, light, L);
	// vec3 lightContribution = brdf(ctx.material, V, L, N);

	return lightContribution;
}

#endif
