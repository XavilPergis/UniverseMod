package net.xavil.hawklib.client.gl.shader;

import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec2Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3iAccess;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public interface UniformHolder {

	void markDirty(boolean dirty);

	UniformSlot getSlot(String uniformName);

	default UniformSlot getSlot(String uniformName, UniformSlot.Type requestedType) {
		final var slot = getSlot(uniformName);
		if (slot == null || !UniformSlot.checkType(slot, requestedType))
			return null;
		return slot;
	}

	default void setUniformSampler(String uniformName, GlTexture texture) {
		final var uniformType = UniformSlot.Type.from(texture.format().samplerType, texture.type);
		final var slot = getSlot(uniformName, uniformType);
		if (slot != null) {
			boolean d = false;
			d |= slot.setTexture(0, texture);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, int v0) {
		final var slot = getSlot(uniformName, UniformSlot.Type.INT1);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, int v0, int v1) {
		final var slot = getSlot(uniformName, UniformSlot.Type.INT2);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, int v0, int v1, int v2) {
		final var slot = getSlot(uniformName, UniformSlot.Type.INT3);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			d |= slot.setInt(2, v2);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, int v0, int v1, int v2, int v3) {
		final var slot = getSlot(uniformName, UniformSlot.Type.INT4);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			d |= slot.setInt(2, v2);
			d |= slot.setInt(3, v3);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, float v0) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT1);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, float v0, float v1) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT2);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, float v0, float v1, float v2) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT3);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			d |= slot.setFloat(2, v2);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, float v0, float v1, float v2, float v3) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT4);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			d |= slot.setFloat(2, v2);
			d |= slot.setFloat(3, v3);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniformMatrix2x2(String uniformName,
			float r0c0, float r0c1,
			float r1c0, float r1c1) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT_MAT2x2);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniformMatrix3x3(String uniformName,
			float r0c0, float r0c1, float r0c2,
			float r1c0, float r1c1, float r1c2,
			float r2c0, float r2c1, float r2c2) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT_MAT3x3);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r0c2);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			d |= slot.setFloat(i++, r1c2);
			d |= slot.setFloat(i++, r2c0);
			d |= slot.setFloat(i++, r2c1);
			d |= slot.setFloat(i++, r2c2);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniformMatrix4x4(String uniformName,
			float r0c0, float r0c1, float r0c2, float r0c3,
			float r1c0, float r1c1, float r1c2, float r1c3,
			float r2c0, float r2c1, float r2c2, float r2c3,
			float r3c0, float r3c1, float r3c2, float r3c3) {
		final var slot = getSlot(uniformName, UniformSlot.Type.FLOAT_MAT4x4);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r0c2);
			d |= slot.setFloat(i++, r0c3);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			d |= slot.setFloat(i++, r1c2);
			d |= slot.setFloat(i++, r1c3);
			d |= slot.setFloat(i++, r2c0);
			d |= slot.setFloat(i++, r2c1);
			d |= slot.setFloat(i++, r2c2);
			d |= slot.setFloat(i++, r2c3);
			d |= slot.setFloat(i++, r3c0);
			d |= slot.setFloat(i++, r3c1);
			d |= slot.setFloat(i++, r3c2);
			d |= slot.setFloat(i++, r3c3);
			slot.markDirty(d);
			this.markDirty(d);
		}
	}

	default void setUniform(String uniformName, double v0) {
		setUniform(uniformName, (float) v0);
	}

	default void setUniform(String uniformName, double v0, double v1) {
		setUniform(uniformName, (float) v0, (float) v1);
	}

	default void setUniform(String uniformName, double v0, double v1, double v2) {
		setUniform(uniformName, (float) v0, (float) v1, (float) v2);
	}

	default void setUniform(String uniformName, double v0, double v1, double v2, double v3) {
		setUniform(uniformName, (float) v0, (float) v1, (float) v2, (float) v3);
	}

	default void setUniform(String uniformName, Vec2i v) {
		setUniform(uniformName, v.x, v.y);
	}

	default void setUniform(String uniformName, Vec3iAccess v) {
		setUniform(uniformName, v.x(), v.y(), v.z());
	}

	default void setUniform(String uniformName, Vec2Access v) {
		setUniform(uniformName, v.x(), v.y());
	}

	default void setUniform(String uniformName, Vec3Access v) {
		setUniform(uniformName, v.x(), v.y(), v.z());
	}

	default void setUniform(String uniformName, Vec4Access v) {
		setUniform(uniformName, v.x(), v.y(), v.z(), v.w());
	}

	default void setUniform(String uniformName, Color color) {
		setUniform(uniformName, color.r(), color.g(), color.b(), color.a());
	}

	default void setUniform(String uniformName, Mat4Access v) {
		setUniformMatrix4x4(uniformName,
				(float) v.r0c0(), (float) v.r0c1(), (float) v.r0c2(), (float) v.r0c3(),
				(float) v.r1c0(), (float) v.r1c1(), (float) v.r1c2(), (float) v.r1c3(),
				(float) v.r2c0(), (float) v.r2c1(), (float) v.r2c2(), (float) v.r2c3(),
				(float) v.r3c0(), (float) v.r3c1(), (float) v.r3c2(), (float) v.r3c3());
	}

}
