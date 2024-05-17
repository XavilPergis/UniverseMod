#ifndef ULTRAVIOLET_STANDARD_H_
#define ULTRAVIOLET_STANDARD_H_

#if defined(IS_VERTEX_STAGE)
#define VARYING_V2F out
#elif defined(IS_FRAGMENT_STAGE)
#define VARYING_V2F in
#endif

#define PI      (3.1415926535897932384626433832795)
#define HALF_PI (PI / 2.0)
#define TAU     (PI * 2.0)

float saturate(in float n) { return clamp(n, 0.0, 1.0); }
vec2  saturate(in  vec2 n) { return clamp(n, 0.0, 1.0); }
vec3  saturate(in  vec3 n) { return clamp(n, 0.0, 1.0); }
vec4  saturate(in  vec4 n) { return clamp(n, 0.0, 1.0); }

float lerp(float t, float oMin, float oMax) { return mix(oMin, oMax, t); }
vec2  lerp(float t,  vec2 oMin,  vec2 oMax) { return mix(oMin, oMax, t); }
vec3  lerp(float t,  vec3 oMin,  vec3 oMax) { return mix(oMin, oMax, t); }
vec4  lerp(float t,  vec4 oMin,  vec4 oMax) { return mix(oMin, oMax, t); }
vec2  lerp( vec2 t,  vec2 oMin,  vec2 oMax) { return vec2(lerp(t.x, oMin.x, oMax.x), lerp(t.y, oMin.y, oMax.y)); }
vec3  lerp( vec3 t,  vec3 oMin,  vec3 oMax) { return vec3(lerp(t.x, oMin.x, oMax.x), lerp(t.y, oMin.y, oMax.y), lerp(t.z, oMin.z, oMax.z)); }
vec4  lerp( vec4 t,  vec4 oMin,  vec4 oMax) { return vec4(lerp(t.x, oMin.x, oMax.x), lerp(t.y, oMin.y, oMax.y), lerp(t.z, oMin.z, oMax.z), lerp(t.w, oMin.w, oMax.w)); }
vec2  lerp( vec2 t, float oMin, float oMax) { return lerp(t, vec2(oMin), vec2(oMax)); }
vec3  lerp( vec3 t, float oMin, float oMax) { return lerp(t, vec3(oMin), vec3(oMax)); }
vec4  lerp( vec4 t, float oMin, float oMax) { return lerp(t, vec4(oMin), vec4(oMax)); }

float invLerp(float n, float iMin, float iMax) {
	return (n - iMin) / (iMax - iMin);
}

float remap(float n, float iMin, float iMax, float oMin, float oMax) {
	float t = invLerp(n, iMin, iMax);
	return lerp(t, oMin, oMax);
}

#endif