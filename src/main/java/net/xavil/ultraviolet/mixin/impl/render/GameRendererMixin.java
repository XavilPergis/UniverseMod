package net.xavil.ultraviolet.mixin.impl.render;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.SkyRenderer;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.shader.ShaderLoader;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.matrices.Mat4;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ResourceManagerReloadListener, AutoCloseable, GameRendererAccessor {

	private final Map<ResourceLocation, ShaderProgram> modShaders = new Object2ObjectOpenHashMap<>();
	private final Map<String, ShaderProgram> vanillaShaderProxies = new Object2ObjectOpenHashMap<>();

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
		SkyRenderer.INSTANCE.resize(width, height);
	}

	@Inject(method = "reloadShaders", at = @At("HEAD"))
	private void onReloadShaders(ResourceManager resourceManager, CallbackInfo info) {
		HawkRendering.LOAD_SHADERS_EVENT.invoker().register(new HawkRendering.ShaderSink() {

			@Override
			public void accept(ResourceLocation name, VertexFormat vertexFormat, GlFragmentWrites fragmentWrites, Iterator<String> shaderDefines) {
				try {
					final var shader = ShaderLoader.load(resourceManager, name, vertexFormat, fragmentWrites, shaderDefines);
					shader.setDebugName(name.toString());
					final var prevShader = modShaders.put(name, shader);
					if (prevShader != null)
						prevShader.close();
					Mod.LOGGER.debug("loaded modded shader '{}'", name);
				} catch (ShaderLoader.ShaderLoadException ex) {
					Mod.LOGGER.error("failed to load modded shader '{}':\n{}", name, ex.getMessage());
					if (ex.getCause() != null) {
						Mod.LOGGER.error("caused by: {}", ex.getCause().toString());
					}
				}
			}

		});
	}

	@Inject(method = "reloadShaders", at = @At("TAIL"))
	private void onReloadShaders2(ResourceManager resourceManager, CallbackInfo info) {
		this.shaders.forEach((name, shader) -> {
			this.vanillaShaderProxies.put(name, new ShaderProgram(shader));
		});
	}

	@Inject(method = "shutdownShaders()V", at = @At("TAIL"))
	private void onShutdownShaders(CallbackInfo info) {
		this.vanillaShaderProxies.clear();
	}
	
	@Inject(method = "close()V", at = @At("TAIL"))
	private void onClose(CallbackInfo info) {
		this.modShaders.values().forEach(shader -> {
			shader.close();
		});
		this.modShaders.clear();
	}

	@Override
	public ShaderProgram ultraviolet_getShader(ResourceLocation name) {
		return this.modShaders.get(name);
	}

	@Override
	public ShaderProgram ultraviolet_getVanillaShader(String id) {
		return this.vanillaShaderProxies.get(id);
	}

	@Shadow
	private double getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		throw new AssertionError("unreachable");
	}

	@Override
	public double ultraviolet_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
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
	public Mat4 ultraviolet_makeProjectionMatrix(float near, float far, boolean applyViewBobTranslation,
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

		final var portalTime = Mth.lerp(partialTick, this.minecraft.player.oPortalTime,
				this.minecraft.player.portalTime);
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
