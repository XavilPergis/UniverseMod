#ifndef ULTRAVIOLET_SAMPLING_H_
#define ULTRAVIOLET_SAMPLING_H_

vec4 upsampleFilter9Tap(sampler2D tex, vec2 uv, ivec2 srcSize, ivec2 dstSize) {
	vec2 texelSizeDst = 1.0 / vec2(dstSize);
	vec2 texelSizeSrc = 1.0 / vec2(srcSize);

	vec2 centerSrcTexel = 0.5 * vec2(dstSize) * uv;
	vec2 centerSrc = texelSizeSrc * centerSrcTexel;

	// vec2 centerSrc = 2.0 * uv * vec2(dstSize) / vec2(srcSize);

	// vec2 d = 2.0 * texelSizeSrc;
	vec2 d = texelSizeSrc;

	vec4 N = vec4(0.0);
	N += texture2D(tex, centerSrc + vec2(-d.x, -d.y)) * 1.0;
	N += texture2D(tex, centerSrc + vec2(-d.x,  0.0)) * 2.0;
	N += texture2D(tex, centerSrc + vec2(-d.x,  d.y)) * 1.0;
	N += texture2D(tex, centerSrc + vec2( 0.0, -d.y)) * 2.0;
	N += texture2D(tex, centerSrc + vec2( 0.0,  0.0)) * 4.0;
	N += texture2D(tex, centerSrc + vec2( 0.0,  d.y)) * 2.0;
	N += texture2D(tex, centerSrc + vec2( d.x, -d.y)) * 1.0;
	N += texture2D(tex, centerSrc + vec2( d.x,  0.0)) * 2.0;
	N += texture2D(tex, centerSrc + vec2( d.x,  d.y)) * 1.0;
	N /= 16.0;

	return N;
	// return texture2D(tex, uv);
}

// Samples occur as follows. Group A falls in the middle of each destination texel in the
// neighborhood around the current one. Group B falls on the dual of this grid, on the border of
// each adjacent destination texel.
//
// Hybrid between a tent filter and a box filter.
//
// A   A   A
//   B   B  
// A  [A]  A
//   B   B  
// A   A   A
vec4 downsampleFilter13Tap(sampler2D tex, vec2 uv, vec2 texelSizeSrc, vec2 dstSizePerSrcSize) {
	vec2 centerSrc = 2.0 * uv * dstSizePerSrcSize;

	vec2 oa = 2.0 * texelSizeSrc;
	vec2 ob = texelSizeSrc;

	vec4 A = vec4(0.0);
	A += texture2D(tex, centerSrc + vec2(-oa.x, -oa.y)) * 1.0;
	A += texture2D(tex, centerSrc + vec2(-oa.x,   0.0)) * 2.0;
	A += texture2D(tex, centerSrc + vec2(-oa.x,  oa.y)) * 1.0;
	A += texture2D(tex, centerSrc + vec2(  0.0, -oa.y)) * 2.0;
	A += texture2D(tex, centerSrc + vec2(  0.0,   0.0)) * 4.0;
	A += texture2D(tex, centerSrc + vec2(  0.0,  oa.y)) * 2.0;
	A += texture2D(tex, centerSrc + vec2( oa.x, -oa.y)) * 1.0;
	A += texture2D(tex, centerSrc + vec2( oa.x,   0.0)) * 2.0;
	A += texture2D(tex, centerSrc + vec2( oa.x,  oa.y)) * 1.0;
	A /= 2.1 * 16.0;

	vec4 B = vec4(0.0);
	B += texture2D(tex, centerSrc + vec2(-ob.x, -ob.y));
	B += texture2D(tex, centerSrc + vec2(-ob.x,  ob.y));
	B += texture2D(tex, centerSrc + vec2( ob.x, -ob.y));
	B += texture2D(tex, centerSrc + vec2( ob.x,  ob.y));
	B /= 2.1 * 4.0;

	return A + B;
}

vec4 downsampleFilter13Tap(sampler2D tex, vec2 uv, ivec2 srcSize, ivec2 dstSize) {
	vec2 texelSizeSrc = 1.0 / vec2(srcSize);
	return downsampleFilter13Tap(tex, uv, texelSizeSrc, texelSizeSrc * vec2(dstSize));
}

vec4 downsampleFilter4Tap(sampler2D tex, vec2 uv, vec2 texelSizeSrc, vec2 dstSizePerSrcSize) {
	vec2 centerSrc = 2.0 * uv * dstSizePerSrcSize;
	vec2 d = texelSizeSrc;

	vec4 N = vec4(0.0);
	N += texture2D(tex, centerSrc + vec2(-d.x, -d.y));
	N += texture2D(tex, centerSrc + vec2(-d.x,  d.y));
	N += texture2D(tex, centerSrc + vec2( d.x, -d.y));
	N += texture2D(tex, centerSrc + vec2( d.x,  d.y));
	N /= 4.0;

	return N;
}

vec4 downsampleFilter4Tap(sampler2D tex, vec2 uv, ivec2 srcSize, ivec2 dstSize) {
	vec2 texelSizeSrc = 1.0 / vec2(srcSize);
	return downsampleFilter4Tap(tex, uv, texelSizeSrc, texelSizeSrc * vec2(dstSize));
}

#endif
