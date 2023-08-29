#stages vertex

VARYING_V2F vec2 texCoord0;

#ifdef IS_VERTEX_STAGE

in vec3 aPos;
in vec2 aTexCoord0;

void main() {
    gl_Position = vec4(aPos, 1.0);
	texCoord0 = aTexCoord0;
}

#endif