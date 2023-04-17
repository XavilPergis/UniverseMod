package net.xavil.universal.client.sky;

import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.universal.Mod;
import net.xavil.universal.client.GalaxyRenderingContext;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.PlanetRenderingContext;
import net.xavil.universal.client.PlanetRenderingContext.PointLight;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleRenderTarget;
import net.xavil.universal.client.flexible.FlexibleVertexConsumer;
import net.xavil.universal.client.screen.CachedCamera;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public class NewSkyRenderDispatcher {

	public static final NewSkyRenderDispatcher INSTANCE = new NewSkyRenderDispatcher();

	private final Minecraft client = Minecraft.getInstance();

	public FlexibleRenderTarget skyTarget = null;
	public boolean useMultisampling = false;
	// public PostChain compositeChain = null;

	private Location previousLocation = null;
	private GalaxyRenderingContext galaxyRenderingContext = null;
	private long galaxyRenderingSeed = 314159265358979L; // :p

	public void resize(int width, int height) {
		if (this.skyTarget != null) {
			this.skyTarget.resize(width, height, false);
		}
	}

	public void setMultisampled(boolean useMultisampling) {
		if (this.useMultisampling != useMultisampling) {
			this.useMultisampling = useMultisampling;
			this.skyTarget = null;
		}
	}

	// public FlexibleRenderTarget getSkyTarget() {
	// if (this.skyTarget == null) {
	// final var window = this.client.getWindow();
	// final var format = new FlexibleRenderTarget.FormatPair(this.useMultisampling,
	// GL32.GL_RGBA32F,
	// OptionalInt.of(GL32.GL_DEPTH_COMPONENT32));
	// this.skyTarget = new FlexibleRenderTarget(window.getWidth(),
	// window.getHeight(), format);
	// }
	// return this.skyTarget;
	// }

	public RenderTarget getSkyCompositeTarget() {
		return ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).getTempTarget("sky");
	}

	private void drawSystem(CachedCamera<?> camera, Vec3 pos, SystemNodeId currentNodeId, double time) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);

		profiler.push("camera");
		camera.setupRenderMatrices();

		profiler.popPush("system_lookup");
		final var system = universe.getSystem(currentNodeId.system()).unwrapOrNull();
		if (system != null) {
			profiler.popPush("planet_context_setup");
			final var builder = BufferRenderer.immediateBuilder();
			final var ctx = new PlanetRenderingContext(time);
			system.rootNode.visit(node -> {
				if (node instanceof StellarCelestialNode starNode) {
					var light = PlanetRenderingContext.PointLight.fromStar(starNode);
					ctx.pointLights.add(light);
				}
			});

			profiler.popPush("visit");
			system.rootNode.visit(node -> {
				final var profiler2 = Minecraft.getInstance().getProfiler();
				profiler2.push("id:" + node.getId());
				ctx.render(builder, camera, node, new PoseStack(), Color.WHITE,
						node.getId() == currentNodeId.nodeId());
				profiler2.pop();
			});
		}
		profiler.pop();
	}

	private CachedCamera<?> createCamera(Camera camera, Vec3 celestialPos, float partialTick) {
		final var px = Vec3.fromMinecraft(camera.getLeftVector()).neg();
		final var py = Vec3.fromMinecraft(camera.getUpVector());
		final var pz = px.cross(py);
		final var quat = Quat.fromOrthonormalBasis(px, py, pz);

		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer, 0.01f, 1e6f, false, partialTick);

		return CachedCamera.create(camera, celestialPos, quat, 1e12, 1e12, proj);
	}

	private void drawSky(Camera srcCamera, RenderTarget target, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);

		target.bindWrite(false);
		target.setClearColor(0, 0, 0, 0);
		target.clear(false);

		final var pos = Vec3.ZERO;

		final var camera = createCamera(srcCamera, Vec3.ZERO, partialTick);

		// profiler.push("galaxy");
		// drawGalaxyFromVertexBuffer(RenderSystem.getModelViewMatrix(),
		// starSphereProj);
		// profiler.popPush("stars");
		// drawStarsFromVertexBuffer(RenderSystem.getModelViewMatrix(), starSphereProj);
		profiler.push("system");
		// drawSystem(camera, pos, nodeId, time);
		profiler.pop();
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
			boolean isSkyVisible) {
		final var profiler = Minecraft.getInstance().getProfiler();

		final var compositeTarget = getSkyCompositeTarget();
		if (this.useMultisampling) {
			if (this.skyTarget == null) {
				final var window = this.client.getWindow();
				final var format = new FlexibleRenderTarget.FormatPair(this.useMultisampling, GL32.GL_RGBA32F,
						OptionalInt.of(GL32.GL_DEPTH_COMPONENT32));
				this.skyTarget = new FlexibleRenderTarget(window.getWidth(), window.getHeight(), format);
			}
			drawSky(camera, this.skyTarget, partialTick);
			profiler.push("resolve");
			this.skyTarget.resolveTo(compositeTarget);
			profiler.pop();
		} else {
			drawSky(camera, compositeTarget, partialTick);
		}

		final var mainTarget = this.client.getMainRenderTarget();

		profiler.push("composite");
		mainTarget.setClearColor(0f, 0f, 0f, 1.0f);
		ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).process(partialTick);
		mainTarget.bindWrite(false);
		profiler.pop();

		// a bit scuffed...
		GlStateManager._clearDepth(1.0);
		GlStateManager._clear(GL32.GL_DEPTH_BUFFER_BIT, false);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();

		return true;
	}

}
