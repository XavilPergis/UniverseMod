package net.xavil.universal.mixin.impl.render;

import java.io.IOException;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.xavil.universal.Mod;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.SkyRenderer;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;
import net.xavil.util.math.matrices.Mat4;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ResourceManagerReloadListener, AutoCloseable, GameRendererAccessor {

	private final Map<String, ShaderInstance> modShaders = new Object2ObjectOpenHashMap<>();
	private final Map<String, PostChain> modPostChains = new Object2ObjectOpenHashMap<>();

	private boolean applyViewBobTranslation = true;

	// @formatter:off
	@Shadow @Final private Map<String, ShaderInstance> shaders;
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

	@Inject(method = "resize", at = @At("HEAD"))
	private void onResize(int width, int height, CallbackInfo info) {
		this.modPostChains.values().forEach(chain -> chain.resize(width, height));
		SkyRenderer.INSTANCE.resize(width, height);
	}

	@Inject(method = "reloadShaders", at = @At("HEAD"))
	private void onReloadShaders(ResourceManager resourceManager, CallbackInfo info) {
		ModRendering.LOAD_SHADERS_EVENT.invoker().register((name, vertexFormat) -> {
			try {
				var shader = new ShaderInstance(resourceManager, name, vertexFormat);
				final var prevShader = modShaders.put(name, shader);
				if (prevShader != null)
					prevShader.close();
				Mod.LOGGER.info("loaded modded shader '{}'", name);
			} catch (IOException ex) {
				Mod.LOGGER.error("failed to load shader '{}'", name);
				ex.printStackTrace();
			}
		});

		ModRendering.LOAD_POST_PROCESS_SHADERS_EVENT.invoker().register((location) -> {
			try {
				var chain = new PostChain(this.minecraft.getTextureManager(), resourceManager,
						this.minecraft.getMainRenderTarget(), new ResourceLocation(location));
				chain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
				final var prevChain = this.modPostChains.put(location, chain);
				if (prevChain != null)
					prevChain.close();
				Mod.LOGGER.info("loaded modded post-process chain '{}'", location);
			} catch (IOException ex) {
				Mod.LOGGER.error("failed to load post chain '{}'", location);
				Mod.LOGGER.error("caused by: {}", ex);
			}
		});
	}

	@Override
	public ShaderInstance universal_getShader(String name) {
		return this.modShaders.get(name);
	}

	@Override
	public PostChain universal_getPostChain(String id) {
		return this.modPostChains.get(id);
	}

	@Shadow
	private double getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		throw new AssertionError("unreachable");
	}

	@Override
	public double universal_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		return this.getFov(activeRenderInfo, partialTicks, useFOVSetting);
	}

	@Redirect(method = "bobView", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
	private void modifyViewBobTranslation(PoseStack poses, double x, double y, double z) {
		if (this.applyViewBobTranslation)
			poses.translate(x, y, z);
	}

	// ok so. this is awful. i want to render very distant objects at a 1:1 scale,
	// which requires that i have a far clipping plane waaaaaay far away, which
	// means i need to make a custom projection matrix, but minecraft doesnt give
	// any particularly easy way to do that...

	@Override
	public Mat4 universal_makeProjectionMatrix(float near, float far, boolean applyViewBobTranslation,
			float partialTick) {
		final var projectionStack = new PoseStack();
		final var fov = this.getFov(this.mainCamera, partialTick, true);

		final var aspectRatio = (float) this.minecraft.getWindow().getWidth()
				/ (float) this.minecraft.getWindow().getHeight();
		if (this.zoom != 1f) {
			projectionStack.translate(this.zoomX, -this.zoomY, 0);
			projectionStack.scale(this.zoom, this.zoom, 1f);
		}
		final var projectionMatrix = Matrix4f.perspective(fov, aspectRatio, near, far);
		projectionStack.last().pose().multiply(projectionMatrix);

		this.bobHurt(projectionStack, partialTick);
		if (this.minecraft.options.bobView) {
			// cursed
			final var oldApplyViewBobTranslation = this.applyViewBobTranslation;
			this.applyViewBobTranslation = applyViewBobTranslation;
			this.bobView(projectionStack, partialTick);
			this.applyViewBobTranslation = oldApplyViewBobTranslation;
		}

		final var portalTime = Mth.lerp(partialTick, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
		final var effectScale = this.minecraft.options.screenEffectScale * this.minecraft.options.screenEffectScale;
		final var portalStrength = portalTime * effectScale;

		if (portalStrength > 0f) {
			final var nauseaModifier = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7f : 20f;
			final var scaleFactor = 5f / (portalStrength * portalStrength + 5f) - portalStrength * 0.04f;

			final var rotationAxis = new Vector3f(0f, Mth.SQRT_OF_TWO / 2f, Mth.SQRT_OF_TWO / 2f);
			final var rotationAngle = ((float) this.tick + partialTick) * nauseaModifier;

			projectionStack.mulPose(rotationAxis.rotationDegrees(rotationAngle));
			projectionStack.scale(1.0f / (scaleFactor * scaleFactor), 1.0f, 1.0f);
			projectionStack.mulPose(rotationAxis.rotationDegrees(-rotationAngle));
		}

		return Mat4.from(projectionStack.last().pose());
	}

}
