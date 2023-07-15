package net.xavil.hawklib.client.gl;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.math.matrices.Vec2i;

public final class GlRenderbuffer extends GlObject {
	private GlTexture.Format textureFormat = null;
	private Vec2i size = Vec2i.ZERO;

	public GlRenderbuffer(int id, boolean owned) {
		super(id, owned);
	}

	public GlRenderbuffer() {
		super(GlManager.createRenderbuffer(), true);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.RENDERBUFFER;
	}

	public Vec2i size() {
		return this.size;
	}

	public GlTexture.Format format() {
		return this.textureFormat;
	}

	public void resize(int width, int height) {
		if (this.textureFormat == null) {
			throw new IllegalStateException(
					"Cannot resize a renderbuffer whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width, height);
	}

	public void createStorage(GlTexture.Format textureFormat, int width, int height) {
		GlLimits.validateRenderbufferSize(width, height);
		GlManager.bindRenderbuffer(this.id);
		GlStateManager._glRenderbufferStorage(GL32C.GL_RENDERBUFFER, textureFormat.id, width, height);
		this.textureFormat = textureFormat;
		this.size = new Vec2i(width, height);
	}

}
