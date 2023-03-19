package net.xavil.universal.client;

import java.util.OptionalInt;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleRenderTarget;
import net.xavil.universal.client.screen.CachedCamera;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.TicketedVolume;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public class SkyRenderer {

	public static final SkyRenderer INSTANCE = new SkyRenderer();

	private final Minecraft client = Minecraft.getInstance();
	public FlexibleRenderTarget skyTargetMultisampled = null;
	// public PostChain compositeChain = null;

	private VertexBuffer distantStarsBuffer = new VertexBuffer();
	private boolean shouldRebuildStarBuffer = true;
	private SystemNodeId previousId = null;

	public void resize(int width, int height) {
		if (this.skyTargetMultisampled != null) {
			this.skyTargetMultisampled.resize(width, height, false);
		}
	}

	public FlexibleRenderTarget getSkyTargetMultisampled() {
		if (this.skyTargetMultisampled == null) {
			final var window = this.client.getWindow();
			final var format = new FlexibleRenderTarget.FormatPair(true, GL32.GL_RGBA32F,
					OptionalInt.of(GL32.GL_DEPTH_COMPONENT32));
			this.skyTargetMultisampled = new FlexibleRenderTarget(window.getWidth(), window.getHeight(), format);
		}
		return this.skyTargetMultisampled;
	}

	public RenderTarget getSkyResolveTarget() {
		return ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).getTempTarget("sky");
	}

	private void addBillboard(VertexConsumer builder, PoseStack poseStack, Vec3 selfPos, Vec3 pos, double s,
			CelestialNode node) {

		var offset = pos.sub(selfPos);
		var forward = offset.normalize();

		s = 1.5e9 * RenderHelper.getCelestialBodySize(selfPos, node, pos);

		var du = forward.dot(Vec3.YP);
		var df = forward.dot(Vec3.ZN);
		var v1 = Math.abs(du) < Math.abs(df) ? Vec3.YP : Vec3.ZN;
		var right = v1.cross(forward);
		var rotation = 0;
		right = Quat.axisAngle(forward, rotation).transform(right);

		var color = Color.WHITE;
		if (node instanceof StellarCelestialNode starNode) {
			color = starNode.getColor();
		}

		var distanceFromFocus = offset.length();
		if (distanceFromFocus > 1.5 * Galaxy.TM_PER_SECTOR)
			return;

		double alpha = 0.0;
		if (node instanceof StellarCelestialNode starNode) {
			// how many Tm can a star with 1 Lsol of luminosity be seen from
			// final double referenceMaxVisibleDistance = 547229.011;

			final double lumW = starNode.luminosityLsol * Units.W_PER_Lsol;
			final double distM = distanceFromFocus * Units.TERA;
			final double intensity = lumW / (4.0 * Math.PI * distM * distM);

			alpha += 1e10 * intensity;
			alpha = Math.min(1.0, alpha);
		}

		if (alpha < 1e-5)
			return;

		s = 0.5f;

		// because of imperfections in optics, light from a point-like source is spread
		// out around the center point. because brighter stars emit so much more light,
		// the outer regions collect much more light than the dimmer stars, and as such,
		// appear larger. If you expose for brighter stars, you wont see dim stars, but
		// the spread will be much less prominent and will look much smaller.

		var up = forward.cross(right).neg();
		RenderHelper.addBillboardCamspace(builder, poseStack, up, right, forward.mul(100),
				s, 0, color.withA(alpha));

	}

	private void buildStars() {
		Mod.LOGGER.info("rebuilding background stars");
		var builder = Tesselator.getInstance().getBuilder();

		var currentId = LevelAccessor.getUniverseId(this.client.level);
		if (currentId == null)
			return;

		var currentSystemId = currentId.system();
		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var galaxyVolume = universe.getVolumeAt(currentSystemId.galaxySector().sectorPos());
		var galaxy = galaxyVolume
				.getById(currentSystemId.galaxySector().sectorId())
				.getFull();
		var systemVolume = galaxy.getVolumeAt(currentSystemId.systemSector().sectorPos());
		var systemPos = systemVolume.posById(currentSystemId.systemSector().sectorId());
		if (systemPos == null) {
			Mod.LOGGER.error("could not build background stars because the system pos was null.");
			return;
		}

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		var selfPos = systemVolume.posById(currentSystemId.systemSector().sectorId());
		TicketedVolume.enumerateSectors(systemPos, Galaxy.TM_PER_SECTOR, Galaxy.TM_PER_SECTOR, sectorPos -> {
			var volume = galaxy.getVolumeAt(sectorPos);
			volume.enumerateInRadius(systemPos, Galaxy.TM_PER_SECTOR, element -> {
				if (element.id == currentId.system().systemSector().sectorId())
					return;
				var displayStar = element.value.getInitial().primaryStar;
				addBillboard(builder, new PoseStack(), selfPos, element.pos, 1, displayStar);
			});
			// volume.streamElements().forEach(element -> {
			// });
		});

		builder.end();
		distantStarsBuffer.upload(builder);
		shouldRebuildStarBuffer = false;
		VertexBuffer.unbind();
	}

	private void drawStars(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		// RenderSystem.disableCull();

		RenderSystem.setShader(() -> ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));
		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();

		var shader = RenderSystem.getShader();
		for (int j = 0; j < 8; ++j) {
			int m = RenderSystem.getShaderTexture(j);
			shader.setSampler("Sampler" + j, m);
		}
		if (shader.MODEL_VIEW_MATRIX != null) {
			shader.MODEL_VIEW_MATRIX.set(modelViewMatrix);
		}
		if (shader.PROJECTION_MATRIX != null) {
			shader.PROJECTION_MATRIX.set(projectionMatrix);
		}
		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		}
		if (shader.COLOR_MODULATOR != null) {
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}
		if (shader.FOG_START != null) {
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		}
		if (shader.FOG_END != null) {
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		}
		if (shader.FOG_COLOR != null) {
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		}
		if (shader.FOG_SHAPE != null) {
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		}
		if (shader.TEXTURE_MATRIX != null) {
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}
		if (shader.GAME_TIME != null) {
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}
		if (shader.SCREEN_SIZE != null) {
			var window = Minecraft.getInstance().getWindow();
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		}
		RenderSystem.setupShaderLights(shader);
		shader.apply();

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		this.distantStarsBuffer.drawChunkLayer();
	}

	private void drawSystem(CachedCamera<?> camera, SystemNodeId currentNodeId, double time, float partialTick) {

		camera.setupRenderMatrices();

		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var system = universe.getSystem(currentNodeId.system());
		var currentNode = system.rootNode.lookup(currentNodeId.nodeId());

		var builder = Tesselator.getInstance().getBuilder();
		var ctx = new PlanetRenderingContext(time);
		system.rootNode.visit(node -> {
			if (node instanceof StellarCelestialNode starNode) {
				var light = PlanetRenderingContext.PointLight.fromStar(starNode);
				ctx.pointLights.add(light);
			}
		});

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();

		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		system.rootNode.visit(node -> {
			if (node instanceof StellarCelestialNode starNode) {
				RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				addBillboard(builder, new PoseStack(), currentNode.position, starNode.position, 1, starNode);
				builder.end();
				this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
				RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
				RenderSystem.enableBlend();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
				RenderSystem.depthMask(false);
				RenderSystem.enableDepthTest();
				RenderSystem.disableCull();
				BufferUploader.end(builder);
			} else {
				final var flexBuilder = BufferRenderer.immediateBuilder();
				ctx.render(flexBuilder, camera, node, new PoseStack(), Color.WHITE,
						node.getId() == currentNodeId.nodeId());
			}
		});
	}

	private Quat toCelestialWorldSpaceRotation(CelestialNode node, double time, double viewX, double viewZ) {
		var worldBorder = this.client.level.getWorldBorder();
		var tx = Mth.inverseLerp(viewX, worldBorder.getMinX(), worldBorder.getMaxX());
		var tz = Mth.inverseLerp(viewZ, worldBorder.getMinZ(), worldBorder.getMaxZ());

		final var hpi = Math.PI / 2;
		var latitudeOffset = Mth.clamp(Mth.lerp(tx, -hpi, hpi), -hpi, hpi);
		var longitudeOffset = Mth.clamp(Mth.lerp(tz, hpi, -hpi), -hpi, hpi);

		var rotationalSpeed = 2 * Math.PI / node.rotationalPeriod;

		var a = Quat.axisAngle(Vec3.XP, longitudeOffset + Math.toRadians(90));
		var b = Quat.axisAngle(Vec3.YP, latitudeOffset + rotationalSpeed * time);
		var c = Quat.axisAngle(Vec3.XP, -node.obliquityAngle);

		return a.hamiltonProduct(b).hamiltonProduct(c);
	}

	private Vec3 getPlanetSurfaceOffset(PlanetaryCelestialNode node, double time, double viewX, double viewZ) {
		var planetViewRotation = toCelestialWorldSpaceRotation(node, time, viewX, viewZ);
		var upCelestialWorld = planetViewRotation.transform(Vec3.YP);
		return upCelestialWorld.mul(node.radiusRearth * (Units.m_PER_Rearth / Units.TERA));
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
			boolean isSkyVisible) {

		var currentId = LevelAccessor.getUniverseId(this.client.level);
		if (isSkyVisible || currentId == null)
			return false;

		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		var currentNodeUntyped = universe.getSystemNode(currentId);
		if (currentNodeUntyped == null)
			return false;

		if (!(currentNodeUntyped instanceof PlanetaryCelestialNode))
			return false;
		var currentNode = (PlanetaryCelestialNode) currentNodeUntyped;

		double time = universe.getCelestialTime(partialTick);

		var currentSystem = universe.getSystem(currentId.system());
		currentSystem.rootNode.updatePositions(time);

		var systemProj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer, 5e4f, 1e13f, partialTick);
		var starSphereProj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer, 1f, 1e5f, partialTick);

		// TODO: is this right? what about when spectating entities? (or in front-view
		// third-person lol)
		// this whole thing feels kinda brittle tbh, since we reconstruct the projection
		// matrix and view rotations from scratch ourselves. This means they can get out
		// of sync with the real things, which other mods might modify!
		var xRot = this.client.player.getViewXRot(partialTick);
		var yRot = this.client.player.getViewYRot(partialTick);

		var planetViewRotation = toCelestialWorldSpaceRotation(currentNode, time, camera.getPosition().x,
				camera.getPosition().z);
		var planetSurfaceOffset = getPlanetSurfaceOffset(currentNode, time, camera.getPosition().x,
				camera.getPosition().z);

		var celestialCamera = CachedCamera.create(camera, currentNode.position.mul(Units.TERA).add(planetSurfaceOffset),
				xRot, yRot, planetViewRotation, systemProj);
		var matrixSnapshot = celestialCamera.setupRenderMatrices();
		poseStack.pushPose();

		RenderSystem.setProjectionMatrix(systemProj);
		RenderSystem.setShaderFogStart(Float.POSITIVE_INFINITY);
		RenderSystem.setShaderFogEnd(Float.POSITIVE_INFINITY);

		if (this.client.options.keySaveHotbarActivator.consumeClick()) {
			this.shouldRebuildStarBuffer = true;
		}
		if (this.previousId == null || !this.previousId.equals(currentId)) {
			this.shouldRebuildStarBuffer = true;
			this.previousId = currentId;
		}

		if (this.shouldRebuildStarBuffer)
			buildStars();

		getSkyResolveTarget().clear(false);

		GL32.glEnable(GL32.GL_MULTISAMPLE);
		final var skyTargetMs = getSkyTargetMultisampled();
		skyTargetMs.setClearColor(0, 0, 0, 0);
		skyTargetMs.clear(false);
		skyTargetMs.bindWrite(false);

		drawStars(RenderSystem.getModelViewMatrix(), starSphereProj);
		drawSystem(celestialCamera, currentId, time, partialTick);

		// resolve nice and crispy multisampled framebuffer to a normal (but floating
		// point backed) framebuffer
		skyTargetMs.resolveTo(getSkyResolveTarget());

		this.client.getMainRenderTarget().setClearColor(0f, 0f, 0f, 1.0f);
		ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).process(partialTick);
		this.client.getMainRenderTarget().bindWrite(false);
		// a bit scuffed...
		GlStateManager._clearDepth(1.0);
		GlStateManager._clear(GL32.GL_DEPTH_BUFFER_BIT, false);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();

		poseStack.popPose();
		matrixSnapshot.restore();

		return true;
	}

}
