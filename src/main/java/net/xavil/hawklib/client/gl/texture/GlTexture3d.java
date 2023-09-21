package net.xavil.hawklib.client.gl.texture;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.client.gl.GlLimits;

public final class GlTexture3d extends GlTexture {

	public GlTexture3d(int glId, boolean owned) {
		super(Type.D3, glId, owned);
	}

	public GlTexture3d() {
		super(Type.D3);
	}

	public void resize(int width, int height, int depth) {
		if (this.textureFormat == null) {
			throw new IllegalStateException(
					debugDescription() + "Cannot resize a texture whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width, height, depth);
	}

	public void createStorage(GlTexture.Format textureFormat, int width, int height, int depth) {
		GlLimits.validateTextureSize(width);
		if (this.textureFormat == textureFormat
				&& this.size.width == width
				&& this.size.height == height
				&& this.size.depth == depth)
			return;

		GL45C.glTextureStorage3D(this.id, 1, textureFormat.id, width, height, depth);
		this.textureFormat = textureFormat;
		this.size = new Size(width, 1, 1, 1);
		this.storageAllocated = true;
	}

}
