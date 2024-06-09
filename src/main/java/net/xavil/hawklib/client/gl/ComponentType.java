package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL45C;

public enum ComponentType {
	FLOAT(GL45C.GL_FLOAT, 4, false, "Float", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putFloat(offset, value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			throw new UnsupportedOperationException("cannot write int via ComponentType.FLOAT");
		}
	}),
	INT(GL45C.GL_INT, 4, false, "Int", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putInt(offset, (int) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putInt(offset, value);
		}
	}),
	UINT(GL45C.GL_UNSIGNED_INT, 4, false, "Unsigned Int", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putInt(offset, (int) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putInt(offset, value);
		}
	}),
	SHORT(GL45C.GL_SHORT, 2, false, "Short", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putShort(offset, (short) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putShort(offset, (short) value);
		}
	}),
	USHORT(GL45C.GL_UNSIGNED_SHORT, 2, false, "Unsigned Short", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putShort(offset, (short) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putShort(offset, (short) value);
		}
	}),
	BYTE(GL45C.GL_BYTE, 1, false, "Byte", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.put(offset, (byte) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putShort(offset, (byte) value);
		}
	}),
	UBYTE(GL45C.GL_UNSIGNED_BYTE, 1, false, "Unsigned Byte", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.put(offset, (byte) value);
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			data.putShort(offset, (byte) value);
		}
	}),
	SHORT_NORM(GL45C.GL_SHORT, 2, true, "Normalized Short", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putShort(offset, (short) (SHORT_MAX * value));
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			throw new UnsupportedOperationException("cannot write int via ComponentType.SHORT_NORM");
		}
	}),
	USHORT_NORM(GL45C.GL_UNSIGNED_SHORT, 2, true, "Normalized Unsigned Short", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.putShort(offset, (short) (USHORT_MAX * value));
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			throw new UnsupportedOperationException("cannot write int via ComponentType.USHORT_NORM");
		}
	}),
	BYTE_NORM(GL45C.GL_BYTE, 1, true, "Normalized Byte", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.put(offset, (byte) (BYTE_MAX * value));
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			throw new UnsupportedOperationException("cannot write int via ComponentType.BYTE_NORM");
		}
	}),
	UBYTE_NORM(GL45C.GL_UNSIGNED_BYTE, 1, true, "Normalized Unsigned Byte", new Writer() {
		@Override
		public void writeFloat(ByteBuffer data, int offset, float value) {
			data.put(offset, (byte) (UBYTE_MAX * value));
		}
		@Override
		public void writeInt(ByteBuffer data, int offset, int value) {
			throw new UnsupportedOperationException("cannot write int via ComponentType.UBYTE_NORM");
		}
	}),
	;

	// @formatter:off
	private static final float   BYTE_MAX =      (float)    Byte.MAX_VALUE;
	private static final float  UBYTE_MAX = 2f * (float)    Byte.MAX_VALUE;
	private static final float  SHORT_MAX =      (float)   Short.MAX_VALUE;
	private static final float USHORT_MAX = 2f * (float)   Short.MAX_VALUE;
	// @formatter:on

	public final int gl;
	public final int byteSize;
	public final boolean isNormalized;
	public final String name;
	public final Writer writer;

	private ComponentType(int gl, int byteSize, boolean isNormalized, String name, Writer writer) {
		this.gl = gl;
		this.byteSize = byteSize;
		this.isNormalized = isNormalized;
		this.name = name;
		this.writer = writer;
	}

	public void writeFloatToBuffer(ByteBuffer data, int offset, float value) {
		this.writer.writeFloat(data, offset, value);
	}

	public interface Writer {
		void writeFloat(ByteBuffer data, int offset, float value);
		void writeInt(ByteBuffer data, int offset, int value);
	}
}