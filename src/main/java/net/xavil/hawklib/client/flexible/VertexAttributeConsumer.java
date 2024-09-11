package net.xavil.hawklib.client.flexible;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.VecMath;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec2Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public interface VertexAttributeConsumer {

	void endVertex();

	interface Generic extends VertexAttributeConsumer {

		// ===== POSITION ===========================
		Generic vertex(float x, float y, float z);

		Generic vertex(float x, float y, float z, float w);

		default Generic vertex(double x, double y, double z) {
			return this.vertex((float) x, (float) y, (float) z);
		}

		default Generic vertex(double x, double y, double z, double w) {
			return this.vertex((float) x, (float) y, (float) z, (float) w);
		}

		default Generic vertex(Mat4Access tfm, double x, double y, double z) {
			final var xp = (tfm.r0c0() * x) + (tfm.r0c1() * y) + (tfm.r0c2() * z) + (tfm.r0c3() * 1);
			final var yp = (tfm.r1c0() * x) + (tfm.r1c1() * y) + (tfm.r1c2() * z) + (tfm.r1c3() * 1);
			final var zp = (tfm.r2c0() * x) + (tfm.r2c1() * y) + (tfm.r2c2() * z) + (tfm.r2c3() * 1);
			final var wp = (tfm.r3c0() * x) + (tfm.r3c1() * y) + (tfm.r3c2() * z) + (tfm.r3c3() * 1);
			return this.vertex((float) (xp / wp), (float) (yp / wp), (float) (zp / wp));
		}

		default Generic vertex(TransformStack tfm, double x, double y, double z) {
			return this.vertex(tfm.current(), x, y, z);
		}

		default Generic vertex(Vec3Access pos) {
			return this.vertex((float) pos.x(), (float) pos.y(), (float) pos.z());
		}

		default Generic vertex(Mat4Access tfm, Vec3Access pos) {
			pos = VecMath.transformPerspective(tfm, pos, 1);
			return this.vertex((float) pos.x(), (float) pos.y(), (float) pos.z());
		}

		default Generic vertex(TransformStack tfm, Vec3Access pos) {
			pos = VecMath.transformPerspective(tfm.current(), pos, 1);
			return this.vertex((float) pos.x(), (float) pos.y(), (float) pos.z());
		}

		// ===== COLOR ==============================
		Generic color(float r, float g, float b, float a);

		default Generic color(ColorRgba color) {
			return this.color(color.r(), color.g(), color.b(), color.a());
		}

		// ===== UV =================================
		Generic uv0(float u, float v);

		default Generic uv0(Vec2Access uv) {
			return this.uv0((float) uv.x(), (float) uv.y());
		}

		Generic uv1(float u, float v);

		default Generic uv1(Vec2Access uv) {
			return this.uv1((float) uv.x(), (float) uv.y());
		}

		Generic uv2(float u, float v);

		default Generic uv2(Vec2Access uv) {
			return this.uv2((float) uv.x(), (float) uv.y());
		}

		// ===== NORMAL =============================
		Generic normal(float x, float y, float z);

		default Generic normal(double x, double y, double z) {
			return this.normal((float) x, (float) y, (float) z);
		}

		default Generic normal(Mat4Access tfm, double x, double y, double z) {
			final var xp = (tfm.r0c0() * x) + (tfm.r0c1() * y) + (tfm.r0c2() * z);
			final var yp = (tfm.r1c0() * x) + (tfm.r1c1() * y) + (tfm.r1c2() * z);
			final var zp = (tfm.r2c0() * x) + (tfm.r2c1() * y) + (tfm.r2c2() * z);
			final var wp = (tfm.r3c0() * x) + (tfm.r3c1() * y) + (tfm.r3c2() * z);
			return this.normal((float) (xp / wp), (float) (yp / wp), (float) (zp / wp));
		}

		default Generic normal(TransformStack tfm, double x, double y, double z) {
			return this.normal(tfm.current(), x, y, z);
		}

		default Generic normal(Vec3Access norm) {
			return this.normal((float) norm.x(), (float) norm.y(), (float) norm.z());
		}

		default Generic normal(Mat4Access tfm, Vec3Access pos) {
			pos = VecMath.transformPerspective(tfm, pos, 0);
			return this.normal((float) pos.x(), (float) pos.y(), (float) pos.z());
		}

		default Generic normal(TransformStack tfm, Vec3Access pos) {
			pos = VecMath.transformPerspective(tfm.current(), pos, 0);
			return this.normal((float) pos.x(), (float) pos.y(), (float) pos.z());
		}

	}

	// ===== WRAPPER ============================
	static Generic wrapVanilla(VertexConsumer consumer) {
		return new Generic() {

			@Override
			public void endVertex() {
				consumer.endVertex();
			}

			@Override
			public Generic vertex(float x, float y, float z, float w) {
				consumer.vertex(x, y, z);
				return this;
			}

			@Override
			public Generic vertex(float x, float y, float z) {
				consumer.vertex(x, y, z);
				return this;
			}

			@Override
			public Generic color(float r, float g, float b, float a) {
				consumer.color(r, g, b, a);
				return this;
			}

			@Override
			public Generic uv0(float u, float v) {
				consumer.uv(u, v);
				return this;
			}

			@Override
			public Generic uv1(float u, float v) {
				consumer.overlayCoords((int) u, (int) v);
				return this;
			}

			@Override
			public Generic uv2(float u, float v) {
				consumer.uv2((int) u, (int) v);
				return this;
			}

			@Override
			public Generic normal(float x, float y, float z) {
				consumer.normal(x, y, z);
				return this;
			}
		};
	}

	static VertexConsumer asVanilla(Generic consumer) {
		return new DefaultedVertexConsumer() {
			@Override
			public VertexConsumer vertex(double x, double y, double z) {
				consumer.vertex(x, y, z);
				return this;
			}

			@Override
			public VertexConsumer color(int r, int g, int b, int a) {
				consumer.color(r / 255f, g / 255f, b / 255f, a / 255f);
				return this;
			}

			@Override
			public VertexConsumer color(float r, float g, float b, float a) {
				consumer.color(r, g, b, a);
				return this;
			}

			@Override
			public VertexConsumer uv(float u, float v) {
				consumer.uv0(u, v);
				return this;
			}

			@Override
			public VertexConsumer overlayCoords(int u, int v) {
				consumer.uv1(u, v);
				return this;
			}

			@Override
			public VertexConsumer uv2(int u, int v) {
				consumer.uv2(u, v);
				return this;
			}

			@Override
			public VertexConsumer normal(float x, float y, float z) {
				consumer.normal(x, y, z);
				return this;
			}

			@Override
			public void endVertex() {
				consumer.endVertex();
			}
		};
	}

}
