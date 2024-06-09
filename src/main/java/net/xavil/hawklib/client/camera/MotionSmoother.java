package net.xavil.hawklib.client.camera;

import net.xavil.hawklib.math.NumericOps;

public class MotionSmoother<T> {

	public final double smoothingFactor;
	private final NumericOps<T> ops;

	public T target;
	public T current;

	public MotionSmoother(double smoothingFactor, NumericOps<T> ops) {
		this(smoothingFactor, ops, ops.zero());
	}

	public MotionSmoother(double smoothingFactor, NumericOps<T> ops, T defaultValue) {
		this.smoothingFactor = smoothingFactor;
		this.ops = ops;
		this.target = this.current = defaultValue;
	}

	public void set(T value) {
		this.current = this.target = value;
	}

	public void tick(double dt) {
		this.current = this.ops.lerp(1 - Math.pow(this.smoothingFactor, dt), this.current, this.target);
	}

	@Override
	public String toString() {
		return "MotionSmoother[target=" + this.target + ", current=" + this.current + "]";
	}
}