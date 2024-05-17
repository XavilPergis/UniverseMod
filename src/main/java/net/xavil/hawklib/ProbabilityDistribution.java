package net.xavil.hawklib;

import java.util.function.DoubleUnaryOperator;

import javax.annotation.Nonnull;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.Interval;

public interface ProbabilityDistribution {

	/**
	 * Picks a value according to this distribution.
	 * 
	 * @param t A value uniformly distributed in [0,1]
	 * @return The mapped t-value.
	 */
	double pick(double t);

	static ProbabilityDistribution UNIFORM_UNIPOLAR = t -> t;
	static ProbabilityDistribution UNIFORM_BIPOLAR = t -> 2.0 * t - 1.0;

	static ProbabilityDistribution uniform(double min, double max) {
		return new Uniform(min, max);
	}

	static ProbabilityDistribution uniform(Interval interval) {
		return new Uniform(interval.min, interval.max);
	}

	@Nonnull
	static ProbabilityDistribution interpolate(DoubleUnaryOperator probabilityFunction,
			Interval probabilityFunctionDomain, int binCount) {
		return new InterpolatedTable(probabilityFunction, probabilityFunctionDomain, binCount);
	}

	final class Uniform implements ProbabilityDistribution {
		private final double min, max;

		public Uniform(double min, double max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public double pick(double t) {
			return Mth.lerp(t, this.min, this.max);
		}
	}

	// adapt an arbitrary pdf into a distribution that can be sampled from in
	// constant time.
	final class InterpolatedTable implements ProbabilityDistribution {

		private final double[] inverseCdf;
		private final double invCdfMin, invCdfMax;

		private static final class LineSegment {
			public double iMin, iMax;
			public double oMin, oMax;

			public LineSegment(double iMin, double oMin, double iMax, double oMax) {
				this.iMin = iMin;
				this.iMax = iMax;
				this.oMin = oMin;
				this.oMax = oMax;
			}

			public void invert() {
				final var minTmp = this.iMin;
				this.iMin = this.oMin;
				this.oMin = minTmp;
				final var maxTmp = this.iMax;
				this.iMax = this.oMax;
				this.oMax = maxTmp;
			}

			public double interpolate(double input) {
				final var t = Mth.inverseLerp(input, this.iMin, this.iMax);
				return Mth.lerp(t, this.oMin, this.oMax);
			}

			public boolean contains(double input) {
				return this.iMin <= input && input <= this.iMax;
			}
		}

		public InterpolatedTable(DoubleUnaryOperator probabilityFunction, Interval domain, int binCount) {
			// im not sure how good the accuracy of this process of building the inverse cdf
			// is, since we do a sort of double linear interpolation. Both of these steps
			// are presumably somewhat lossy. This could probably be improved by using
			// higher-order interpolation, but i dont know how to do that :p

			final var segments = new LineSegment[binCount];
			this.inverseCdf = new double[binCount];

			final var iStep = (domain.max - domain.min) / binCount;
			double currentI = domain.min, currentO = 0;
			double currentPf = probabilityFunction.applyAsDouble(currentI);
			for (int i = 0; i < binCount; ++i) {
				final var iMin = currentI;
				final var iMax = iMin + iStep;
				currentI = iMax;

				final var nextPf = probabilityFunction.applyAsDouble(iMax);
				// since we normalize the cdf afterwards, and the delta between iMin and iMax is
				// constant for this entire loop, it factors out and cancels with itself when
				// dividing.
				final var pfAverage = 0.5 * (currentPf + nextPf);
				currentPf = nextPf;

				final var oMin = currentO;
				final var oMax = oMin + pfAverage;
				currentO = oMax;

				segments[i] = new LineSegment(iMin, oMin, iMax, oMax);
			}

			final var normFactor = 1.0 / segments[segments.length - 1].oMax;
			for (final var segment : segments) {
				// the probability function (and therefore the cdf) is probably not normalized,
				// so we have to do it here. This remaps everything so that the min output of
				// the first segment is 0 and the max of the last segment is 1.
				segment.oMin *= normFactor;
				segment.oMax *= normFactor;
				// invert the segment, since we really want to build a table of the *inverse* of
				// the cdf.
				segment.invert();
			}

			final var tStep = 1.0 / (binCount + 1.0);
			int currentSegmentIndex = 0;
			double currentT = 0;
			for (int i = 0; i < binCount; ++i) {
				final var samplePos = currentT + 0.5 * tStep;
				currentT += tStep;

				// find the segment we're in. since `samplePos` is monotonic, as well as the
				// domains of the segments in `segments`, we know that if `samplePos` is not
				// within the current segment, it *will* be within a later segment.
				while (!segments[currentSegmentIndex].contains(samplePos))
					currentSegmentIndex += 1;
				this.inverseCdf[i] = segments[currentSegmentIndex].interpolate(samplePos);
			}

			// cdf inverse, so domain becomes range
			this.invCdfMin = domain.min;
			this.invCdfMax = domain.max;
		}

		// constant time evaluation <3
		@Override
		public double pick(double t) {
			// just to be sure...
			t = Mth.clamp(t, 0, 1);
			final var fractionalIndex = t * this.inverseCdf.length;
			// the inverse cdf is constructed out of N bins spaced evenly over the domain
			// [0,1]. each element in `inverseCdf` is the centerpoint of that bin.
			if (fractionalIndex < 0.5) {
				final var t2 = 2 * fractionalIndex;
				final var endpointBin = this.inverseCdf[0];
				return Mth.lerp(t2, this.invCdfMin, endpointBin);
			} else if (fractionalIndex > this.inverseCdf.length - 0.5) {
				final var t2 = 2 * (fractionalIndex - this.inverseCdf.length) + 1;
				final var endpointBin = this.inverseCdf[this.inverseCdf.length - 1];
				return Mth.lerp(t2, endpointBin, this.invCdfMax);
			}
			// shift by 0.5 so that we still index the lower bin when fractionalIndex is in
			// [N,N+0.5) for some integer N.
			final var index = Mth.floor(fractionalIndex - 0.5);
			final var fract = fractionalIndex - 0.5 - index;
			return Mth.lerp(fract, this.inverseCdf[index], this.inverseCdf[index + 1]);
		}

	}

}
