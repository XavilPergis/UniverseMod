package net.xavil.universal.client;

import java.util.HashMap;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.client.screen.Color;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class SkyRenderer {

	private static final Minecraft CLIENT = Minecraft.getInstance();
	public static TextureTarget SKY_TARGET = null;

	public static void resize(int width, int height) {
		if (SKY_TARGET != null) {
			SKY_TARGET.resize(width, height, false);
		}
	}

	public static boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
			boolean isSkyVisible) {

		final int width = CLIENT.getWindow().getWidth(), height = CLIENT.getWindow().getHeight();
		if (SKY_TARGET == null) {
			SKY_TARGET = new TextureTarget(width, height, true, false);
		}

		var currentId = LevelAccessor.getUniverseId(CLIENT.level);
		if (currentId == null)
			return false;

		SKY_TARGET.setClearColor(0, 0, 0, 0);
		SKY_TARGET.clear(false);
		SKY_TARGET.bindWrite(false);

		var universe = MinecraftClientAccessor.getUniverse(CLIENT);
		var galaxyVolume = universe.getVolumeAt(currentId.galaxySector().sectorPos());
		var galaxy = galaxyVolume.getById(currentId.galaxySector().sectorId()).getFull();
		var systemVolume = galaxy.getVolumeAt(currentId.systemSector().sectorPos());
		var system = systemVolume.getById(currentId.systemSector().sectorId()).getFull();

		// TODO: figure out how we actually wanna handle time
		float time = (System.currentTimeMillis() % 1000000) / 1000f;

		var positions = new HashMap<Integer, Vec3>();
		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> positions.put(node.getId(), pos));

		RenderSystem.setShaderFogStart(10000);
		RenderSystem.setShaderFogEnd(10000);

		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();

		poseStack.pushPose();
		poseStack.mulPose(Vector3f.YP.rotationDegrees(10));
		poseStack.mulPose(Vector3f.ZP.rotationDegrees(0.8f * time));
		poseStack.mulPose(Vector3f.XP.rotationDegrees(90));

		var builder = Tesselator.getInstance().getBuilder();

		var systemSectorPos = systemVolume.posById(currentId.systemSector().sectorId());
		int count = 0;
		for (var element : systemVolume.elements) {
			if (element.id == currentId.systemSector().sectorId())
				continue;

			var offset = element.pos.subtract(systemSectorPos);
			var dir = offset.normalize();

			poseStack.pushPose();

			var planetNode = new PlanetNode(PlanetNode.Type.WATER_WORLD, 10000);
			poseStack.translate(100 * dir.x, 100 * dir.y, 100 * dir.z);
			poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
			poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 3));

			RenderHelper.renderPlanet(builder, planetNode, systemSectorPos.scale(0.000000004), 0.5,
					poseStack, Vec3.ZERO, Color.WHITE);

			poseStack.popPose();

			// aaa.pos
			count += 1;
			if (count >= 200)
				break;
		}

		var thisPos = positions.get(currentId.systemNodeId());
		for (var entry : positions.entrySet()) {
			var node = system.rootNode.lookup(entry.getKey());
			var pos = entry.getValue();

			if (node.getId() == currentId.systemNodeId())
				continue;

			var offset = pos.subtract(thisPos);
			var dir = offset.normalize();

			var s = 0.1 / offset.length();
			s = Math.max(s, 0.5);

			poseStack.pushPose();
			if (node instanceof PlanetNode planetNode) {
				poseStack.translate(100 * dir.x, 100 * dir.y, 100 * dir.z);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 3));

				RenderHelper.renderPlanet(builder, planetNode, thisPos, s,
						poseStack, Vec3.ZERO, Color.WHITE);
			} else if (node instanceof StarNode starNode) {
				var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, 10000);
				poseStack.translate(100 * dir.x, 100 * dir.y, 100 * dir.z);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 3));

				RenderHelper.renderPlanet(builder, planetNode, thisPos, s,
						poseStack, Vec3.ZERO, Color.WHITE);
			}
			poseStack.popPose();
		}

		poseStack.popPose();

		if (CLIENT.options.keySaveHotbarActivator.consumeClick()) {
			Screenshot.grab(CLIENT.gameDirectory, SKY_TARGET,
					component -> CLIENT.execute(() -> CLIENT.gui.getChat().addMessage(component)));
		}

		// CLIENT.getMainRenderTarget().clear(false);
		CLIENT.getMainRenderTarget().bindWrite(false);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		SKY_TARGET.blitToScreen(width, height, false);
		RenderSystem.disableBlend();

		return true;
	}

}
