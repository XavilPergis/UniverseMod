#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 texCoord0;

void main() {
    gl_Position = vec4(Position, 1.0);
	texCoord0 = Position;
}
