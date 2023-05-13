#version 150

in vec3 Position;
in vec2 UV0;

// out VertexOutput {
// 	vec2 texCoord0;
// } output;

out vec2 texCoord0;

void main() {
    gl_Position = vec4(Position, 1.0);
    // gl_Position = vec4(UV0, 0.0, 1.0);
	// output.texCoord0 = UV0;
	texCoord0 = UV0;
}
