package net.xavil.universal.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import com.mojang.blaze3d.pipeline.TextureTarget;
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
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.screen.Color;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.RenderMatricesSnapshot;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.TicketedVolume;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class SkyRenderer {

	public static final SkyRenderer INSTANCE = new SkyRenderer();
	private static final double DISTANT_STAR_DISTANCE = 200;

	private final Minecraft client = Minecraft.getInstance();
	public TextureTarget skyTarget = null;

	private VertexBuffer distantStarsBuffer = new VertexBuffer();
	private boolean shouldRebuildStarBuffer = true;
	private SystemNodeId previousId = null;

	public void resize(int width, int height) {
		if (this.skyTarget != null) {
			this.skyTarget.resize(width, height, false);
		}
	}

	private void addBillboard(VertexConsumer builder, PoseStack poseStack, Vec3 selfPos, Vec3 pos, double s,
			StarSystemNode node) {

		var offset = pos.sub(selfPos);
		var forward = offset.normalize();

		var du = forward.dot(Vec3.YP);
		var df = forward.dot(Vec3.ZN);
		var v1 = Math.abs(du) < Math.abs(df) ? Vec3.YP : Vec3.ZN;
		var right = v1.cross(forward);
		// var rotation = random.nextDouble(0, Mth.TWO_PI);
		var rotation = 0;
		right = transformByQuaternion(axisAngle(forward, rotation), right);

		var color = Color.WHITE;
		if (node instanceof StarNode starNode)
			color = starNode.getColor();

		var distanceFromFocus = offset.length();
		if (distanceFromFocus > 1.5 * Galaxy.TM_PER_SECTOR)
			return;
		// var alpha = 1 - Mth.clamp(distanceFromFocus / (1.5 * Galaxy.TM_PER_SECTOR),
		// 0, 1);
		// if (alpha <= 0.05)
		// return;

		double alpha = 1;
		double k = 1;

		if (node instanceof StarNode starNode) {
			// Mth.inverseLerp(starNode.luminosityLsol, 0, 30000)
			var t = Math.pow(starNode.luminosityLsol, 0.2);
			var d = distanceFromFocus / Galaxy.TM_PER_SECTOR;
			alpha *= t / d;
			alpha /= 4;
			// alpha *= (starNode.luminosityLsol * 20) / (distanceFromFocus /
			// Galaxy.TM_PER_SECTOR);
			alpha = Math.min(1, alpha);
			// alpha = Math.max(0.1, alpha);

			// k = Mth.lerp(alpha, 0.1, 1);
		}

		// difference of 1.0 in magnitude corresponds to a brightness ratio of 100 5
		// {\displaystyle {\sqrt[{5}]{100}}}, or about 2.512. For example, a star of
		// magnitude 2.0 is 2.512 times as bright as a star of magnitude 3.0, 6.31 times
		// as bright as a star of magnitude 4.0, and 100 times as bright as one of
		// magnitude 7.0.

		// because of imperfections in optics, light from a point-like source is spread
		// out around the center point. because brighter stars emit so much more light,
		// the outer regions collect much more light than the dimmer stars, and as such,
		// appear larger. If you expose for brighter stars, you wont see dim stars, but
		// the spread will be much less prominent and will look much smaller.
		var up = forward.cross(right).neg();
		RenderHelper.addBillboard(builder, poseStack, up, right, forward.mul(DISTANT_STAR_DISTANCE),
				s * DISTANT_STAR_DISTANCE, 0, color.withA(alpha));
		RenderHelper.addBillboard(builder, poseStack, up, right, forward.mul(DISTANT_STAR_DISTANCE),
				0.5 * s * DISTANT_STAR_DISTANCE, 0, Color.WHITE.withA(alpha));

	}

	private void buildStars() {
		Mod.LOGGER.info("rebuilding background stars");
		var random = new Random(1035098490512L);
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

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		var selfPos = systemVolume.posById(currentSystemId.systemSector().sectorId());
		TicketedVolume.enumerateSectors(systemPos, Galaxy.TM_PER_SECTOR, Galaxy.TM_PER_SECTOR, sectorPos -> {
			var volume = galaxy.getVolumeAt(sectorPos);
			volume.enumerateInRadius(systemPos, Galaxy.TM_PER_SECTOR, element -> {
				if (element.id == currentId.system().systemSector().sectorId())
					return;
				var brightestStar = element.value.getInitial().stars.stream()
						.max(Comparator.comparing(star -> star.luminosityLsol)).get();
				addBillboard(builder, new PoseStack(), selfPos, element.pos, 0.02, brightestStar);
			});
			// volume.streamElements().forEach(element -> {
			// });
		});

		builder.end();
		distantStarsBuffer.upload(builder);
		shouldRebuildStarBuffer = false;
	}

	private void drawStars(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		// RenderSystem.disableCull();

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
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

	private void drawSystem(PoseStack poseStack, Camera camera, SystemNodeId currentPlanetId, double time,
			float partialTick) {

		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var system = universe.getSystem(currentPlanetId.system());

		var positions = new HashMap<Integer, Vec3>();
		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> positions.put(node.getId(), pos));

		var builder = Tesselator.getInstance().getBuilder();
		var ctx = new PlanetRenderingContext(builder);
		system.rootNode.visit(node -> {
			if (node instanceof StarNode starNode) {
				final var pos = positions.get(starNode.getId());
				var light = new PlanetRenderingContext.PointLight(pos.mul(1e12), starNode.getColor(),
						starNode.luminosityLsol);
				ctx.pointLights.add(light);
			}
		});

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		var thisPos = positions.get(currentPlanetId.nodeId());

		for (var entry : positions.entrySet()) {
			var node = system.rootNode.lookup(entry.getKey());
			var pos = entry.getValue();

			if (node.getId() == currentPlanetId.nodeId())
				continue;

			var offset = pos.sub(thisPos);
			var dir = offset.normalize();

			poseStack.pushPose();
			if (node instanceof PlanetNode planetNode) {
				ctx.render(planetNode, poseStack, offset.mul(1e12), 1, Color.WHITE);

				// RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
				// builder.begin(VertexFormat.Mode.QUADS,
				// DefaultVertexFormat.POSITION_COLOR_TEX);
				// var color = new Color(1, 0.5, 0.2, 1);
				// var up = Vec3.fromMinecraft(camera.getUpVector());
				// var right = Vec3.fromMinecraft(camera.getLeftVector()).neg();
				// RenderHelper.addBillboard(builder, poseStack, up, right, dir.mul(1000), 10,
				// 0, color);
				// builder.end();
				// this.client.getTextureManager()
				// .getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
				// .setFilter(true, false);
				// RenderSystem.setShaderTexture(0,
				// RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
				// RenderSystem.defaultBlendFunc();
				// BufferUploader.end(builder);

			} else if (node instanceof StarNode starNode) {
				ctx.renderStar(starNode, poseStack, offset.mul(1e12), 1, Color.WHITE);

				RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				var color = new Color(0.3, 1, 0.2, 1);
				var up = Vec3.fromMinecraft(camera.getUpVector());
				var right = Vec3.fromMinecraft(camera.getLeftVector()).neg();
				RenderHelper.addBillboard(builder, poseStack, up, right, dir.mul(1000), 10, 0, color);
				builder.end();
				this.client.getTextureManager()
						.getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
						.setFilter(true, false);
				RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
				RenderSystem.defaultBlendFunc();
				BufferUploader.end(builder);

			}
			poseStack.popPose();
		}
	}

	private static Quaternion axisAngle(Vec3 axis, double angle) {
		return new Vector3f((float) axis.x, (float) axis.y, (float) axis.z).rotation((float) angle);
	}

	private static Vec3 transformByQuaternion(Quaternion quat, Vec3 pos) {
		var vec = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
		vec.transform(quat);
		return Vec3.fromMinecraft(vec);
	}

	private void applyPlanetTrasform(PoseStack poseStack, StarSystemNode node, double time,
			double viewX, double viewZ) {

		var worldBorder = this.client.level.getWorldBorder();
		var tx = Mth.inverseLerp(viewX, worldBorder.getMinX(), worldBorder.getMaxX());
		var tz = Mth.inverseLerp(viewZ, worldBorder.getMinZ(), worldBorder.getMaxZ());

		final var hpi = Math.PI / 2;
		var latitudeOffset = Mth.clamp(Mth.lerp(tx, -hpi, hpi), -hpi, hpi);
		var longitudeOffset = Mth.clamp(Mth.lerp(tz, hpi, -hpi), -hpi, hpi);

		poseStack.mulPose(Vector3f.XP.rotation((float) (longitudeOffset + Math.toRadians(90))));
		poseStack.mulPose(Vector3f.YP.rotation((float) (latitudeOffset + node.rotationalSpeed * time)));
		poseStack.mulPose(Vector3f.XP.rotation((float) -node.obliquityAngle));
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
			boolean isSkyVisible) {

		var currentId = LevelAccessor.getUniverseId(this.client.level);
		if (isSkyVisible || currentId == null)
			return false;

		var matrixSnapshot = RenderMatricesSnapshot.capture();

		var fov = GameRendererAccessor.getFov(this.client.gameRenderer, partialTick);
		var proj = Matrix4f.perspective(fov,
				(float) this.client.getWindow().getWidth() / (float) this.client.getWindow().getHeight(),
				50, 1e12f);
		RenderSystem.setProjectionMatrix(proj);
		RenderSystem.setShaderFogStart(Float.POSITIVE_INFINITY);
		RenderSystem.setShaderFogEnd(Float.POSITIVE_INFINITY);
		this.client.gameRenderer.getProjectionMatrix(partialTick);

		final int width = this.client.getWindow().getWidth(), height = this.client.getWindow().getHeight();
		if (this.skyTarget == null) {
			this.skyTarget = new TextureTarget(width, height, true, false);
		}

		if (this.client.options.keySaveHotbarActivator.consumeClick()) {
			this.shouldRebuildStarBuffer = true;
		}
		if (this.previousId == null || !this.previousId.equals(currentId)) {
			this.shouldRebuildStarBuffer = true;
			this.previousId = currentId;
		}

		if (this.shouldRebuildStarBuffer)
			buildStars();

		this.skyTarget.setClearColor(0, 0, 0, 0);
		this.skyTarget.clear(false);
		this.skyTarget.bindWrite(false);

		// TODO: figure out how we actually wanna handle time
		// double time = 1e7 + (1440.0 / 5.0) * System.currentTimeMillis() / 1000.0 -
		// 1e10;
		// double time = (System.currentTimeMillis() % 1000000) / 1000f;
		double time = (System.currentTimeMillis() % 1000000) / 10f;
		// double time = (System.currentTimeMillis() % 1000000) * 1000f;

		poseStack.pushPose();
		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var planet = universe.getSystemNode(currentId);
		applyPlanetTrasform(poseStack, planet, time, camera.getPosition().x, camera.getPosition().z);
		drawStars(poseStack.last().pose(), projectionMatrix);
		drawSystem(poseStack, camera, currentId, time, partialTick);
		poseStack.popPose();

		this.client.getMainRenderTarget().bindWrite(false);
		RenderSystem.enableBlend();
		// RenderSystem.depthMask(false);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		this.skyTarget.blitToScreen(width, height, false);
		RenderSystem.disableBlend();

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.depthMask(true);

		matrixSnapshot.restore();
		return true;
	}

}
