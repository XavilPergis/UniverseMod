#stages fragment

#define BILLBOARD_KIND_TOWARDS_CAMERA
// #define BILLBOARD_KIND_VIEW_ALIGNED
#include [ultraviolet:vertex/billboard.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uBillboardTexture;

out vec4 fColor;

void main() {
    vec4 s1 = texture(uBillboardTexture, texCoord0) * vertexColor;
    fColor = vec4(s1.a * vertexColor.rgb * vertexColor.a, 1.0);
}

#endif
