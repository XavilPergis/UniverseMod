#stages fragment

#include [ultraviolet:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uSampler;

out vec4 fragColor;

void main() {
	fragColor = texture2D(uSampler, texCoord0);
}

#endif
