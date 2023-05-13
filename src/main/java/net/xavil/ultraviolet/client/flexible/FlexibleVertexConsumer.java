package net.xavil.ultraviolet.client.flexible;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.xavil.util.math.Color;
import net.xavil.util.math.matrices.interfaces.Vec2Access;
import net.xavil.util.math.matrices.interfaces.Vec3Access;

public interface FlexibleVertexConsumer {

	void endVertex();

	// ===== POSITION ===========================
	FlexibleVertexConsumer vertex(float x, float y, float z);

	default FlexibleVertexConsumer vertex(double x, double y, double z) {
		return this.vertex((float) x, (float) y, (float) z);
	}

	default FlexibleVertexConsumer vertex(Vec3Access pos) {
		return this.vertex((float) pos.x(), (float) pos.y(), (float) pos.z());
	}

	// ===== COLOR ==============================
	FlexibleVertexConsumer color(float r, float g, float b, float a);

	default FlexibleVertexConsumer color(Color color) {
		return this.color(color.r(), color.g(), color.b(), color.a());
	}

	// ===== UV =================================
	FlexibleVertexConsumer uv0(float u, float v);

	default FlexibleVertexConsumer uv0(Vec2Access uv) {
		return this.uv0((float) uv.x(), (float) uv.y());
	}

	FlexibleVertexConsumer uv1(float u, float v);

	default FlexibleVertexConsumer uv1(Vec2Access uv) {
		return this.uv1((float) uv.x(), (float) uv.y());
	}

	FlexibleVertexConsumer uv2(float u, float v);

	default FlexibleVertexConsumer uv2(Vec2Access uv) {
		return this.uv2((float) uv.x(), (float) uv.y());
	}

	// ===== NORMAL =============================
	FlexibleVertexConsumer normal(float x, float y, float z);

	default FlexibleVertexConsumer normal(double x, double y, double z) {
		return this.normal((float) x, (float) y, (float) z);
	}

	default FlexibleVertexConsumer normal(Vec3Access norm) {
		return this.normal((float) norm.x(), (float) norm.y(), (float) norm.z());
	}

	// ===== WRAPPER ============================
	static FlexibleVertexConsumer wrapVanilla(VertexConsumer consumer) {
		return new FlexibleVertexConsumer() {

			@Override
			public void endVertex() {
				consumer.endVertex();
			}

			@Override
			public FlexibleVertexConsumer vertex(float x, float y, float z) {
				consumer.vertex(x, y, z);
				return this;
			}

			@Override
			public FlexibleVertexConsumer color(float r, float g, float b, float a) {
				consumer.color(r, g, b, a);
				return this;
			}

			@Override
			public FlexibleVertexConsumer uv0(float u, float v) {
				consumer.uv(u, v);
				return this;
			}

			@Override
			public FlexibleVertexConsumer uv1(float u, float v) {
				consumer.overlayCoords((int) u, (int) v);
				return this;
			}

			@Override
			public FlexibleVertexConsumer uv2(float u, float v) {
				consumer.uv2((int) u, (int) v);
				return this;
			}

			@Override
			public FlexibleVertexConsumer normal(float x, float y, float z) {
				consumer.normal(x, y, z);
				return this;
			}
		};
	}

	default VertexConsumer asVanilla() {
		return new DefaultedVertexConsumer() {
			@Override
			public VertexConsumer vertex(double x, double y, double z) {
				FlexibleVertexConsumer.this.vertex(x, y, z);
				return this;
			}

			@Override
			public VertexConsumer color(int r, int g, int b, int a) {
				FlexibleVertexConsumer.this.color(r / 255f, g / 255f, b / 255f, a / 255f);
				return this;
			}

			@Override
			public VertexConsumer color(float r, float g, float b, float a) {
				FlexibleVertexConsumer.this.color(r, g, b, a);
				return this;
			}

			@Override
			public VertexConsumer uv(float u, float v) {
				FlexibleVertexConsumer.this.uv0(u, v);
				return this;
			}

			@Override
			public VertexConsumer overlayCoords(int u, int v) {
				FlexibleVertexConsumer.this.uv1(u, v);
				return this;
			}

			@Override
			public VertexConsumer uv2(int u, int v) {
				FlexibleVertexConsumer.this.uv2(u, v);
				return this;
			}

			@Override
			public VertexConsumer normal(float x, float y, float z) {
				FlexibleVertexConsumer.this.normal(x, y, z);
				return this;
			}

			@Override
			public void endVertex() {
				FlexibleVertexConsumer.this.endVertex();
			}
		};
	}

}
