package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.mixin.accessor.FontAccessor;

@Mixin(Font.class)
public abstract class FontMixin implements FontAccessor {

	@Override
	@Invoker("getFontSet")
	public abstract FontSet ultraviolet_getFontSet(ResourceLocation fontLocation);
	
}
