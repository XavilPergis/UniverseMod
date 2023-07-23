#ifndef ULTRAVIOLET_LOGDEPTH_H_
#define ULTRAVIOLET_LOGDEPTH_H_

float logDepthCoefficient(const float zFar) {
	return 2.0 / log2(zFar + 1.0);
}

// https://io7m.github.io/r2/documentation/p2s24.xhtml
float encodeLogDepth(float z, const float coefficient) {
  z = max(0.000001, z + 1.0);
  return log2(z) * coefficient * 0.5;
}

float decodeLogDepth(const float z, const float coefficient) {
  float exponent = z / (coefficient * 0.5);
  return pow(2.0, exponent) - 1.0;
}

#endif
