#stages vertex fragment

VARYING_V2F vec3 texCoord0;

#ifdef IS_VERTEX_STAGE

in vec3 aPos;

void main() {
    gl_Position = vec4(aPos, 1.0);
	texCoord0 = aPos;
}

#elif defined(IS_FRAGMENT_STAGE)

uniform samplerCube SkyboxSampler;



struct Foo {
	vec4 a[3];
	vec4 b[3];
};

uniform Foo foos[5];

out vec4 fColor;

void main() {
	fColor = vec4(0.0);
	fColor += foos[1].a[2];
	fColor += foos[4].b[2];
	fColor += texture(SkyboxSampler, texCoord0);
}

#endif
