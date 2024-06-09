#stages fragment

#define BILLBOARD_KIND_TOWARDS_CAMERA
#define BILLBOARD_RANDOM_ORIENTATION
#include [ultraviolet:vertex/billboard.glsl]

#ifdef IS_FRAGMENT_STAGE

uniform sampler2D uBillboardTexture;

out vec4 fColor;

void main() {
    vec4 s1 = 0.5 * texture(uBillboardTexture, texCoord0) * vertexColor;
	// vec4 s1 = vec4(vec3(length(texCoord0)), 1.0) * vertexColor;
	// float d = max(0, 1.0 - length(2.0 * texCoord0 - 1.0));
	// d = pow(d, 4.0);
	// vec4 s1 = vec4(vec3(d), 1.0) * vertexColor;
	// vec4 s1 = vertexColor;
    // fColor = vec4(s1.a * vertexColor.rgb * vertexColor.a, 1.0);
	s1.a *= 0.1;
    fColor = s1;
}

#endif
