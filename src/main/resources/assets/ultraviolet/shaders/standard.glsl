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

#endif