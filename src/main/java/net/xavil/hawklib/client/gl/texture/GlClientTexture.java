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
	private int sizeX, sizeY, sizeZ;

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

	public void createStorage(int sizeX, int sizeY, int sizeZ) {
		final var bufferSize = 4 * sizeX * sizeY * sizeZ;
		if (bufferSize <= 0 || bufferSize > MAX_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"cannot create texture storage of size (%d, %d, %d) (%d bytes)",
					sizeX, sizeY, sizeZ, bufferSize));
		}

		if (this.data != null)
			this.close();
		this.data = MemoryUtil.memCalloc(bufferSize);
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	public static int packRgba(float r, float g, float b, float a) {
		int packed = 0;
		packed |= ((int) (255.0 * r)) << 0;
		packed |= ((int) (255.0 * g)) << 8;
		packed |= ((int) (255.0 * b)) << 16;
		packed |= ((int) (255.0 * a)) << 24;
		return packed;
	}

	public void clear(int rgba) {
		final var pixelCount = sizeX * sizeY * sizeZ;
		for (int i = 0; i < pixelCount; ++i)
			this.data.putInt(i * 4, rgba);
	}

	public void clear(float r, float g, float b, float a) {
		clear(packRgba(r, g, b, a));
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
		return 4 * (x + y * this.sizeX + z * this.sizeX * this.sizeY);
	}

	public void setPixel(int x, int y, int z, int rgba) {
		validateIndex(x, y, z);
		this.data.putInt(pixelIndex(x, y, z), rgba);
	}

	public void setPixel(int x, int y, int z, float r, float g, float b, float a) {
		setPixel(x, y, z, packRgba(r, g, b, a));
	}

	public void upload(GlTexture serverTexture, int layer) {
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
						GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, this.data);
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
						GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, this.data);
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
						GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, this.data);
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
