#version 150

#moj_import <universal_noise.glsl>
#moj_import <universal_util.glsl>
#moj_import <universal_misc.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float MetersPerUnit;

in vec4 vertexPos;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 s1 = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = vec4(s1.a * vertexColor.rgb * vertexColor.a, 1.0);
    // fragColor = vec4(vertexColor.rgb * vertexColor.a * 0.2, 1.0);
}
