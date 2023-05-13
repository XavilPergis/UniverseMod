#ifndef ULTRAVIOLET_LOGDEPTH_H_
#define ULTRAVIOLET_LOGDEPTH_H_

#moj_import <ultraviolet_common_uniforms.glsl>

void ApplyLogDepth(inout float depth) {
	float w = gl_Position[3];
	float C = 0.001;
	// 2 / log2(far + 1)
	depth *= log(depth + 1.0) * 2.0 / log(far + 1.0) - 1.0;
}

#endif