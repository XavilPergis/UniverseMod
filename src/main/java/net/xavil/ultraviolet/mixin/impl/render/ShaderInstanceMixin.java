package net.xavil.ultraviolet.mixin.impl.render;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

	@Redirect(method = "apply()V", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object aaa(Map<String, Object> map, Object key) {
		final var res = map.get(key);
		if (res instanceof GlTexture texture)
			texture.bind();
		return res;
	}

}
