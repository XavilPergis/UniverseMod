#stages vertex fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]
#include [ultraviolet:lib/lighting.glsl]
#include [ultraviolet:common_uniforms.glsl]
#include [ultraviolet:lib/tonemap.glsl]

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

uniform int uPlanetType;
uniform float uRenderingSeed;

uniform vec3 uRotationAxis;
uniform float uRotationAngle;

out vec4 fColor;

#define PLANET_TYPE_EARTH_LIKE_WORLD 0
#define PLANET_TYPE_GAS_GIANT 1
#define PLANET_TYPE_ICE_WORLD 2
#define PLANET_TYPE_ROCKY_ICE_WORLD 3
#define PLANET_TYPE_ROCKY_WORLD 4
#define PLANET_TYPE_WATER_WORLD 5
#define PLANET_TYPE_BROWN_DWARF 6

// =============== GAS GIANT SHADING ===============

float fbm01(in vec3 pos, in int N, in float fI, in float aI, in float fS, in float aS) {
	float freq = fI;
	float amp = aI;
	float ampTotal = 0.0;
	float total = 0.0;
	for (int i = 0; i < N; ++i) {
		total += amp * noiseSimplex(freq * pos);
		freq *= fS;
		ampTotal += amp;
		amp *= aS;
	}
	return (total / ampTotal) * 0.5 + 0.5;
}

vec2 fbm2(in vec3 pos, in int N, in float seed, in float fI, in float aI, in float fS, in float aS) {
	vec2 off = vec2(0.0);
	off.x += fbm01(pos + seed, N, fI, aI, fS, aS);
	off.y += fbm01(pos - seed, N, fI, aI, fS, aS);
	return off;
}

Material gasGiantField(in vec3 pos, in float seed) {
	float n = 0.0;
	pos.y *= mix(5.0, 7.0, pow(rand(seed), 2.0));
	pos.y += seed;

	pos.xz += 1.0 * fbm2(pos, 4, 10.0, 1.0, 1.0, 4.0, 0.5);
	pos.xz += 2.0 * fbm2(pos, 4, 10.0, 1.0, 1.0, 3.0, 0.5);
	pos.xz /= mix(3.0, 7.0, rand(seed + 1.0));
	n += fbm01(pos, 4, 1.0, 1.0, 2.0, 0.5);

	// fbm2(p + fbm2(p + fbm2(p)));

	// vec3 offpos = pos;
	// vec2 off = vec2(0.0);
	// off.x += fbm01(offpos + 40.0, 4, 1.0, 1.0, 3.3, 0.5);
	// off.y += fbm01(offpos - 40.0, 4, 1.0, 1.0, 3.3, 0.5);
	// off *= 10.0;
	// offpos.xz += off;
	// offpos.xz /= 4.0;

	// vec2 off2 = vec2(0.0);
	// off2.x += fbm01(offpos + 20.0, 5, 1.0, 1.0, 2.3, 0.6);
	// off2.y += fbm01(offpos - 20.0, 5, 1.0, 1.0, 2.3, 0.6);
	// off2 *= 1.0;
	// pos.xz += off2;
	// pos.xz /= 4.0;


	n = pow(n, 1.0);

	vec4 A = vec4(0.92, 0.90, 0.83, 0.0);
	// vec4 B = vec4(0.33, 0.29, 0.21, 0.0);
	vec4 B = 2.5 * vec4(1.0, 0.2, 0.0, 4.0);
	vec4 C = vec4(0.22, 0.19, 0.16, 0.0);
	vec4 D = vec4(0.35, 0.22, 0.28, 0.2);

	float c1 = 0.9;
	float c2 = 0.5;
	float c3 = 0.1;

	vec4 col = vec4(0.0);
	col += A * smoothstep(c1, 1.0, n);
	col += B * smoothstep(c2, c1, n) * smoothstep(1.0, c1, n);
	col += C * smoothstep(c3, c2, n) * smoothstep(c1, c2, n);
	col += D * smoothstep(0.0, c3, n) * smoothstep(c2, c3, n);

    return Material(2e25 * col.rgb * col.a, col.rgb, 1.0, 0.0);
}

// =============== === ===== ======= ===============

vec3 noiseSimplex3(vec3 p) {
	vec3 r = vec3(0.0);
	r.x = noiseSimplex(mod(uRenderingSeed, 1000.0) + p + 10);
	r.y = noiseSimplex(mod(uRenderingSeed, 1000.0) + p + 20);
	r.z = noiseSimplex(mod(uRenderingSeed, 1000.0) + p + 30);
	return r;
}

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(uViewMatrix) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / HALF_PI;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

Material gasGiantBaseColor(vec3 pos) {
	return gasGiantField(pos, uRenderingSeed / 1000.0);
}

Light makeStarLight(in vec3 posWorld, in vec4 color, in float radius) {
	return makeSphereLight(posWorld, color.rgb * color.a * 3.827e26, radius);
	// return makePointLight(posWorld, color.rgb * color.a);
	// return makePointLight(posWorld, color.rgb * color.a * 100.0);
	// return makePointLight(posWorld, vec3(1.0e3));
}

float sampleHeight(vec3 normWorld) {
	float height = 0.0;
	height += noiseSimplex(1.0 * normWorld);
	height += 0.5 * noiseSimplex(2.5 * normWorld);
	height += 0.3 * noiseSimplex(5.0 * normWorld);
	height += 0.1 * noiseSimplex(20.0 * normWorld);
	height += 0.05 * noiseSimplex(80.0 * normWorld);
	// height += 0.05 * noiseSimplex(150.0 * normWorld);
	return height;
}

Material shadeElw(vec3 fragPosWorld, vec3 normWorld, inout vec3 newNorm) {
	float hc = sampleHeight(normWorld);

	if (hc >= 0.0) {
		float colnoise = noiseSimplex(normWorld + 10.0) + 0.5 * noiseSimplex(5.0 * normWorld + 10.0);
		colnoise /= 1.5;
		colnoise = sign(colnoise) * pow(abs(colnoise), 0.6);
		colnoise = colnoise * 0.5 + 0.5;
		vec3 col = mix(vec3(0.0392, 0.0745, 0.0314), vec3(0.2941, 0.1608, 0.1176), colnoise);
		vec3 normOffset = vec3(0.0);
		normOffset += 0.2 * noiseSimplex3(10.0 * normWorld);
		normOffset += 0.1 * noiseSimplex3(30.0 * normWorld);
		// normOffset += 0.05 * noiseSimplex3(400.0 * normWorld);
		normOffset *= min(hc, 1.0) * 1.5;
		newNorm = normalize(newNorm + normOffset);
		// return Material(vec3(0.0), vec3(0.3, 1.0, 0.3), 1.0, 0.0);
		return Material(vec3(0.0), 5.0 * col, 1.0, 0.5);
	}
	return Material(vec3(0.0), 3.0 * vec3(0.0196, 0.0588, 0.1255), 0.3, 0.0);
}
Material shadeBrownDwarf(vec3 fragPosWorld, vec3 normWorld) {
	Material mat = gasGiantBaseColor(normWorld);
	mat.emissiveFlux += 5e24 * vec3(1.0, 0.2, 0.0);
	return mat;
}
Material shadeGasGiant(vec3 fragPosWorld, vec3 normWorld) {
	Material mat = gasGiantBaseColor(normWorld);
	mat.emissiveFlux = vec3(0.0);
	return mat;
}
Material shadeIceWorld(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}
Material shadeRockyIceWorld(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}
Material shadeRocky(vec3 fragPosWorld, vec3 normWorld) {
	float colnoise = 0.0;

	float maxAmplitude = 0.0;
	float amp = 1.0;
	float freq = 1.0;
	for (int i = 0; i < 8; ++i) {
		colnoise += amp * noiseSimplex(mod(uRenderingSeed, 1000.0) + freq * normWorld);
		maxAmplitude += amp;
		freq *= 2.0;
		amp *= 0.6;
	}

	colnoise /= maxAmplitude;
	colnoise = colnoise * 0.5 + 0.5;

	vec3 col = mix(vec3(0.4), vec3(0.8), smoothstep(0.3, 0.6, colnoise));
	return Material(vec3(0.0), col, 1.0, 0.0);
}
Material shadeWater(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}

Material shadePlanet(vec3 fragPosWorld, vec3 normWorld, inout vec3 newNorm) {
	// return Material(vec3(0.0), vec3(0.1, 0.2, 1.0), 0.5, 0.0);

	if (uPlanetType == PLANET_TYPE_EARTH_LIKE_WORLD) return shadeElw(fragPosWorld, normWorld, newNorm);
	else if (uPlanetType == PLANET_TYPE_GAS_GIANT) return shadeGasGiant(fragPosWorld, normWorld);
	else if (uPlanetType == PLANET_TYPE_BROWN_DWARF) return shadeBrownDwarf(fragPosWorld, normWorld);
	else if (uPlanetType == PLANET_TYPE_ICE_WORLD) return shadeIceWorld(fragPosWorld, normWorld);
	else if (uPlanetType == PLANET_TYPE_ROCKY_ICE_WORLD) return shadeRockyIceWorld(fragPosWorld, normWorld);
	else if (uPlanetType == PLANET_TYPE_ROCKY_WORLD) return shadeRocky(fragPosWorld, normWorld);
	else if (uPlanetType == PLANET_TYPE_WATER_WORLD) return shadeWater(fragPosWorld, normWorld);
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}

mat4 rotationMatrix(vec3 axis, float angle)
{
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;
    
    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
                oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
                oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
                0.0,                                0.0,                                0.0,                                1.0);
}

void main() {
	// fColor = vec4(1.0, 0.0, 1.0, 1.0);
	// fColor = vec4(normal.xyz * 0.5 + 0.5, 1.0);
	// fColor = vec4(normalRaw.xyz * 0.5 + 0.5, 1.0);
	// fColor = vec4(vertexColor.rgb, 1.0);
	// fColor = vec4(texCoord0.x, 0.0, texCoord0.y, 1.0);

	mat4 rotMat = rotationMatrix(uRotationAxis, -uRotationAngle);

	vec3 normWorld = (inverse(uViewMatrix) * normalize(normal)).xyz;
	vec3 posWorld = (inverse(uViewMatrix) * normalize(vertexPos)).xyz;

	vec3 sp = normalize((rotMat * vec4(normWorld, 1.0)).xyz);

	vec3 newNorm = normWorld;
	Material material = shadePlanet(posWorld, sp, newNorm);

	// if (uPlanetType == PLANET_TYPE_ICE_WORLD || uPlanetType == PLANET_TYPE_ROCKY_ICE_WORLD || uPlanetType == PLANET_TYPE_ROCKY_WORLD) {
	// 	vec3 normOffset = vec3(0.0);
	// 	normOffset += 0.2 * noiseSimplex3(30.0 * sp);
	// 	normOffset += 0.1 * noiseSimplex3(100.0 * sp);
	// 	normOffset += 0.05 * noiseSimplex3(400.0 * sp);
	// 	normOffset *= 0.1;
	// 	newNorm = normalize(newNorm + normOffset);
	// }
	LightingContext ctx = makeLightingContext(material, uMetersPerUnit, uCameraPos, posWorld, newNorm);

	vec3 res = vec3(0.0);
	Light l0 = makeStarLight(uLightPos0.xyz, uLightColor0, 1.0 * uLightRadius0);
	res += lightContribution(ctx, l0);
	Light l1 = makeStarLight(uLightPos1.xyz, uLightColor1, 1.0 * uLightRadius1);
	res += lightContribution(ctx, l1);
	res += material.emissiveFlux;
	vec3 finalColor = res;

	float exposure = 5e-26;
	finalColor *= exposure;

	// finalColor = tonemapACESFull(finalColor);
	// finalColor = pow(finalColor, vec3(1.0 / 2.2));

    fColor = vec4(finalColor, 1);
}

#endif
