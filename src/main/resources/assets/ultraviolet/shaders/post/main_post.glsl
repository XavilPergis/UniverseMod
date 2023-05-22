// tonemapping, color grading, etc. stuff that can be combined into a single shader pass.

#stages fragment

#include [ultraviolet:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/tonemap.glsl]

const float gamma = 2.2;

uniform float uExposure;

uniform sampler2D uSampler;

out vec4 fragColor;

void main() {
    vec3 light = texture(uSampler, texCoord0).rgb;

	light *= uExposure;

	light = tonemapACESFull(light);

	vec3 gammaEncoded = pow(light, vec3(1.0 / gamma));
    fragColor = vec4(gammaEncoded, 1.0);
}

#endif
