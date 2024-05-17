#stages compute

#define HISTOGRAM_SIZE (256)

uniform float uMinLogLuminance;
uniform float uMaxLogLuminance;

layout (r16f, binding = 1) uniform image2D uSceneLuminance;
layout (std430) writeonly buffer bHistogram { uint oHistogramBins[]; };

shared uint sharedBins[HISTOGRAM_SIZE];

uint computeBin(float luminance) {
	// too small of a luminance value would cause the log2 luminance to shoot off to -inf, so we just put those values in the first bin
	// if (luminance < exp2(uMinLogLuminance))
	// 	return 0;
	if (luminance < 1e-6)
		return 0;
	float histogramT = saturate(invLerp(log2(luminance), uMinLogLuminance, uMaxLogLuminance));
	// bin 0 is already used by out-of-range values
	// return uint(histogramT * float(HISTOGRAM_SIZE - 1)) + 1;
	return uint(histogramT * 254.0 + 1.0);
}

layout (local_size_x = 16, local_size_y = 16, local_size_z = 1) in;
void main() {
	sharedBins[gl_LocalInvocationIndex] = 0;

	// groupMemoryBarrier();
	barrier();

	uvec2 size = imageSize(uSceneLuminance);
	if (gl_GlobalInvocationID.x < size.x && gl_GlobalInvocationID.y < size.y) {
		float luminance = imageLoad(uSceneLuminance, ivec2(gl_GlobalInvocationID.xy)).r;
		uint bin = computeBin(luminance);
		atomicAdd(sharedBins[bin], 1);
	}

	// groupMemoryBarrier();
	barrier();

	atomicAdd(oHistogramBins[gl_LocalInvocationIndex], sharedBins[gl_LocalInvocationIndex]);
	// atomicAdd(oHistogramBins[gl_LocalInvocationIndex], 1);
}
