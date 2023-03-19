package net.xavil.universal.mixin.accessor;

public interface GlStateManagerAccessor {
	void universal_bindTexture(int target, int id);

	static void bindTexture(int target, int id) {
	}
}
