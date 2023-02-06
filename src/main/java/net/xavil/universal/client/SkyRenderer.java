package net.xavil.universal.client;

import java.util.HashMap;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
		var galaxyVolume = universe.getOrGenerateGalaxyVolume(currentId.galaxySector().sectorPos());
		var galaxy = galaxyVolume.getById(currentId.galaxySector().sectorId()).getFull();
		var systemVolume = galaxy.getOrGenerateVolume(currentId.systemSector().sectorPos());
		var system = systemVolume.getById(currentId.systemSector().sectorId()).getFull();

		// TODO: figure out how we actually wanna handle time
		float time = (System.currentTimeMillis() % 1000000) / 1000f;

		var positions = new HashMap<Integer, Vec3>();
		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> positions.put(node.getId(), pos));

		RenderSystem.setShaderFogStart(10000);
		RenderSystem.setShaderFogEnd(10000);

		var thisPos = positions.get(currentId.systemNodeId());
		for (var entry : positions.entrySet()) {
			var node = system.rootNode.lookup(entry.getKey());
			var pos = entry.getValue();

			var offset = pos.subtract(thisPos);
			if (node instanceof PlanetNode planetNode) {
				poseStack.pushPose();
				poseStack.mulPose(Vector3f.YP.rotationDegrees(10));
				poseStack.mulPose(Vector3f.ZP.rotationDegrees(0.8f * time));
				poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
				poseStack.translate(1000 * offset.x, 1000 * offset.y, 1000 * offset.z);
				// poseStack.translate(400, 0, 0);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 3));

				RenderHelper.renderPlanet(Tesselator.getInstance().getBuilder(), planetNode, 0.04, poseStack, Vec3.ZERO,
						Color.WHITE);

				poseStack.popPose();
			} else if (node instanceof StarNode starNode) {
				var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, 10000);
				poseStack.pushPose();
				poseStack.mulPose(Vector3f.YP.rotationDegrees(10));
				poseStack.mulPose(Vector3f.ZP.rotationDegrees(0.8f * time));
				poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
				poseStack.translate(100 * offset.x, 100 * offset.y, 100 * offset.z);
				// poseStack.translate(400, 0, 0);
				poseStack.mulPose(Vector3f.XP.rotationDegrees(20));
				poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 3));

				RenderHelper.renderPlanet(Tesselator.getInstance().getBuilder(), planetNode, 0.4, poseStack, Vec3.ZERO,
						Color.WHITE);

				poseStack.popPose();
			}
		}

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
