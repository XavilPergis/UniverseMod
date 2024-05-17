package net.xavil.hawklib.client.flexible;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public abstract class VertexBuilder {

	public final class BuiltBuffer implements Disposable {
		public final BufferLayout layout;

		public final int vertexCount;
		public final ByteBuffer vertexData;
		public final PrimitiveType primitiveType;

		public final int indexCount;
		public final ByteBuffer indexData;
		public final VertexFormat.IndexType indexType;

		private boolean isDrawn = false;

		public BuiltBuffer(
				ByteBuffer vertexData, ByteBuffer indexData, BufferLayout layout,
				PrimitiveType primitiveType, IndexType indexType,
				int vertexCount, int indexCount) {
			this.layout = layout;
			this.vertexCount = vertexCount;
			this.indexCount = indexCount;
			this.primitiveType = primitiveType;
			this.indexType = indexType;

			this.vertexData = vertexData;
			this.indexData = indexData;
		}

		@Override
		public void close() {
			if (!this.isDrawn) {
				this.isDrawn = true;
				VertexBuilder.this.handleBufferDrawn(this);
			}
		}

		public void draw(ShaderProgram shader, DrawState drawState) {
			BufferRenderer.draw(shader, this, drawState);
			close();
		}

		public void drawWithoutClosing(ShaderProgram shader, DrawState drawState) {
			BufferRenderer.draw(shader, this, drawState);
		}
	}

	// @formatter:off
	private static final float   BYTE_MAX =      (float)    Byte.MAX_VALUE;
	private static final float  SHORT_MAX =      (float)   Short.MAX_VALUE;
	private static final float    INT_MAX =      (float) Integer.MAX_VALUE;
	private static final float  UBYTE_MAX = 2f * (float)    Byte.MAX_VALUE;
	private static final float USHORT_MAX = 2f * (float)   Short.MAX_VALUE;
	private static final float   UINT_MAX = 2f * (float) Integer.MAX_VALUE;
	// @formatter:on

	private static FloatElementConsumer floatWriterForType(BufferLayout.BuiltElement elem, ByteBuffer buf) {
		// @formatter:off
		if (!elem.isNormalized) return switch (elem.type) {
			case BYTE   -> (off, n) -> buf.put     (off,  (byte) n);
			case UBYTE  -> (off, n) -> buf.put     (off,  (byte) n);
			case SHORT  -> (off, n) -> buf.putShort(off, (short) n);
			case USHORT -> (off, n) -> buf.putShort(off, (short) n);
			case INT    -> (off, n) -> buf.putInt  (off,   (int) n);
			case UINT   -> (off, n) -> buf.putInt  (off,   (int) n);
			case FLOAT  -> (off, n) -> buf.putFloat(off,         n);
		};
		return switch (elem.type) {
			case BYTE   -> (off, n) -> buf.put     (off,  (byte) (n *   BYTE_MAX));
			case UBYTE  -> (off, n) -> buf.put     (off,  (byte) (n *  UBYTE_MAX));
			case SHORT  -> (off, n) -> buf.putShort(off, (short) (n *  SHORT_MAX));
			case USHORT -> (off, n) -> buf.putShort(off, (short) (n * USHORT_MAX));
			case INT    -> (off, n) -> buf.putInt  (off,   (int) (n *    INT_MAX));
			case UINT   -> (off, n) -> buf.putInt  (off,   (int) (n *   UINT_MAX));
			case FLOAT  -> (off, n) -> buf.putFloat(off,         (n             ));
		};
		// @formatter:on
	}

	private static IntElementConsumer intWriterForType(BufferLayout.BuiltElement elem, ByteBuffer buf) {
		// @formatter:off
		return switch (elem.type) {
			case BYTE   -> (off, n) -> buf.put     (off,  (byte) n);
			case UBYTE  -> (off, n) -> buf.put     (off,  (byte) n);
			case SHORT  -> (off, n) -> buf.putShort(off, (short) n);
			case USHORT -> (off, n) -> buf.putShort(off, (short) n);
			case INT    -> (off, n) -> buf.putInt  (off,         n);
			case UINT   -> (off, n) -> buf.putInt  (off,         n);
			default     -> throw new IllegalStateException(String.format(
				"Cannot create int consumer for element type '%s'",
				elem.type));
		};
		// @formatter:on
	}

	private static interface FloatElementConsumer {
		void emit(int relativeOffset, float value);
	}

	private static interface IntElementConsumer {
		void emit(int relativeOffset, int value);
	}

	private static abstract class ElementHolder {
		public final BufferLayout.BuiltElement element;
		public final int c0o, c1o, c2o, c3o;

		public ElementHolder(BufferLayout.BuiltElement element) {
			this.element = element;
			final var elementSize = element.type.byteSize;
			this.c0o = (0 * elementSize) + element.byteOffset;
			this.c1o = (1 * elementSize) + element.byteOffset;
			this.c2o = (2 * elementSize) + element.byteOffset;
			this.c3o = (3 * elementSize) + element.byteOffset;
		}

		// @formatter:off
		public abstract void setFloat0(float value);
		public abstract void setFloat1(float value);
		public abstract void setFloat2(float value);
		public abstract void setFloat3(float value);

		public abstract void setInt0(int value);
		public abstract void setInt1(int value);
		public abstract void setInt2(int value);
		public abstract void setInt3(int value);
		// @formatter:on

		public abstract void emit(int vertexBase);
	}

	private static class FloatElementHolder extends ElementHolder {
		public final FloatElementConsumer consumer;
		// "c" for "component"
		public float c0, c1, c2, c3;

		public FloatElementHolder(FloatElementConsumer consumer, BufferLayout.BuiltElement element) {
			super(element);
			this.consumer = consumer;
		}

		// @formatter:off
		@Override public void setFloat0(float value) { this.c0 = value; }
		@Override public void setFloat1(float value) { this.c1 = value; }
		@Override public void setFloat2(float value) { this.c2 = value; }
		@Override public void setFloat3(float value) { this.c3 = value; }
		@Override public void setInt0(int value) { this.c0 = value; }
		@Override public void setInt1(int value) { this.c1 = value; }
		@Override public void setInt2(int value) { this.c2 = value; }
		@Override public void setInt3(int value) { this.c3 = value; }
		// @formatter:on

		@Override
		public void emit(int vertexBase) {
			// @formatter:off
			switch (element.componentCount) {
				case 4: this.consumer.emit(vertexBase + this.c3o, this.c3);
				case 3: this.consumer.emit(vertexBase + this.c2o, this.c2);
				case 2: this.consumer.emit(vertexBase + this.c1o, this.c1);
				case 1: this.consumer.emit(vertexBase + this.c0o, this.c0);
				default:
			}
			// @formatter:on
		}
	}

	private static class IntElementHolder extends ElementHolder {
		public final IntElementConsumer consumer;
		public int c0, c1, c2, c3;

		public IntElementHolder(IntElementConsumer consumer, BufferLayout.BuiltElement element) {
			super(element);
			this.consumer = consumer;
		}

		// @formatter:off
		@Override public void setFloat0(float value) { this.c0 = (int) value; }
		@Override public void setFloat1(float value) { this.c1 = (int) value; }
		@Override public void setFloat2(float value) { this.c2 = (int) value; }
		@Override public void setFloat3(float value) { this.c3 = (int) value; }
		@Override public void setInt0(int value) { this.c0 = value; }
		@Override public void setInt1(int value) { this.c1 = value; }
		@Override public void setInt2(int value) { this.c2 = value; }
		@Override public void setInt3(int value) { this.c3 = value; }
		// @formatter:on

		@Override
		public void emit(int vertexBase) {
			// @formatter:off
			switch (element.componentCount) {
				case 4: this.consumer.emit(vertexBase + this.c3o, this.c3);
				case 3: this.consumer.emit(vertexBase + this.c2o, this.c2);
				case 2: this.consumer.emit(vertexBase + this.c1o, this.c1);
				case 1: this.consumer.emit(vertexBase + this.c0o, this.c0);
				default:
			}
			// @formatter:on
		}
	}

	public static abstract class VertexDispatcher {
		protected VertexBuilder builder;
		protected ElementHolder[] activeHolders;

		public final void setup(VertexBuilder builder, BufferLayout layout, ByteBuffer buf) {
			this.builder = builder;

			final var holdersList = new Vector<ElementHolder>();
			for (final var element : layout.elements.iterable()) {
				holdersList.push(switch (element.attribType) {
					case INT -> new IntElementHolder(intWriterForType(element, buf), element);
					case FLOAT -> new FloatElementHolder(floatWriterForType(element, buf), element);
				});
			}
			this.activeHolders = holdersList.toArray(ElementHolder.class);

			setupHolders();
		}

		public abstract void setupHolders();

		public final void emit(int vertexBase) {
			for (final var holder : this.activeHolders)
				holder.emit(vertexBase);
		}

		public final BuiltBuffer end() {
			return this.builder.end();
		}
	}

	public static final class GenericVertexDispatcher extends VertexDispatcher implements FlexibleVertexConsumer {

		public ElementHolder position;
		public ElementHolder color;
		public ElementHolder normal;
		public ElementHolder uv0, uv1, uv2;

		private void addHolder(ElementHolder holder) {
			switch (holder.element.usage) {
				case POSITION -> this.position = holder;
				case COLOR -> this.color = holder;
				case NORMAL -> this.normal = holder;
				case UV -> {
					switch (holder.element.index) {
						case 0 -> this.uv0 = holder;
						case 1 -> this.uv1 = holder;
						case 2 -> this.uv2 = holder;
						default -> {
						}
					}
				}
				default -> {
				}
			}
		}

		@Override
		public void setupHolders() {
			for (final var holder : this.activeHolders)
				addHolder(holder);
		}

		@Override
		public void endVertex() {
			this.builder.emitVertex();
		}

		@Override
		public GenericVertexDispatcher vertex(double x, double y, double z) {
			this.position.setFloat0((float) x);
			this.position.setFloat1((float) y);
			this.position.setFloat2((float) z);
			return this;
		}

		@Override
		public GenericVertexDispatcher vertex(float x, float y, float z) {
			this.position.setFloat0(x);
			this.position.setFloat1(y);
			this.position.setFloat2(z);
			return this;
		}

		@Override
		public GenericVertexDispatcher vertex(Vec3Access pos) {
			this.position.setFloat0((float) pos.x());
			this.position.setFloat1((float) pos.y());
			this.position.setFloat2((float) pos.z());
			return this;
		}

		@Override
		public GenericVertexDispatcher color(ColorRgba color) {
			this.color.setFloat0(color.r());
			this.color.setFloat1(color.g());
			this.color.setFloat2(color.b());
			this.color.setFloat3(color.a());
			return this;
		}

		@Override
		public GenericVertexDispatcher color(float r, float g, float b, float a) {
			this.color.setFloat0(r);
			this.color.setFloat1(g);
			this.color.setFloat2(b);
			this.color.setFloat3(a);
			return this;
		}

		@Override
		public GenericVertexDispatcher uv0(float u, float v) {
			this.uv0.setFloat0(u);
			this.uv0.setFloat1(v);
			return this;
		}

		@Override
		public GenericVertexDispatcher uv1(float u, float v) {
			this.uv1.setFloat0(u);
			this.uv1.setFloat1(v);
			return this;
		}

		@Override
		public GenericVertexDispatcher uv2(float u, float v) {
			this.uv2.setFloat0(u);
			this.uv2.setFloat1(v);
			return this;
		}

		@Override
		public GenericVertexDispatcher normal(double x, double y, double z) {
			this.normal.setFloat0((float) x);
			this.normal.setFloat1((float) y);
			this.normal.setFloat2((float) z);
			return this;
		}

		@Override
		public GenericVertexDispatcher normal(float x, float y, float z) {
			this.normal.setFloat0(x);
			this.normal.setFloat1(y);
			this.normal.setFloat2(z);
			return this;
		}

		@Override
		public GenericVertexDispatcher normal(Vec3Access norm) {
			this.normal.setFloat0((float) norm.x());
			this.normal.setFloat1((float) norm.y());
			this.normal.setFloat2((float) norm.z());
			return this;
		}

	}

	protected final GenericVertexDispatcher genericDispatcher = new GenericVertexDispatcher();

	protected abstract void handleBufferDrawn(BuiltBuffer buffer);

	protected abstract void emitVertex();

	protected abstract BuiltBuffer end();

	public abstract <T extends VertexDispatcher> T begin(T dispatcher, PrimitiveType mode, BufferLayout layout);

	public GenericVertexDispatcher beginGeneric(PrimitiveType mode, BufferLayout layout) {
		return begin(this.genericDispatcher, mode, layout);
	}

	public abstract boolean isBuilding();

	public abstract void reset();

}
