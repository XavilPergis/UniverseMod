#version 150

#moj_import <universal_noise.glsl>
#moj_import <universal_util.glsl>
#moj_import <universal_misc.glsl>

uniform sampler2D Sampler0;

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

uniform int IsGasGiant;
uniform float RenderingSeed;
uniform float Time;

in vec2 texCoord0;
in vec4 vertexPos;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

vec3 contribution(vec4 color, vec4 pos) {
	if (color.a >= 0) {
		vec3 starPos = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;
		vec3 toStar = normalize(starPos - vertexPos.xyz);
		float starDistanceMeters = distance(vertexPos.xyz, starPos) * MetersPerUnit;
		float fragDistanceMeters = length(vertexPos.xyz) * MetersPerUnit;

		float receivedIntensity = (color.a * 3.827e26) / (4 * PI * pow(starDistanceMeters, 2.0));
		receivedIntensity *= max(0.0, dot(toStar, normal.xyz));
		// float reflectedIntensity = receivedIntensity / (4 * PI * pow(fragDistanceMeters, 2.0));

		return 50.0 * color.rgb * receivedIntensity;
	}
	return vec3(0);
}

// =============== GAS GIANT SHADING ===============

float fbm(in vec3 pos) {
    float maxValue = 0.0;
    float currentValue = 0.0;
    
    float currentAmplitude = 1.0;
    float currentFrequency = 1.0;
    for (int i = 0; i < 8; ++i) {
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
    float t = Time * 0.001;

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
    return smoothedField(warped(pos), seed);
}

// =============== === ===== ======= ===============

vec2 uvFromNormal(vec4 norm) {
	vec3 normCam = (inverse(ModelViewMat) * norm).xyz;
	float pole = normCam.y;
	float equator = atan(normCam.z / normCam.x) / PI_2;
	equator = normCam.x >= 0 ? equator * 0.5 - 0.5 : equator * 0.5 + 0.5;
	return vec2(equator, pole);
}

vec3 gasGiantBaseColor(vec3 pos) {
	return field(pos, RenderingSeed / 1000.0);
}

void main() {
	vec4 baseColor = vec4(0);
	if (IsGasGiant != 0) {
		vec3 norm = (inverse(ModelViewMat) * normalize(normal)).xyz;
		// baseColor = vec4(gasGiantBaseColor(uvFromNormal(normalize(normal))), 1.0);
		baseColor = vec4(gasGiantBaseColor(norm), 1.0);
	} else {
    	baseColor = texture(Sampler0, texCoord0);
	}
	baseColor *= vertexColor;

	vec3 res = vec3(0);
	res += contribution(LightColor0, LightPos0);
	res += contribution(LightColor1, LightPos1);
	res += contribution(LightColor2, LightPos2);
	res += contribution(LightColor3, LightPos3);
	vec3 finalColor = res * baseColor.rgb;

	// finalColor = acesTonemap(finalColor);

    fragColor = vec4(finalColor, 1);
}
