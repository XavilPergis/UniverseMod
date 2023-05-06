package net.xavil.universal.client.flexible;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.util.math.matrices.Vec2i;

public final class Texture2d extends Texture {
	private Vec2i size;
	private int textureFormat;

	public Texture2d(int textureBinding, int glId, boolean owned) {
		super(textureBinding, glId, owned);
	}

	public Texture2d(int textureBinding) {
		super(textureBinding);
	}

	public void createStorage(int textureFormat, Vec2i size) {
		this.textureFormat = textureFormat;
		this.size = size;
		if (this.textureBinding == GL32.GL_TEXTURE_2D_MULTISAMPLE) {
			GL32.glTexImage2DMultisample(
					this.textureBinding, 4,
					this.textureFormat,
					this.size.x, this.size.y, true);
		} else {
			GlStateManager._texImage2D(
					this.textureBinding, 0,
					this.textureFormat,
					this.size.x, this.size.y, 0,
					// format + type are bogus values, and are ignored as we aren't actually doing
					// any data transfer here.
					GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, null);
		}
	}

}
