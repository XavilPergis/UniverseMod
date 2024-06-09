package net.xavil.hawklib.client.gl.texture;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.ComponentType;

/**
 * This class holds a native image buffer (rgba8 unorm)
 */
public final class GlClientTexture implements Disposable {

	public static final int MAX_TEXTURE_SIZE = 512 * 1024 * 1024;

	private ByteBuffer data;
	private ClientFormat format;
	private int sizeX, sizeY, sizeZ;

	public static enum ClientFormat {
		// @formatter:off
		RGBA8_INT_NORM  (GlTexture.Format.RGBA8_INT_NORM,   ComponentType.BYTE_NORM  ),
		RGBA8_UINT_NORM (GlTexture.Format.RGBA8_UINT_NORM,  ComponentType.UBYTE_NORM ),
		RGBA16_INT_NORM (GlTexture.Format.RGBA16_INT_NORM,  ComponentType.SHORT_NORM ),
		RGBA16_UINT_NORM(GlTexture.Format.RGBA16_UINT_NORM, ComponentType.USHORT_NORM),
		RGBA32_FLOAT    (GlTexture.Format.RGBA32_FLOAT,     ComponentType.FLOAT      ),
		RGB8_INT_NORM   (GlTexture.Format.RGB8_INT_NORM,    ComponentType.BYTE_NORM  ),
		RGB8_UINT_NORM  (GlTexture.Format.RGB8_UINT_NORM,   ComponentType.UBYTE_NORM ),
		RGB16_INT_NORM  (GlTexture.Format.RGB16_INT_NORM,   ComponentType.SHORT_NORM ),
		RGB16_UINT_NORM (GlTexture.Format.RGB16_UINT_NORM,  ComponentType.USHORT_NORM),
		RGB32_FLOAT     (GlTexture.Format.RGB32_FLOAT,      ComponentType.FLOAT      ),
		RG8_INT_NORM    (GlTexture.Format.RG8_INT_NORM,     ComponentType.BYTE_NORM  ),
		RG8_UINT_NORM   (GlTexture.Format.RG8_UINT_NORM,    ComponentType.UBYTE_NORM ),
		RG16_INT_NORM   (GlTexture.Format.RG16_INT_NORM,    ComponentType.SHORT_NORM ),
		RG16_UINT_NORM  (GlTexture.Format.RG16_UINT_NORM,   ComponentType.USHORT_NORM),
		RG32_FLOAT      (GlTexture.Format.RG32_FLOAT,       ComponentType.FLOAT      ),
		R8_INT_NORM     (GlTexture.Format.R8_INT_NORM,      ComponentType.BYTE_NORM  ),
		R8_UINT_NORM    (GlTexture.Format.R8_UINT_NORM,     ComponentType.UBYTE_NORM ),
		R16_INT_NORM    (GlTexture.Format.R16_INT_NORM,     ComponentType.SHORT_NORM ),
		R16_UINT_NORM   (GlTexture.Format.R16_UINT_NORM,    ComponentType.USHORT_NORM),
		R32_FLOAT       (GlTexture.Format.R32_FLOAT,        ComponentType.FLOAT      ),
		// @formatter:on
		;

		public final int pixelStride;
		public final int subpixelStride;
		public final GlTexture.Format glFormat;
		public final ComponentType type;

		private ClientFormat(GlTexture.Format glFormat, ComponentType type) {
			this.pixelStride = type.byteSize * glFormat.components.componentCount;
			this.subpixelStride = type.byteSize;
			this.glFormat = glFormat;
			this.type = type;
		}

		public void putPixel(ByteBuffer buf, int i, float r, float g, float b, float a) {
			this.type.writeFloatToBuffer(buf, i + this.subpixelStride * 0, r);
			this.type.writeFloatToBuffer(buf, i + this.subpixelStride * 1, r);
			this.type.writeFloatToBuffer(buf, i + this.subpixelStride * 2, r);
			this.type.writeFloatToBuffer(buf, i + this.subpixelStride * 3, r);
		}
	}

	@Override
	public void close() {
		// if (this.data != null)
		// 	MemoryUtil.memFree(this.data);
		// this.data = null;
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
		this.data = ByteBuffer.allocateDirect(bufferSize);
		// this.data = MemoryUtil.memCalloc(bufferSize);
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

	public void uploadTo(GlTexture.Slice dst) {
		// TODO: assert that the current buffer format is convertible to the gl texture
		// format
		// GlStateManager._pixelStore(3314, 0);
        // GlStateManager._pixelStore(3316, 0);
        // GlStateManager._pixelStore(3315, 0);

		GL45C.glTextureSubImage1D(dst.texture.id, 0,
				0, dst.texture.size().width,
				this.format.glFormat.components.gl, this.format.type.gl, this.data);

		// dst.uploadImage(this.format.glFormat.components, this.format.type, data);
	}

	private <T extends GlTexture> T makeTexture(T value, Consumer<T> setup) {
		try {
			setup.accept(value);
			uploadTo(value.slice());
			// value.generateMipmaps();
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
