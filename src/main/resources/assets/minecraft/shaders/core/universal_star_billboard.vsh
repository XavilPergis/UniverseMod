#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// uniform vec3 CameraPos;
// uniform vec3 CaptureOrigin;

// out vec4 centerPosView;
// out vec4 vertexPosView;
out vec4 vertexColor;
out vec2 texCoord0;

out float vertexId;

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
	vec4 posView = ModelViewMat * vec4(Position, 1.0);

	vec3 facing = normalize(posView.xyz);
	vec3 up = normalize(cross(facing, vec3(1.0, 0.0, 0.0)));
	vec3 right = normalize(cross(facing, up));
	vec3 off = offsetDirView();
	posView.xyz += UV0.x * (off.x * right + off.y * up);

	// posView.xyz += UV0.x * offsetDirView();
	vec4 posClip = ProjMat * posView;

    gl_Position = posClip;

    texCoord0 = uv();
    vertexColor = Color;
}
