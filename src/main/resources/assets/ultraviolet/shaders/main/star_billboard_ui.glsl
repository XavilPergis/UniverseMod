#stages vertex fragment

VARYING_V2F vec4 vertexColor;

#include [ultraviolet:common_uniforms.glsl]

#ifdef IS_VERTEX_STAGE
in vec3 aPos;
in vec4 aColor;

uniform float uMinDistance;
uniform float uMaxDistance;
uniform float uFadeoutDistance;
uniform float uMinSize;
uniform float uMaxSize;

// #define MIN_DISTANCE (0.0)
// #define MAX_DISTANCE (100000.0)
// #define FADEOUT_DISTANCE (5000000.0)
// #define MIN_SIZE (3.0)
// #define MAX_SIZE (15.0)

// vec3 rgb2hsv(vec3 c) {
//     vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
//     vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
//     vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

//     float d = q.x - min(q.w, q.y);
//     float e = 1.0e-10;
//     return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
// }

// vec3 hsv2rgb(vec3 c) {
//     vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
//     vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
//     return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
// }

void main() {
	vec4 viewPos = uViewMatrix * vec4(aPos, 1.0);
	float distanceFromCameraTm = length(viewPos.xyz) * (uMetersPerUnit / 1e12);

	float alpha = 1.0;

	float t = invLerp(distanceFromCameraTm, uMinDistance, uMaxDistance);
	if (t > 1) {
		float t2 = invLerp(distanceFromCameraTm, uMaxDistance, uFadeoutDistance);
		t2 = clamp(t2, 0.0, 1.0);
		t2 = pow(t2, 0.2);
		alpha *= 1.0 - 0.8 * t2;
	}

	t = clamp(t, 0.0, 1.0);
	t = pow(t, 2.0);

	float size = lerp(t, uMaxSize, uMinSize);

	gl_Position = uProjectionMatrix * viewPos;
	gl_PointSize = size;

	// vec3 hsv = rgb2hsv(alpha * aColor.rgb);
	// hsv.y = min(1.0, hsv.y * 5.0);
	// vec3 col = hsv2rgb(hsv);
	// vertexColor = vec4(col, 1.0);
	vertexColor = vec4(alpha * aColor.rgb, 1);
}

#endif

#ifdef IS_FRAGMENT_STAGE

out vec4 fColor;

void main() {
	if (length(gl_PointCoord - 0.5) > 0.5)
		discard;

	vec3 color = vertexColor.rgb;
    fColor = vec4(color, 1.0);
}

#endif
