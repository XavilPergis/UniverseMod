#stages fragment

// vanilla outputs everything already gamma-encoded, and i dont want to change literally every single vanilla shader...
// so instead, we revert back to linear rgb before compositing back onto the sky buffer so we can apply post-processing to everything.

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uSampler;

out vec4 fColor;

void main() {
    vec4 colorSrgb = texture(uSampler, texCoord0);
	if (colorSrgb.a >= 0.5) colorSrgb.a = 1.0;
    fColor = vec4(pow(colorSrgb.rgb, vec3(2.0)), colorSrgb.a);
}

#endif
