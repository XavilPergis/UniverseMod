#stages fragment

#define BILLBOARD_KIND_TOWARDS_CAMERA
#define BILLBOARD_RANDOM_ORIENTATION
#include [ultraviolet:vertex/billboard.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uBillboardTexture;

out vec4 fColor;

void main() {
    vec4 s1 = 0.5 * texture(uBillboardTexture, texCoord0) * vertexColor;
    fColor = vec4(s1.a * vertexColor.rgb * vertexColor.a, 1.0);
}

#endif
