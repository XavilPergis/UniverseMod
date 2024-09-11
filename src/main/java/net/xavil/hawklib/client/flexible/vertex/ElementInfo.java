package net.xavil.hawklib.client.flexible.vertex;

import java.nio.ByteBuffer;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.gl.ComponentType;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.ultraviolet.Mod;

public abstract class ElementInfo {

	public final BufferLayout.BuiltElement element;
	public final int[] offsets;
	public final ByteBuffer buffer;
	public final ComponentType.Writer writer;

	public ElementInfo(ByteBuffer buffer, BufferLayout.BuiltElement element) {
		this.element = element;
		this.offsets = new int[element.elementCount];
		for (int i = 0; i < element.elementCount; ++i) {
			this.offsets[i] = (i * element.type.byteSize) + element.byteOffset;
		}
		this.buffer = buffer;
		this.writer = element.type.writer;
	}

	protected void logOutOfBoundsAccess(int vertexBase, int component) {
		Mod.LOGGER.error(
				"Index {} out of bounds for buffer of size {}. Setting component {} at offset {} of element {}",
				vertexBase + this.offsets[component],
				this.buffer.limit(),
				component,
				this.offsets[component],
				this.element);
	}

	public static final class Float extends ElementInfo {

		public Float(ByteBuffer buffer, BufferLayout.BuiltElement element) {
			super(buffer, element);
		}

		public void setFloat(int vertexBase, int component, float value) {
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[component], value);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, component);
				throw ex;
			}
		}

		public void setFloats(int vertexBase, float c0) {
			int i = 0;
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, i);
				throw ex;
			}
		}

		public void setFloats(int vertexBase, float c0, float c1) {
			int i = 0;
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, i);
				throw ex;
			}
		}

		public void setFloats(int vertexBase, float c0, float c1, float c2) {
			int i = 0;
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c2);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, i);
				throw ex;
			}
		}

		public void setFloats(int vertexBase, float c0, float c1, float c2, float c3) {
			int i = 0;
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c2);
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c3);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, i);
				throw ex;
			}
		}

		public void setFloats(int vertexBase, Mat4Access value) {
			int i = 0;
			try {
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c0());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c0());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c0());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c0());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c1());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c1());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c1());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c1());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c2());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c2());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c2());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c2());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c3());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c3());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c3());
				this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c3());
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, i);
				throw ex;
			}
		}
	}

	public static final class Int extends ElementInfo {

		public Int(ByteBuffer buffer, BufferLayout.BuiltElement element) {
			super(buffer, element);
		}

		public void setInt(int vertexBase, int component, int value) {
			try {
				this.writer.writeInt(this.buffer, vertexBase + this.offsets[component], value);
			} catch (IndexOutOfBoundsException ex) {
				logOutOfBoundsAccess(vertexBase, component);
				throw ex;
			}
		}
	}

}