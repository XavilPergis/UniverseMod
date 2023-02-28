package net.xavil.universal.mixin.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ResourceManagerReloadListener, AutoCloseable, GameRendererAccessor {

	private final Map<String, ShaderInstance> modShaders = new Object2ObjectOpenHashMap<>();
	private ResourceManager lastResourceManager = null;

	@Inject(method = "reloadShaders", at = @At("HEAD"))
	private void reloadShadersPre(ResourceManager resourceManager, CallbackInfo info) {
		this.lastResourceManager = resourceManager;
	}

	@Inject(method = "reloadShaders", at = @At("TAIL"))
	private void reloadShadersPost(ResourceManager resourceManager, CallbackInfo info) {
		this.lastResourceManager = null;
	}

	@Redirect(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithCapacity(I)Ljava/util/ArrayList;", remap = false))
	private ArrayList<Pair<ShaderInstance, Consumer<ShaderInstance>>> loadModShaders(int capacity) {

		final var shadersToApply = new ArrayList<Pair<ShaderInstance, Consumer<ShaderInstance>>>(capacity);

		try {
			ModRendering.LOAD_SHADERS_EVENT.invoker().register((name, vertexFormat) -> {
				var shader = new ShaderInstance(this.lastResourceManager, name, vertexFormat);
				shadersToApply.add(new Pair<>(shader, inst -> {
					final var prevShader = modShaders.put(name, inst);
					if (prevShader != null)
						prevShader.close();
				}));
			});
		} catch (IOException ex) {
			shadersToApply.forEach(pair -> pair.getFirst().close());
			throw new RuntimeException("could not reload shaders", ex);
		}

		return shadersToApply;
	}

	@Override
	public ShaderInstance universal_getShader(String name) {
		return this.modShaders.get(name);
	}

	@Shadow
	private double getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		throw new AssertionError("unreachable");
	}

	@Override
	public double universal_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		return this.getFov(activeRenderInfo, partialTicks, useFOVSetting);
	}

}
