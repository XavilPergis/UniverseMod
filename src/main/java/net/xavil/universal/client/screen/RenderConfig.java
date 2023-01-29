package net.xavil.universal.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;

public class RenderConfig {
	public boolean readsDepth = true, writesDepth = true;

	public void apply() {
		RenderSystem.depthMask(this.writesDepth);
		if (this.readsDepth) {
			RenderSystem.enableDepthTest();
		} else {
			RenderSystem.disableDepthTest();
		}
	}
}
