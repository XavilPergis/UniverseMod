// tonemapping, color grading, etc. stuff that can be combined into a single shader pass.

#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/tonemap.glsl]

uniform float uExposure;

uniform sampler2D uSampler;

out vec4 fColor;

const float GAMMA = 2.2;

void main() {
	// raw HDR
    vec4 res = texture(uSampler, texCoord0);

	// exposure compensation
	res.rgb *= uExposure;

	// tonemapping
	res.rgb = tonemapACESFull(res.rgb);

	// gamma correction
	res.rgb = pow(res.rgb, vec3(1.0 / GAMMA));

    fColor = res;
}

#endif
