// tonemapping, color grading, etc. stuff that can be combined into a single shader pass.

#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:common_uniforms.glsl]

uniform sampler2D uColorSampler;
uniform sampler2D uDepthSampler;

out vec4 fColor;

void main() {
    vec3 light = texture(uColorSampler, texCoord0).rgb;
	fColor = vec4(light, 1.0);
}

#endif
