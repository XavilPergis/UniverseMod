#stages vertex

VARYING_V2F vec2 texCoord0;

#ifdef IS_VERTEX_STAGE

in vec3 Position;
in vec2 UV0;

void main() {
    gl_Position = vec4(Position, 1.0);
	texCoord0 = UV0;
}

#endif