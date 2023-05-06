#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexPos;
out vec4 vertexColor;
out vec2 texCoord0;

vec3 offsetDirView() {
	int id = gl_VertexID % 4;
	if (id == 0) return vec3(-1.0, -1.0, 0.0);
	if (id == 1) return vec3(-1.0,  1.0, 0.0);
	if (id == 2) return vec3( 1.0,  1.0, 0.0);
	else         return vec3( 1.0, -1.0, 0.0);
}

vec2 uv() {
	int id = gl_VertexID % 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(0.0, 1.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(1.0, 0.0);
}

void main() {
    // gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	// vertexPos = ModelViewMat * vec4(Position, 1.0);

	vec4 posView = ModelViewMat * vec4(Position, 1.0);
	// posView.xyz += 1000.0 * offsetDirView();
	posView.xyz += UV0.x * offsetDirView();
	vec4 posClip = ProjMat * posView;
	vertexPos = posView;

    gl_Position = posClip;

    vertexColor = Color;
    // texCoord0 = UV0;
	texCoord0 = uv();
}
