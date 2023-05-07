#version 150

#moj_import <fog.glsl>
#moj_import <universal_common_uniforms.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out VertexOut {
	vec2 uv0;
	vec4 color;
	vec4 posView;
	vec4 normalView;
} output;

void main() {
	vec4 posView = uViewMatrix * vec4(Position, 1.0);
	output.posView = posView;
    gl_Position = uProjectionMatrix * posView;
    output.uv0 = UV0;
    output.color = Color;
    output.normalView = uViewMatrix * vec4(Normal, 0.0);
}
