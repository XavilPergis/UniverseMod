package net.xavil.ultraviolet.client.screen.layer;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.Interval;

public abstract sealed class AxisMapping {

	public final Interval domain;

	protected AxisMapping(Interval domain) {
		this.domain = domain;
	}

	// value -> t-value
	public abstract double remap(double v);

	// t-value -> value
	public abstract double unmap(double t);

	public static final class Linear extends AxisMapping {
		public Linear(Interval domain) {
			super(domain);
		}

		public Linear(double min, double max) {
			this(new Interval(min, max));
		}

		@Override
		public double remap(double v) {
			return this.domain.inverseLerp(v);
		}

		@Override
		public double unmap(double t) {
			return this.domain.lerp(t);
		}
	}

	public static final class Log extends AxisMapping {
		public final double base;

		public Log(double base, Interval domain) {
			super(domain);
			this.base = base;
		}

		public Log(double base, double min, double max) {
			this(base, new Interval(min, max));
		}

		@Override
		public double remap(double v) {
			v = Math.log(v) / Math.log(this.base);
			final var min = Math.log(this.domain.min) / Math.log(this.base);
			final var max = Math.log(this.domain.max) / Math.log(this.base);
			return Mth.inverseLerp(v, min, max);
		}

		@Override
		public double unmap(double t) {
			final var min = Math.log(this.domain.min) / Math.log(this.base);
			final var max = Math.log(this.domain.max) / Math.log(this.base);
			t = Mth.lerp(t, min, max);
			return Math.pow(this.base, t);
		}
	}

}