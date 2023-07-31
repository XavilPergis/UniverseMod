package net.xavil.hawklib.math;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec4;

public final class TransformStack {
	private final MutableList<Mat4.Mutable> stack = new Vector<>(1);
	private Mat4.Mutable current;
	private Mat4 cached = null;

	public TransformStack() {
		final var mat = new Mat4.Mutable().loadIdentity();
		this.stack.push(mat);
		this.current = mat;
	}

	public TransformStack prependTransform(Mat4Access tfm) {
		this.current.prependTransform(tfm);
		this.cached = null;
		return this;
	}
	
	public TransformStack appendTransform(Mat4Access tfm) {
		this.current.appendTransform(tfm);
		this.cached = null;
		return this;
	}
	public TransformStack prependRotation(Quat tfm) {
		this.current.prependRotation(tfm);
		this.cached = null;
		return this;
	}
	
	public TransformStack appendRotation(Quat tfm) {
		this.current.appendRotation(tfm);
		this.cached = null;
		return this;
	}
	public TransformStack prependTranslation(Vec3 tfm) {
		this.current.prependTranslation(tfm);
		this.cached = null;
		return this;
	}
	
	public TransformStack appendTranslation(Vec3 tfm) {
		this.current.appendTranslation(tfm);
		this.cached = null;
		return this;
	}

	public Vec3 applyTransform(Vec3Access vec, double w) {
		return Mat4.mul(this.current, vec, w);
	}

	public Vec4 applyTransform(Vec4Access vec) {
		return Mat4.mul(this.current, vec);
	}

	public Mat4 get() {
		if (this.cached == null) {
			this.cached = this.current.asImmutable();
		}
		return this.cached;
	}

	public void push() {
		this.current = this.current.copy();
		this.stack.push(this.current);
	}

	public void pop() {
		this.stack.remove(this.stack.size() - 1);
		this.current = this.stack.get(this.stack.size() - 1);
		if (this.stack.isEmpty()) {
			this.stack.push(new Mat4.Mutable().loadIdentity());
		}
	}
}
