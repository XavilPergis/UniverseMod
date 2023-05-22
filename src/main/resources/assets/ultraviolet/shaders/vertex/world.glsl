#stages vertex

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec4 vertexColor;
VARYING_V2F vec4 vertexPos;
VARYING_V2F vec4 normal;

#ifdef IS_VERTEX_STAGE

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	vertexPos = ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    normal = ModelViewMat * vec4(Normal, 0.0);
}

#endif