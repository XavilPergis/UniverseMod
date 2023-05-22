#stages vertex fragment

VARYING_V2F vec3 texCoord0;

#ifdef IS_VERTEX_STAGE

in vec3 Position;

void main() {
    gl_Position = vec4(Position, 1.0);
	texCoord0 = Position;
}

#elif defined(IS_FRAGMENT_STAGE)

uniform samplerCube SkyboxSampler;

out vec4 fragColor;

void main() {
	fragColor = texture(SkyboxSampler, texCoord0);
}

#endif
