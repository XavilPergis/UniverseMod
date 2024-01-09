#stages vertex fragment

VARYING_V2F vec2 texCoord0;
VARYING_V2F vec3 vertexPos;
VARYING_V2F vec4 vertexColor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;
in vec2 aTexCoord0;

vec2 uvFromVertex(int id) {
	id = id % 4;
	if (id == 0) return vec2(0.0, 0.0);
	if (id == 1) return vec2(1.0, 0.0);
	if (id == 2) return vec2(1.0, 1.0);
	else         return vec2(0.0, 1.0);
}

void emitPoint(vec4 viewPos, float pointSize) {
	vec2 uv = uvFromVertex(gl_VertexID);
	vec2 off = vec2(2.0 * uv - 1.0);

	gl_Position = uProjectionMatrix * viewPos;
	gl_Position /= gl_Position.w;
	gl_Position.xy += pointSize * off / uScreenSize;

	texCoord0 = uv;
	vertexPos = viewPos.xyz;
}

#define MIN_DISTANCE (0.0)
#define MAX_DISTANCE (100000.0)
#define FADEOUT_DISTANCE (5000000.0)
#define MIN_SIZE (3.0)
#define MAX_SIZE (15.0)

void main() {
	vec4 viewPos = uViewMatrix * vec4(aPos, 1.0);
	float distanceFromCameraTm = length(viewPos.xyz) * (uMetersPerUnit / 1e12);

	float alpha = 1.0;

	float t = invLerp(distanceFromCameraTm, MIN_DISTANCE, MAX_DISTANCE);
	if (t > 1) {
		float t2 = invLerp(distanceFromCameraTm, MAX_DISTANCE, FADEOUT_DISTANCE);
		t2 = clamp(t2, 0.0, 1.0);
		t2 = pow(t2, 0.2);
		alpha *= 1.0 - t2;
	}

	t = clamp(t, 0.0, 1.0);
	t = pow(t, 2.0);

	float size = lerp(t, MAX_SIZE, MIN_SIZE);

	emitPoint(viewPos, size);
	vertexColor = vec4(alpha * aColor.rgb, 1);
}

#endif

#ifdef IS_FRAGMENT_STAGE

out vec4 fColor;

void main() {
	if (length(texCoord0 - 0.5) > 0.5)
		discard;

	vec3 color = vertexColor.rgb;
    fColor = vec4(color, 1.0);
}

#endif
