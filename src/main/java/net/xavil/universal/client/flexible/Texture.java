package net.xavil.universal.client.flexible;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.util.Disposable;

public abstract class Texture implements Disposable {
	protected final int textureBinding;
	protected final int glId;
	protected final boolean owned;

	protected Texture(int textureBinding, int glId, boolean owned) {
		this.textureBinding = textureBinding;
		this.glId = glId;
		this.owned = owned;
	}

	protected Texture(int textureBinding) {
		this.textureBinding = textureBinding;
		this.glId = TextureUtil.generateTextureId();
		this.owned = true;
		bindTexture(this.textureBinding, this.glId);
	}

	@Override
	public void dispose() {
		if (this.owned)
			TextureUtil.releaseTextureId(this.glId);
	}

	public int id() {
		return this.glId;
	}

	public void bind() {
		bindTexture(this.textureBinding, this.glId);
	}

	public static void bindTexture(int target, int id) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (id != GlStateManager.TEXTURES[GlStateManager.activeTexture].binding) {
			GlStateManager.TEXTURES[GlStateManager.activeTexture].binding = id;
			GL32.glBindTexture(target, id);
		}
	}

}
