#stages fragment

// #define BILLBOARD_KIND_TOWARDS_CAMERA
#define BILLBOARD_KIND_VIEW_ALIGNED
#include [ultraviolet:vertex/billboard.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:lib/util.glsl]

uniform sampler2D uBillboardTexture;

out vec4 fColor;

void main() {
    vec4 s1 = texture(uBillboardTexture, texCoord0);
	s1 *= vertexColor;
    vec4 s2 = texture(uBillboardTexture, saturate((texCoord0 - 0.5) / 0.5 + 0.5));

	vec4 hdrColor = vec4(0.0);
	hdrColor = (1.0 * hdrColor) + (1.0 * s1.a * s1);
	hdrColor = (1.0 * hdrColor) + (1.0 * s2.a * s2);

	float alpha = max(0.0, pow(vertexColor.a, 1.0));
    fColor = 0.9 * hdrColor * alpha;
}

#endif
