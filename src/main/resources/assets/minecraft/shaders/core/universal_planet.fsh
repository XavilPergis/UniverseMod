#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

uniform mat4 ModelViewMat;

uniform vec4 LightPos0;
uniform vec4 LightColor0;
uniform vec4 LightPos1;
uniform vec4 LightColor1;
uniform vec4 LightPos2;
uniform vec4 LightColor2;
uniform vec4 LightPos3;
uniform vec4 LightColor3;

in vec2 texCoord0;
in vec4 vertexPos;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

vec3 contribution(vec4 color, vec4 pos) {
	if (color.a >= 0) {
		vec3 p0 = (ModelViewMat * vec4(pos.xyz, 1.0)).xyz;
		// vec3 p0 = (ModelViewMat * vec4(0, 10, 0, 1.0)).xyz;
		// vec3 p0 = vec3(0);
		vec3 d0 = normalize(p0 - vertexPos.xyz);
		return color.rgb * dot(normal.xyz, d0);
	}
	return vec3(0);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }

	vec3 res = vec3(0);

	res += contribution(LightColor0, LightPos0);
	// res += contribution(LightColor1, LightPos1);
	// res += contribution(LightColor2, LightPos2);
	// res += contribution(LightColor3, LightPos3);

    fragColor = vec4(res * color.rgb, 1);
}
