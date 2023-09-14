#stages vertex fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE

#include [ultraviolet:common_uniforms.glsl]
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]
#include [ultraviolet:lib/lighting.glsl]

uniform vec4 uLightPos0;
uniform vec4 uLightColor0;
uniform float uLightRadius0;
uniform vec4 uLightPos1;
uniform vec4 uLightColor1;
uniform float uLightRadius1;
uniform vec4 uLightPos2;
uniform vec4 uLightColor2;
uniform float uLightRadius2;
uniform vec4 uLightPos3;
uniform vec4 uLightColor3;
uniform float uLightRadius3;

uniform vec3 uParentPos;
uniform float uParentRadius;

out vec4 fColor;

vec2 raySphereIntersect(vec3 r0, vec3 rd, vec3 s0, float sr) {
    float a = dot(rd, rd);
    vec3 s0_r0 = r0 - s0;
    float b = 2.0 * dot(rd, s0_r0);
    float c = dot(s0_r0, s0_r0) - (sr * sr);
	float disc = b * b - 4.0 * a* c;
    if (disc < 0.0) {
        return vec2(-1.0, -1.0);
    }else{
		return vec2(-b - sqrt(disc), -b + sqrt(disc)) / (2.0 * a);
	}
}

// vec3 contribution(vec4 color, vec4 starPos) {
// 	if (color.a >= 0) {
// 		vec3 p0 = (uViewMatrix * vec4(starPos.xyz, 1.0)).xyz;

// 		float distanceMeters = distance(vertexPos.xyz, p0) * uMetersPerUnit;
// 		float d2 = 1.0 / pow(distanceMeters / 1e14, 2.0);
// 		float light = min(10.0, color.a * d2);

// 		// NOTE: rings are made of a bunch of dust and debris and stuff, and that stuff can
// 		// reflect light back even at grazing angles, so we don't do a dot product here.
// 		// TODO: its more complicated than this
// 		return 0.5 * color.rgb * light;
// 	}
// 	return vec3(0);
// }

float fbm(float t) {
	float res = 0.0;
	float a = 1.0;
	float f = 1.0;
	float maxValue = 0.0;
	for (int i = 0; i < 8; ++i) {
		res += a * noiseSimplex(vec2(f * t, float(i)));
		maxValue += a;
		a /= 2.0;
		f *= 1.5;
	}
	return res / maxValue;
}

vec4 shadeRing(float t) {
	float k = 0.6;
	float intensity = 4.0 * k * fbm(t) - k + 1.0;
	return vec4(vec3(0.5), saturate(intensity));
}

Light makeStarLight(in vec3 posWorld, in vec4 color, in float radius) {
	return makeSphereLight(posWorld, color.rgb * color.a, radius);
}

vec3 contrib(Material material, vec3 lightPos, vec4 lightColor, float lightRadius) {
	vec3 posWorld = (inverse(uViewMatrix) * normalize(vertexPos)).xyz;
	LightingContext ctx = makeLightingContext(material, uMetersPerUnit, uCameraPos, posWorld, normalize(lightPos - posWorld));
	vec3 res = lightContribution(ctx, makeStarLight(lightPos, lightColor, lightRadius));

	float parentRadius_u = uParentRadius * 1000.0 / uMetersPerUnit;
	vec2 ts = raySphereIntersect(posWorld, normalize(lightPos.xyz - posWorld), uParentPos, parentRadius_u);
	if (ts.x >= 0.0) {
		res *= 0.0;
	}

	return 0.5 * res;
}

void main() {
	float gamma = 2.2;
    // vec4 baseColor = texture(Sampler0, texCoord0) * vertexColor;
	// baseColor.rgb = pow(baseColor.rgb, vec3(gamma));

    // if (baseColor.a < 0.1) {
    //     discard;
    // }

	vec3 normWorld = (inverse(uViewMatrix) * normalize(normal)).xyz;
	vec3 posWorld = (inverse(uViewMatrix) * normalize(vertexPos)).xyz;

	Material material = Material(vec3(0.0), vec3(1.0), 0.5, 0.0);

	// LightingContext ctx = makeLightingContext(material, uMetersPerUnit, uCameraPos, posWorld, normWorld);
	vec3 res = vec3(0.0);
	res += contrib(material, uLightPos0.xyz, uLightColor0, uLightRadius0);
	res += contrib(material, uLightPos1.xyz, uLightColor1, uLightRadius1);
	res += contrib(material, uLightPos2.xyz, uLightColor2, uLightRadius2);
	res += contrib(material, uLightPos3.xyz, uLightColor3, uLightRadius3);
	res += material.emissiveFlux;


	// float parentRadius_u = uParentRadius * 1000.0 / uMetersPerUnit;
	// vec2 ts = raySphereIntersect(posWorld, normalize(starPos.xyz - posWorld), uParentPos, parentRadius_u);
	// if (ts.x >= 0.0) {
	// 	light *= 0.0;
	// }

	vec4 baseColor = shadeRing(texCoord0.y);
	vec3 finalColor = res * baseColor.rgb;

	float exposure = 1.0;
	// finalColor = 1.0 - exp(-finalColor * exposure);
	finalColor *= exposure;

    // finalColor = pow(finalColor, vec3(1.0 / gamma));
	// finalColor = acesTonemap(finalColor);

    fColor = vec4(finalColor, baseColor.a);
}

#endif
