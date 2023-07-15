package net.xavil.ultraviolet.client.screen.debug;

import java.util.Random;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.ultraviolet.common.universe.Octree;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.hawklib.math.matrices.Vec3;

public class GalaxyDensityDebugScreen extends HawkScreen3d {

	private Octree<Double> galaxyPoints = null;
	private final Galaxy galaxy;

	public GalaxyDensityDebugScreen(Screen previousScreen, Galaxy galaxy) {
		super(new TranslatableComponent("narrator.screen.debug.galaxy_density"), previousScreen,
				new OrbitCamera(1e12), 3e3, 1e9);
		this.galaxy = galaxy;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		if (keyCode == GLFW.GLFW_KEY_O && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT)) != 0) {
			buildGalaxyPoints(false);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_R) {
			buildGalaxyPoints(true);
			return true;
		}

		return false;
	}

	@Override
	public OrbitCamera.Cached setupCamera(CameraConfig config, float partialTick) {
		return this.camera.cached(config, partialTick);
	}

	private static double aabbVolume(Vec3 a, Vec3 b) {
		double res = 1;
		res *= Math.abs(a.x - b.x);
		res *= Math.abs(a.y - b.y);
		res *= Math.abs(a.z - b.z);
		return res;
	}

	private void buildGalaxyPoints(boolean generateRandomDensityField) {
		var random = new Random();
		var densityFields = this.galaxy.densityFields;
		if (generateRandomDensityField) {
			final var age = random.nextDouble(500, 12000);
			densityFields = this.galaxy.info.type.createDensityFields(age, random);
		}

		var volumeMin = Vec3.from(-densityFields.galaxyRadius, -densityFields.galaxyRadius * 0.3,
				-densityFields.galaxyRadius);
		var volumeMax = Vec3.from(densityFields.galaxyRadius, densityFields.galaxyRadius * 0.3, densityFields.galaxyRadius);

		this.galaxyPoints = new Octree<Double>(volumeMin, volumeMax);

		var attemptCount = 1000000;

		int successfulPlacements = 0;
		double highestSeenDensity = Double.NEGATIVE_INFINITY;
		double lowestSeenDensity = Double.POSITIVE_INFINITY;
		var maxDensity = 1e21 * attemptCount / aabbVolume(volumeMin, volumeMax);
		for (var i = 0; i < attemptCount; ++i) {
			if (successfulPlacements >= 30000)
				break;

			var pos = Vec3.random(random, volumeMin, volumeMax);

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			var density = densityFields.stellarDensity.sample(pos)
					/ (Units.ly_PER_Tm * Units.ly_PER_Tm * Units.ly_PER_Tm);

			if (density > highestSeenDensity)
				highestSeenDensity = density;
			if (density < lowestSeenDensity)
				lowestSeenDensity = density;

			if (density >= random.nextDouble(0, maxDensity)) {
				this.galaxyPoints.insert(pos, 0, density);
				successfulPlacements += 1;
			}
		}

		Mod.LOGGER.info("placed " + successfulPlacements + " sample points");
		Mod.LOGGER.info("max " + maxDensity);
		Mod.LOGGER.info("lowest " + lowestSeenDensity);
		Mod.LOGGER.info("highest " + highestSeenDensity);
	}

	// @Override
	// public void render3d(OrbitCamera.Cached camera, float partialTick) {
	// 	if (galaxyPoints == null)
	// 		buildGalaxyPoints(false);

	// 	final var builder = BufferRenderer.IMMEDIATE_BUILDER;

	// 	RenderHelper.renderGrid(builder, camera, 1, 1, 10, 40, partialTick);

	// 	builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

	// 	this.galaxyPoints.enumerateElements(elem -> {
	// 		// var s = 0.2 * camera.pos.distanceTo(elem.pos);
	// 		double s = 4e7 * (elem.value / 5.0);
	// 		if (s < 5e5) s = 5e5;
	// 		if (s > 4e7) s = 4e7;
	// 		// double s = 1.0 * (elem.value / 5.0);
	// 		RenderHelper.addBillboard(builder, camera, new PoseStack(), elem.pos, s, Color.WHITE.withA(0.2));
	// 	});

	// 	builder.end();

	// 	this.client.getTextureManager().getTexture(Mod.namespaced("textures/misc/galaxyglow.png")).setFilter(true,
	// 			false);
	// 	RenderSystem.setShaderTexture(0, Mod.namespaced("textures/misc/galaxyglow.png"));
	// 	RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
	// 	RenderSystem.depthMask(false);
	// 	RenderSystem.enableDepthTest();
	// 	RenderSystem.disableCull();
	// 	RenderSystem.enableBlend();
	// 	builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));
	// }

	// @Override
	// public void render2d(PoseStack poseStack, float partialTick) {
	// 	this.client.font.draw(poseStack,
	// 			"test", 0, 0,
	// 			0xff777777);
	// }

}
