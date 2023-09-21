package net.xavil.hawklib.client.gl.texture;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.client.gl.GlLimits;

public final class GlTexture1d extends GlTexture {

	public GlTexture1d(int glId, boolean owned) {
		super(Type.D1, glId, owned);
	}

	public GlTexture1d() {
		super(Type.D1);
	}

	public void resize(int width) {
		if (this.textureFormat == null) {
			throw new IllegalStateException(
					debugDescription() + "Cannot resize a texture whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width);
	}

	public void createStorage(GlTexture.Format textureFormat, int width) {
		GlLimits.validateTextureSize(width);
		if (this.textureFormat == textureFormat && this.size.width == width)
			return;

		GL45C.glTextureStorage1D(this.id, 1, textureFormat.id, width);
		this.textureFormat = textureFormat;
		this.size = new Size(width, 1, 1, 1);
		this.storageAllocated = true;
	}

}
