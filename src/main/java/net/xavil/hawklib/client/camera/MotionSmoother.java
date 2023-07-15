package net.xavil.hawklib.client.camera;

import net.xavil.hawklib.math.NumericOps;

public class MotionSmoother<T> {

	public final double smoothingFactor;
	private final NumericOps<T> ops;

	public T target;
	public T current;
	public T previous;

	public MotionSmoother(double smoothingFactor, NumericOps<T> ops) {
		this.smoothingFactor = smoothingFactor;
		this.ops = ops;
		this.target = this.current = this.previous = ops.zero();
	}

	public void set(T value) {
		this.current = this.previous = this.target = value;
	}

	public T get(float partialTick) {
		return this.ops.lerp(partialTick, this.previous, this.current);
	}

	public void tick() {
		this.previous = this.current;
		this.current = this.ops.lerp(this.smoothingFactor, this.current, this.target);
	}

	@Override
	public String toString() {
		return "MotionSmoother[target=" + this.target + ", current=" + this.current + "]";
	}
}