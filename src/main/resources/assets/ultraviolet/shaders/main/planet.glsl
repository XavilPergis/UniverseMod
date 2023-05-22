#stages vertex fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]
#include [ultraviolet:lib/lighting.glsl]
#include [ultraviolet:common_uniforms.glsl]

uniform vec4 ColorModulator;

uniform mat4 ModelViewMat;

uniform vec4 LightPos0;
uniform vec4 LightColor0;
uniform vec4 LightPos1;
uniform vec4 LightColor1;
uniform vec4 LightPos2;
uniform vec4 LightColor2;
uniform vec4 LightPos3;
uniform vec4 LightColor3;
uniform float MetersPerUnit;

uniform int PlanetType;
uniform float RenderingSeed;
uniform float Time;

out vec4 fragColor;

#define PLANET_TYPE_EARTH_LIKE_WORLD 0
#define PLANET_TYPE_GAS_GIANT 1
#define PLANET_TYPE_ICE_WORLD 2
#define PLANET_TYPE_ROCKY_ICE_WORLD 3
#define PLANET_TYPE_ROCKY_WORLD 4
#define PLANET_TYPE_WATER_WORLD 5

// vec3 contribution(vec4 color, vec4 pos) {
// 	if (color.a >= 0) {
// 		vec3 starPos = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;
// 		vec3 toStar = normalize(starPos - vertexPos.xyz);
// 		float starDistanceMeters = distance(vertexPos.xyz, starPos) * MetersPerUnit;
// 		float fragDistanceMeters = length(vertexPos.xyz) * MetersPerUnit;

// 		float receivedIntensity = (color.a * 3.827e26) / (4 * PI * pow(starDistanceMeters, 2.0));
// 		// float receivedIntensity = (color.a * 3.827e13) / (4 * PI * pow(starDistanceMeters, 1.0));
// 		receivedIntensity *= max(0.0, dot(toStar, normal.xyz));
// 		// float reflectedIntensity = receivedIntensity / (4 * PI * pow(fragDistanceMeters, 2.0));

// 		return max(0.1 * (color.rgb + 0.1) * receivedIntensity, 0.05 * (color.rgb + 0.1));
// 	}
// 	return vec3(0);
// }

// =============== GAS GIANT SHADING ===============

float fbm(in vec3 pos) {
    float maxValue = 0.0;
    float currentValue = 0.0;
    
    float currentAmplitude = 1.0;
    float currentFrequency = 1.0;
    for (int i = 0; i < 4; ++i) {
        maxValue += currentAmplitude;
        vec3 offset = 10.0 * vec3(rand(float(i)));
        currentValue += currentAmplitude * noiseSimplex(currentFrequency * pos + offset);
        currentAmplitude *= 0.7;
        currentFrequency *= 1.7;
    }
    return currentValue / maxValue;
}

float field2(in vec3 pos) {
    vec3 p = pos;
    p += fbm(0.1 * p);
    p += fbm(0.8 * p);
    return 0.5 + 0.5 * fbm(p);
}

struct BandInfo {
    float pos;
    float positiveEdge;
    float negativeEdge;
};

float closestEdge(in BandInfo info) {
    float pd = distance(info.positiveEdge, info.pos);
    float nd = distance(info.negativeEdge, info.pos);
    return pd < nd ? info.positiveEdge : info.negativeEdge;
}

float centerOf(in BandInfo info) {
    return (info.positiveEdge + info.negativeEdge) / 2.0;
}

float edgeDistance(in BandInfo info) {
    float pd = distance(info.positiveEdge, info.pos);
    float nd = distance(info.negativeEdge, info.pos);
    return pd < nd ? pd : nd;
}

float centerDistance(in BandInfo info) {
    float center = (info.positiveEdge + info.negativeEdge) / 2.0;
    return distance(center, info.pos);
}

BandInfo bandField(in float t) {
    float bc = 1.5;
    
    float p = bc * t;
    float bp = fract(p);
    float fp = floor(p);

    float rc = fp + rand(fp);
    float rm = fp - 1.0 + rand(fp - 1.0);
    float rp = fp + 1.0 + rand(fp + 1.0);
    
    if (p < rc) return BandInfo(p, rc, rm);
    else        return BandInfo(p, rp, rc);
}

float sigmoid(float x) {
    return 1.0 / (1.0 + (exp(-(x - 0.5) * 14.0))); 
}

vec3 flowField(in vec3 pos, in float time, in float seed) {
    BandInfo bi = bandField(pos.y + seed);
    
    vec3 p = pos;
	p.xz /= 6.0;
    float d = 2.0 * rand(bi.negativeEdge) - 1.0;
    d = clamp(d, -0.2, 0.2);
    p.x += d * edgeDistance(bi) * time;
    
    float closest = closestEdge(bi);
    float center = centerOf(bi);
   
    float er = rand(seed + 10.0);
    float eg = rand(seed + 11.0);
    float eb = rand(seed + 12.0);
    vec3 ec = 0.5 * vec3(er, eg, eb);
    float cr = rand(seed + 13.0);
    float cg = rand(seed + 14.0);
    float cb = rand(seed + 15.0);
    vec3 cc = 0.5 * vec3(cr, cg, cb);

    float ar = rand(seed + 16.0);
    float ag = rand(seed + 17.0);
    float ab = rand(seed + 18.0);
    vec3 ac = vec3(ar, ag, ab);

    vec3 baseColor = mix(ec, cc, edgeDistance(bi) / distance(center, bi.negativeEdge));
	vec3 col = mix(baseColor, vec3(0.0), pow(field2(p), 2.0));
    
    return col;
}

vec3 smoothedField(in vec3 pos, in float seed) {
    float t = Time * 0.2;

    vec3 a = flowField(pos, 1.0 + mod(t, 1.0), seed);
    vec3 b = flowField(pos, mod(t, 1.0), seed);
    return mix(a, b, mod(t, 1.0));
}

vec3 warped(in vec3 uv) {
    vec3 p = uv;
    p.y += 0.1 * fbm(1.8 * p);
    return p;
}

vec3 field(in vec3 pos, in float seed) {
    // return smoothedField(warped(pos), seed);
    return smoothedField(pos, seed);
}

// =============== === ===== ======= ===============

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / HALF_PI;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

vec3 gasGiantBaseColor(vec3 pos) {
	return field(pos, RenderingSeed / 1000.0);
}

Light makeStarLight(in vec3 posWorld, in vec4 color) {
	return makePointLight(posWorld, color.rgb * color.a * 3.827e26);
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
	return height;
}

Material shadeElw(vec3 fragPosWorld, vec3 normWorld) {
	float height = sampleHeight(normWorld);

	if (height >= 0.0) {
		float colnoise = noiseSimplex(normWorld + 10.0) * 0.5 + 0.5;
		vec3 col = mix(vec3(0.4, 1.0, 0.35), vec3(0.6, 0.4, 0.4), colnoise);
		// return Material(vec3(0.0), vec3(0.3, 1.0, 0.3), 1.0, 0.0);
		return Material(vec3(0.0), col, 1.0, 0.0);
	}
	return Material(vec3(0.0), vec3(0.1, 0.2, 1.0), 0.1, 0.0);
}
Material shadeGasGiant(vec3 fragPosWorld, vec3 normWorld) {
	vec3 color = gasGiantBaseColor(normWorld);
	return Material(vec3(0.0), color, 1.0, 0.0);
}
Material shadeIceWorld(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}
Material shadeRockyIceWorld(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}
Material shadeRocky(vec3 fragPosWorld, vec3 normWorld) {
	float colnoise = noiseSimplex(normWorld) * 0.5 + 0.5;
	vec3 col = mix(vec3(0.2), vec3(0.8), smoothstep(0.2, 0.3, colnoise));
	return Material(vec3(0.0), col, 1.0, 0.0);
}
Material shadeWater(vec3 fragPosWorld, vec3 normWorld) {
	return Material(vec3(0.0), vec3(1.0, 0.0, 1.0), 1.0, 0.0);
}

Material shadePlanet(vec3 fragPosWorld, vec3 normWorld) {
	if (PlanetType == PLANET_TYPE_EARTH_LIKE_WORLD) return shadeElw(fragPosWorld, normWorld);
	else if (PlanetType == PLANET_TYPE_GAS_GIANT) return shadeGasGiant(fragPosWorld, normWorld);
	else if (PlanetType == PLANET_TYPE_ICE_WORLD) return shadeIceWorld(fragPosWorld, normWorld);
	else if (PlanetType == PLANET_TYPE_ROCKY_ICE_WORLD) return shadeRockyIceWorld(fragPosWorld, normWorld);
	else if (PlanetType == PLANET_TYPE_ROCKY_WORLD) return shadeRocky(fragPosWorld, normWorld);
	else if (PlanetType == PLANET_TYPE_WATER_WORLD) return shadeWater(fragPosWorld, normWorld);
}

void main() {
	vec3 normWorld = (inverse(ModelViewMat) * normalize(normal)).xyz;
	vec3 posWorld = (inverse(ModelViewMat) * normalize(vertexPos)).xyz;

	Material material = shadePlanet(posWorld, normWorld);
	LightingContext ctx = makeLightingContext(material, uMetersPerUnit, uCameraPos, posWorld, normWorld);

	vec3 res = vec3(0.0);
	Light l0 = makeStarLight(LightPos0.xyz, LightColor0);
	res += lightContribution(ctx, l0);
	// res += lightContribution(ctx, makeStarLight(LightPos1.xyz, LightColor1));
	// res += lightContribution(ctx, makeStarLight(LightPos2.xyz, LightColor2));
	// res += lightContribution(ctx, makeStarLight(LightPos3.xyz, LightColor3));
	vec3 finalColor = res;

	float exposure = 5e-26;
	// finalColor = 1.0 - exp(-finalColor * exposure);
	finalColor *= exposure;
	// finalColor = finalColor / (1.0 + finalColor);

	// finalColor = acesTonemap(finalColor);

    fragColor = vec4(finalColor, 1);
}

#endif
