package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;

public interface FontAccessor {

	FontSet ultraviolet_getFontSet(ResourceLocation location);

	static FontSet getFontSet(Font font, ResourceLocation location) {
		return ((FontAccessor) font).ultraviolet_getFontSet(location);
	}

}
