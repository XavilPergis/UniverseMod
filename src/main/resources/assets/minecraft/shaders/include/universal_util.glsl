
#define PI (3.1415926535897932384626433832795)
#define PI_2 (PI / 2.0)
#define TAU (PI * 2.0)

float saturate(float n) { return clamp(n, 0.0, 1.0); }
vec2  saturate(vec2 n)  { return vec2(saturate(n.x), saturate(n.y)); }
vec3  saturate(vec3 n)  { return vec3(saturate(n.x), saturate(n.y), saturate(n.z)); }
vec4  saturate(vec4 n)  { return vec4(saturate(n.x), saturate(n.y), saturate(n.z), saturate(n.w)); }
