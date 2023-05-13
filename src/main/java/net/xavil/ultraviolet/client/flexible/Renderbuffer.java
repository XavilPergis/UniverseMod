package net.xavil.ultraviolet.client.flexible;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.util.math.matrices.Vec2i;

public final class Renderbuffer extends GlObject {
	private int textureFormat = -1;
	private Vec2i size = Vec2i.ZERO;

	public Renderbuffer(int id, boolean owned) {
		super(id, owned);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.RENDERBUFFER;
	}

	@Override
	protected void release(int id) {
		GlStateManager._glDeleteRenderbuffers(this.id);
	}

	public Vec2i size() {
		return this.size;
	}

	public int id() {
		return this.id;
	}

	public void resize(int width, int height) {
		if (this.textureFormat < 0) {
			throw new IllegalStateException(
					"Cannot resize a renderbuffer whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width, height);
	}

	public void createStorage(int textureFormat, int width, int height) {
		final var maxTextureSize = RenderSystem.maxSupportedTextureSize();
		if (size.x > maxTextureSize || size.y > maxTextureSize) {
			throw new IllegalArgumentException("Maximum texture size is " + maxTextureSize
			+ ", but the requested size was (" + size.x + ", " + size.y + ")");
		}
		GlStateManager._glRenderbufferStorage(GL32.GL_RENDERBUFFER, textureFormat, width, height);
		this.textureFormat = textureFormat;
		this.size = new Vec2i(width, height);
	}

}
