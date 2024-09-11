package net.xavil.hawklib.client.camera;

import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;

public class MotionSmoother<T> {

	public interface Interpolator<T> {
		T lerp(double delta, T a, T b);

		static Interpolator<Double> DOUBLE = new Interpolator<Double>() {
			@Override
			public Double lerp(double t, Double a, Double b) {
				return a + t * (b - a);
			}
		};

		static Interpolator<Vec2> VEC2 = new Interpolator<Vec2>() {
			@Override
			public Vec2 lerp(double t, Vec2 a, Vec2 b) {
				return new Vec2(
						a.x + t * (b.x - a.x),
						a.y + t * (b.y - a.y));
			}
		};

		static Interpolator<Vec3> VEC3 = new Interpolator<Vec3>() {
			@Override
			public Vec3 lerp(double t, Vec3 a, Vec3 b) {
				return new Vec3(
						a.x + t * (b.x - a.x),
						a.y + t * (b.y - a.y),
						a.z + t * (b.z - a.z));
			}
		};

		static Interpolator<Quat> QUAT = new Interpolator<Quat>() {
			@Override
			public Quat lerp(double t, Quat a, Quat b) {
				return Quat.slerp(t, a, b);
			}
		};

		static Interpolator<UniversePosition> UNIVERSE_POS = new Interpolator<UniversePosition>() {
			@Override
			public UniversePosition lerp(double t, UniversePosition a, UniversePosition b) {
				return UniversePosition.lerp(t, a, b);
			}
		};

	}

	public final double smoothingFactor;
	private final Interpolator<T> interp;

	public T target;
	public T current;

	public MotionSmoother(double smoothingFactor, Interpolator<T> interp, T defaultValue) {
		this.smoothingFactor = smoothingFactor;
		this.interp = interp;
		this.target = this.current = defaultValue;
	}

	public void set(T value) {
		this.current = this.target = value;
	}

	public void tick(double dt) {
		// https://www.youtube.com/watch?v=LSNQuFEDOyQ
		final var t = 1 - Math.pow(this.smoothingFactor, dt);
		this.current = this.interp.lerp(t, this.current, this.target);
	}

	@Override
	public String toString() {
		return "MotionSmoother[target=" + this.target + ", current=" + this.current + "]";
	}
}