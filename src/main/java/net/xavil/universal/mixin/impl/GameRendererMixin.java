package net.xavil.universal.mixin.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ResourceManagerReloadListener, AutoCloseable, GameRendererAccessor {

	private final Map<String, ShaderInstance> modShaders = new Object2ObjectOpenHashMap<>();
	private ResourceManager lastResourceManager = null;

	// @formatter:off
	@Shadow @Final private Camera mainCamera;
	@Shadow @Final private Minecraft minecraft;
	@Shadow private int tick;
	@Shadow private float zoom;
	@Shadow private float zoomX;
	@Shadow private float zoomY;
	// @formatter:on

	@Shadow
	private void bobView(PoseStack matrixStack, float partialTicks) {
	}

	@Shadow
	private void bobHurt(PoseStack matrixStack, float partialTicks) {
	}

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

	// ok so. this is awful. i want to render very distant objects at a 1:1 scale,
	// which requires that i have a far clipping plane waaaaaay far away, which
	// means i need to make a custom projection matrix, but minecraft doesnt give
	// any particularly easy way to do that...

	@Override
	public Matrix4f universal_makeProjectionMatrix(float near, float far, float partialTick) {
		final var projectionStack = new PoseStack();
		double fov = this.getFov(this.mainCamera, partialTick, true);

		var aspectRatio = (float) this.minecraft.getWindow().getWidth()
				/ (float) this.minecraft.getWindow().getHeight();
		if (this.zoom != 1f) {
			projectionStack.translate(this.zoomX, -this.zoomY, 0);
			projectionStack.scale(this.zoom, this.zoom, 1f);
		}
		var projectionMatrix = Matrix4f.perspective(fov, aspectRatio, near, far);
		projectionStack.last().pose().multiply(projectionMatrix);

		this.bobHurt(projectionStack, partialTick);
		if (this.minecraft.options.bobView) {
			this.bobView(projectionStack, partialTick);
		}

		var portalTime = Mth.lerp(partialTick, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
		var effectScale = this.minecraft.options.screenEffectScale * this.minecraft.options.screenEffectScale;
		var portalStrength = portalTime * effectScale;

		if (portalStrength > 0f) {
			var nauseaModifier = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7f : 20f;
			var scaleFactor = 5f / (portalStrength * portalStrength + 5f) - portalStrength * 0.04f;

			var rotationAxis = new Vector3f(0f, Mth.SQRT_OF_TWO / 2f, Mth.SQRT_OF_TWO / 2f);
			var rotationAngle = ((float) this.tick + partialTick) * nauseaModifier;

			projectionStack.mulPose(rotationAxis.rotationDegrees(rotationAngle));
			projectionStack.scale(1.0f / (scaleFactor * scaleFactor), 1.0f, 1.0f);
			projectionStack.mulPose(rotationAxis.rotationDegrees(-rotationAngle));
		}

		return projectionStack.last().pose();
	}

}
