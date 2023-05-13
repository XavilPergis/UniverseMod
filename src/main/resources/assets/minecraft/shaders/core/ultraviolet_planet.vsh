#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
// in vec2 UV1;
in vec4 Color;
in vec3 Normal;

// uniform mat4 ModelMat;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;
out vec4 vertexPos;
out vec4 normal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	vertexPos = ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    vertexColor = Color;
    normal = ModelViewMat * vec4(Normal, 0.0);
}
