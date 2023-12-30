package net.xavil.hawklib.client.gl.texture;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

import net.xavil.hawklib.Disposable;

/**
 * This class holds a native image buffer (rgba8 unorm)
 */
public final class GlClientTexture implements Disposable {

	public static final int MAX_TEXTURE_SIZE = 512 * 1024 * 1024;

	private ByteBuffer data;
	private ClientFormat format;
	private int sizeX, sizeY, sizeZ;

	@FunctionalInterface
	public interface SubpixelWriter {
		void write(ByteBuffer data, int index, float c);
	}

	// @formatter:off
	private static final float   BYTE_MAX =      (float)  Byte.MAX_VALUE;
	private static final float  SHORT_MAX =      (float) Short.MAX_VALUE;
	private static final float  UBYTE_MAX = 2f * (float)  Byte.MAX_VALUE;
	private static final float USHORT_MAX = 2f * (float) Short.MAX_VALUE;
	// @formatter:on

	public static enum ClientFormat {
		// @formatter:off
		RGBA8_INT_NORM  (4,  1, GlTexture.Format.RGBA8_INT_NORM,   GL45C.GL_BYTE,           (data, i, x) -> data.put     (i,  (byte) (  BYTE_MAX * x))),
		RGBA8_UINT_NORM (4,  1, GlTexture.Format.RGBA8_UINT_NORM,  GL45C.GL_UNSIGNED_BYTE,  (data, i, x) -> data.put     (i,  (byte) ( UBYTE_MAX * x))),
		RGBA8_INT       (4,  1, GlTexture.Format.RGBA8_INT,        GL45C.GL_BYTE,           (data, i, x) -> data.put     (i,  (byte) (             x))),
		RGBA8_UINT      (4,  1, GlTexture.Format.RGBA8_UINT,       GL45C.GL_UNSIGNED_BYTE,  (data, i, x) -> data.put     (i,  (byte) (             x))),
		RGBA16_INT_NORM (8,  2, GlTexture.Format.RGBA16_INT_NORM,  GL45C.GL_SHORT,          (data, i, x) -> data.putShort(i, (short) ( SHORT_MAX * x))),
		RGBA16_UINT_NORM(8,  2, GlTexture.Format.RGBA16_UINT_NORM, GL45C.GL_UNSIGNED_SHORT, (data, i, x) -> data.putShort(i, (short) (USHORT_MAX * x))),
		RGBA16_INT      (8,  2, GlTexture.Format.RGBA16_INT,       GL45C.GL_SHORT,          (data, i, x) -> data.putShort(i, (short) (             x))),
		RGBA16_UINT     (8,  2, GlTexture.Format.RGBA16_UINT,      GL45C.GL_UNSIGNED_SHORT, (data, i, x) -> data.putShort(i, (short) (             x))),
		RGBA32_INT      (16, 4, GlTexture.Format.RGBA32_INT,       GL45C.GL_INT,            (data, i, x) -> data.putInt  (i,   (int) (             x))),
		RGBA32_UINT     (16, 4, GlTexture.Format.RGBA32_UINT,      GL45C.GL_UNSIGNED_INT,   (data, i, x) -> data.putInt  (i,   (int) (             x))),
		RGBA32_FLOAT    (16, 4, GlTexture.Format.RGBA32_FLOAT,     GL45C.GL_FLOAT,          (data, i, x) -> data.putFloat(i,         (             x))),
		// @formatter:on
		;

		public final int pixelStride;
		public final int subpixelStride;
		public final GlTexture.Format glFormat;
		public final int glType;
		public final SubpixelWriter writer;

		private ClientFormat(int pixelStride, int subpixelStride, GlTexture.Format glFormat, int glType,
				SubpixelWriter writer) {
			this.pixelStride = pixelStride;
			this.subpixelStride = subpixelStride;
			this.glFormat = glFormat;
			this.glType = glType;
			this.writer = writer;
		}

		public void putPixel(ByteBuffer buf, int i, float r, float g, float b, float a) {
			this.writer.write(buf, i + this.subpixelStride * 0, r);
			this.writer.write(buf, i + this.subpixelStride * 1, g);
			this.writer.write(buf, i + this.subpixelStride * 2, b);
			this.writer.write(buf, i + this.subpixelStride * 3, a);
		}
	}

	@Override
	public void close() {
		if (this.data != null)
			MemoryUtil.memFree(this.data);
	}

	public ByteBuffer imageData() {
		return this.data;
	}

	public int sizeX() {
		return this.sizeX;
	}

	public int sizeY() {
		return this.sizeY;
	}

	public int sizeZ() {
		return this.sizeZ;
	}

	public void createStorage(ClientFormat format, int sizeX, int sizeY, int sizeZ) {
		final var bufferSize = format.pixelStride * sizeX * sizeY * sizeZ;
		if (bufferSize <= 0 || bufferSize > MAX_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"cannot create texture storage of size (%d, %d, %d) (%d bytes)",
					sizeX, sizeY, sizeZ, bufferSize));
		}

		if (this.data != null)
			this.close();
		this.data = MemoryUtil.memCalloc(bufferSize);
		this.format = format;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	public void clear(float r, float g, float b, float a) {
		final var pixelCount = sizeX * sizeY * sizeZ;
		for (int i = 0; i < pixelCount; ++i) {
			final var pixelIndex = this.format.pixelStride * i;
			this.format.putPixel(this.data, pixelIndex, r, g, b, a);
		}
	}

	private void validateIndex(int x, int y, int z) {
		if (this.data == null) {
			throw new IllegalStateException(String.format(
					"tried to do pixel operation on client image whose storage was not allocated"));
		}
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
			throw new IllegalArgumentException(String.format(
					"image index of (%d, %d, %d) is out of bounds for texture of size (%d, %d, %d)",
					x, y, z, this.sizeX, this.sizeY, this.sizeZ));
		}
	}

	private int pixelIndex(int x, int y, int z) {
		return this.format.pixelStride * (x + y * this.sizeX + z * this.sizeX * this.sizeY);
	}

	public void setPixel(int x, float r, float g, float b, float a) {
		setPixel(x, 0, 0, r, g, b, a);
	}

	public void setPixel(int x, int y, float r, float g, float b, float a) {
		setPixel(x, y, 0, r, g, b, a);
	}

	public void setPixel(int x, int y, int z, float r, float g, float b, float a) {
		validateIndex(x, y, z);
		this.format.putPixel(this.data, pixelIndex(x, y, z), r, g, b, a);
	}

	public void upload(GlTexture serverTexture, int layer) {
		// TODO: assert that the current buffer format is convertible to the gl texture
		// format
		if (layer > serverTexture.size.layers) {
			throw new IllegalArgumentException(String.format(
					"cannot upload to layer (%d), there are only (%d) layers in the dest image",
					layer, serverTexture.size.layers));
		}

		switch (serverTexture.type) {
			case D1, D1_ARRAY -> {
				if (serverTexture.size.width != this.sizeX) {
					throw new IllegalArgumentException(String.format(
							"client texture of size (%d) and gl texture of size (%d) are mismatched",
							this.sizeX, serverTexture.size.width));
				}
				GL45C.glTextureSubImage1D(serverTexture.id, layer, 0, this.sizeX,
						GL45C.GL_RGBA, this.format.glType, this.data);
			}
			case D2, D2_ARRAY -> {
				if (serverTexture.size.width != this.sizeX
						|| serverTexture.size.height != this.sizeY) {
					throw new IllegalArgumentException(String.format(
							"client texture of size (%d, %d) and gl texture of size (%d, %d) are mismatched",
							this.sizeX, this.sizeY,
							serverTexture.size.width, serverTexture.size.height));
				}
				GL45C.glTextureSubImage2D(serverTexture.id, layer, 0, 0, this.sizeX, this.sizeY,
						GL45C.GL_RGBA, this.format.glType, this.data);
			}
			case D3 -> {
				if (serverTexture.size.width != this.sizeX
						|| serverTexture.size.height != this.sizeY
						|| serverTexture.size.depth != this.sizeZ) {
					throw new IllegalArgumentException(String.format(
							"client texture of size (%d, %d, %d) and gl texture of size (%d, %d, %d) are mismatched",
							this.sizeX, this.sizeY, this.sizeZ,
							serverTexture.size.width, serverTexture.size.height, serverTexture.size.depth));
				}
				GL45C.glTextureSubImage3D(serverTexture.id, layer, 0, 0, 0, this.sizeX, this.sizeY, this.sizeZ,
						GL45C.GL_RGBA, this.format.glType, this.data);
			}
			default -> {
			}
		}
	}

	private <T extends GlTexture> T makeTexture(T value, Consumer<T> setup) {
		try {
			setup.accept(value);
			upload(value, 0);
			return value;
		} catch (Throwable t) {
			value.close();
			throw t;
		}
	}

	public GlTexture1d create1d(GlTexture.Format format) {
		return makeTexture(new GlTexture1d(),
				tex -> tex.createStorage(format, this.sizeX));
	}

	public GlTexture2d create2d(GlTexture.Format format) {
		return makeTexture(new GlTexture2d(false),
				tex -> tex.createStorage(format, this.sizeX, this.sizeY));
	}

	public GlTexture3d create3d(GlTexture.Format format) {
		return makeTexture(new GlTexture3d(),
				tex -> tex.createStorage(format, this.sizeX, this.sizeY, this.sizeZ));
	}

}
