// tonemapping, color grading, etc. stuff that can be combined into a single shader pass.

#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/tonemap.glsl]

uniform float uExposure;
uniform float uAverageLuminance;

uniform sampler2D uSampler;

out vec4 fColor;

const float GAMMA = 2.2;

vec3 tonemapReinhard(vec3 C, float Cw) {
	return C * (1.0 + C / (Cw * Cw)) / (1.0 + C);
}

void main() {
	// raw HDR
    vec4 res = texture(uSampler, texCoord0);

	// exposure compensation
	// float luminance = dot(res.rgb, vec3(0.2125, 0.7154, 0.0721));
	// float lumRatio = luminance / uAverageLuminance;
	// res.rgb *= min(1e6, lumRatio);
	// res.rgb *= min(1.0, 1.0 / (1.0 * uAverageLuminance));
	// res.rgb *= min(100.0, 1.0 / (1.0 * uAverageLuminance));
	// res.rgb *= uExposure;

	// tonemapping
	// res.rgb = tonemapACESFull(res.rgb);

	res.rgb = tonemapReinhard(res.rgb, uAverageLuminance);

	// gamma correction
	res.rgb = pow(res.rgb, vec3(1.0 / GAMMA));

    fColor = res;
}

#endif
