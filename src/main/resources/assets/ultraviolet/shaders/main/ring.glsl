#stages vertex fragment

#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE

#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]

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

out vec4 fColor;

vec3 contribution(vec4 color, vec4 pos) {
	if (color.a >= 0) {
		vec3 p0 = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;

		float distanceMeters = distance(vertexPos.xyz, p0) * MetersPerUnit;
		float d2 = 1.0 / pow(distanceMeters / 1e14, 2.0);
		float light = min(10.0, color.a * d2);

		// NOTE: rings are made of a bunch of dust and debris and stuff, and that stuff can
		// reflect light back even at grazing angles, so we don't do a dot product here.
		// TODO: its more complicated than this
		return color.rgb * light;
	}
	return vec3(0);
}

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

void main() {
	float gamma = 2.2;
    // vec4 baseColor = texture(Sampler0, texCoord0) * vertexColor;
	// baseColor.rgb = pow(baseColor.rgb, vec3(gamma));

    // if (baseColor.a < 0.1) {
    //     discard;
    // }

	vec3 res = vec3(0);

	res += contribution(LightColor0, LightPos0);
	res += contribution(LightColor1, LightPos1);
	res += contribution(LightColor2, LightPos2);
	res += contribution(LightColor3, LightPos3);

	vec4 baseColor = shadeRing(texCoord0.y);
	vec3 finalColor = res * baseColor.rgb;

	float exposure = 0.1;
	// finalColor = 1.0 - exp(-finalColor * exposure);
	finalColor *= exposure;

    // finalColor = pow(finalColor, vec3(1.0 / gamma));
	// finalColor = acesTonemap(finalColor);

    fColor = vec4(finalColor, baseColor.a);
}

#endif
