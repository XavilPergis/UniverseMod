#stages fragment

#define BILLBOARD_KIND_VIEW_ALIGNED
#include [ultraviolet:vertex/billboard.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uBillboardTexture;

uniform vec4 ColorModulator;
uniform float MetersPerUnit;

out vec4 fragColor;

void main() {
    vec4 s1 = texture(uBillboardTexture, texCoord0) * vertexColor;
    fragColor = vec4(s1.a * vertexColor.rgb * vertexColor.a, 1.0);
}

#endif
