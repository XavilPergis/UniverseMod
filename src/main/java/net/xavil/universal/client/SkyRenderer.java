package net.xavil.universal.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.client.screen.Color;
import net.xavil.universal.client.screen.GalaxyMapScreen;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.RenderMatricesSnapshot;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class SkyRenderer {

	public static final SkyRenderer INSTANCE = new SkyRenderer();
	private static final double DISTANT_STAR_DISTANCE = 500;

	private final Minecraft client = Minecraft.getInstance();
	public TextureTarget skyTarget = null;

	private VertexBuffer distantStarsBuffer = new VertexBuffer();
	private boolean shouldRebuildStarBuffer = true;

	public void resize(int width, int height) {
		if (this.skyTarget != null) {
			this.skyTarget.resize(width, height, false);
		}
	}

	private void buildStars() {
		var random = new Random(1035098490512L);
		var builder = Tesselator.getInstance().getBuilder();

		var currentId = LevelAccessor.getUniverseId(this.client.level);
		if (currentId == null)
			return;

		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var galaxyVolume = universe.getVolumeAt(currentId.galaxySector().sectorPos());
		var galaxy = galaxyVolume.getById(currentId.galaxySector().sectorId()).getFull();
		var systemVolume = galaxy.getVolumeAt(currentId.systemSector().sectorPos());
		var systemPos = systemVolume.posById(currentId.systemSector().sectorId());

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		var selfPos = systemVolume.posById(currentId.systemSector().sectorId());
		GalaxyMapScreen.enumerateSectors(systemPos, Galaxy.TM_PER_SECTOR, sectorPos -> {
			var volume = galaxy.getVolumeAt(sectorPos);
			for (var element : volume.elements) {
				if (element.id == currentId.systemSector().sectorId())
					continue;

				var offset = element.pos.subtract(selfPos);
				var forward = offset.normalize();

				var du = forward.dot(new Vec3(Vector3f.YP));
				var df = forward.dot(new Vec3(Vector3f.ZN));
				var v1 = Math.abs(du) < Math.abs(df) ? new Vec3(Vector3f.YP) : new Vec3(Vector3f.ZN);
				var right = v1.cross(forward);
				var rotation = random.nextDouble(0, Mth.TWO_PI);
				right = transformByQuaternion(axisAngle(forward, rotation), right);

				var initial = element.value.getInitial();
				var brightestStar = initial.stars.stream().max(Comparator.comparing(star -> star.luminosityLsol)).get();

				var color = brightestStar.getColor();

				double k = 0.02;

				var distanceFromFocus = offset.length();
				var alpha = 1 - Mth.clamp(distanceFromFocus / (2 * Galaxy.TM_PER_SECTOR), 0, 1);
				if (alpha <= 0.05)
					return;

				alpha *= brightestStar.luminosityLsol * 4;
				alpha = Math.min(1, alpha);

				var up = forward.cross(right).reverse();
				RenderHelper.addBillboard(builder, up, right, forward.scale(DISTANT_STAR_DISTANCE),
						k * DISTANT_STAR_DISTANCE, 0, color.withA(alpha));
				RenderHelper.addBillboard(builder, up, right, forward.scale(DISTANT_STAR_DISTANCE),
						0.5 * k * DISTANT_STAR_DISTANCE, 0, Color.WHITE.withA(alpha));

			}
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

	private void drawSystem(PoseStack poseStack, UniverseId currentPlanetId, double time, float partialTick) {

		var universe = MinecraftClientAccessor.getUniverse(this.client);
		var galaxyVolume = universe.getVolumeAt(currentPlanetId.galaxySector().sectorPos());
		var galaxy = galaxyVolume.getById(currentPlanetId.galaxySector().sectorId()).getFull();
		var systemVolume = galaxy.getVolumeAt(currentPlanetId.systemSector().sectorPos());
		var system = systemVolume.getById(currentPlanetId.systemSector().sectorId()).getFull();

		var positions = new HashMap<Integer, Vec3>();
		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> positions.put(node.getId(), pos));

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		var builder = Tesselator.getInstance().getBuilder();

		var thisPos = positions.get(currentPlanetId.systemNodeId());
		for (var entry : positions.entrySet()) {
			var node = system.rootNode.lookup(entry.getKey());
			var pos = entry.getValue();

			if (node.getId() == currentPlanetId.systemNodeId())
				continue;

			var offset = pos.subtract(thisPos);
			var dir = offset.normalize();

			var s = 0.1 / offset.length();
			s = Math.max(s, 0.5);

			poseStack.pushPose();
			if (node instanceof PlanetNode planetNode) {
				poseStack.translate(100 * dir.x, 100 * dir.y, 100 * dir.z);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees((float) time * 3));

				RenderHelper.renderPlanet(builder, planetNode, thisPos, s,
						poseStack, Vec3.ZERO, Color.WHITE);
			} else if (node instanceof StarNode starNode) {
				var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, 10000);
				poseStack.translate(100 * dir.x, 100 * dir.y, 100 * dir.z);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees((float) time * 3));

				RenderHelper.renderPlanet(builder, planetNode, thisPos, s,
						poseStack, Vec3.ZERO, Color.WHITE);
			}
			poseStack.popPose();
		}
	}

	private static Quaternion axisAngle(Vec3 axis, double angle) {
		return new Vector3f(axis).rotation((float) angle);
	}

	private static Vec3 transformByQuaternion(Quaternion quat, Vec3 pos) {
		var vec = new Vector3f(pos);
		vec.transform(quat);
		return new Vec3(vec);
	}

	private void applyPlanetTrasform(PoseStack poseStack, double time) {
		poseStack.mulPose(Vector3f.YP.rotationDegrees(10));
		poseStack.mulPose(Vector3f.ZP.rotationDegrees(0.8f * (float) time));
		poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
			boolean isSkyVisible) {

		var currentId = LevelAccessor.getUniverseId(this.client.level);
		if (isSkyVisible || currentId == null)
			return false;

		var matrixSnapshot = RenderMatricesSnapshot.capture();

		final int width = this.client.getWindow().getWidth(), height = this.client.getWindow().getHeight();
		if (this.skyTarget == null) {
			this.skyTarget = new TextureTarget(width, height, true, false);
		}

		if (this.shouldRebuildStarBuffer)
			buildStars();

		this.skyTarget.setClearColor(0, 0, 0, 0);
		this.skyTarget.clear(false);
		this.skyTarget.bindWrite(false);

		// TODO: figure out how we actually wanna handle time
		float time = (System.currentTimeMillis() % 1000000) / 10000f;

		RenderSystem.setShaderFogStart(10000);
		RenderSystem.setShaderFogEnd(10000);

		poseStack.pushPose();
		applyPlanetTrasform(poseStack, time);
		drawStars(poseStack.last().pose(), projectionMatrix);
		drawSystem(poseStack, currentId, time, partialTick);
		poseStack.popPose();

		if (this.client.options.keySaveHotbarActivator.consumeClick()) {
			shouldRebuildStarBuffer = true;
		}

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
