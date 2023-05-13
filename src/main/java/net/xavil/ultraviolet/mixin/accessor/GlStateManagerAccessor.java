package net.xavil.ultraviolet.mixin.accessor;

public interface GlStateManagerAccessor {
	void ultraviolet_bindTexture(int target, int id);

	static void bindTexture(int target, int id) {
	}
}
