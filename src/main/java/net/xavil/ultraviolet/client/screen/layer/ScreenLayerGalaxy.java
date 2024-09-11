package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.ultraviolet.client.GalaxyRenderingContext;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.RenderHelper;

import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyRegionWeights;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyType;
import net.xavil.ultraviolet.mixin.accessor.MouseHandlerAccessor;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerGalaxy extends HawkScreen3d.Layer3d {
	private GalaxyRenderingContext galaxyRenderingContext;
	private final Galaxy galaxy;
	private final Vec3 originOffset;

	public ScreenLayerGalaxy(HawkScreen3d screen, Galaxy galaxy, Vec3 originOffset) {
		super(screen, new CameraConfig(1e3, false, 1e15, false));
		this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy.parameters);
		this.originOffset = originOffset;
		this.galaxy = galaxy;
	}

	@Override
	public boolean handleKeypress(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_P && keypress.hasModifiers(GLFW.GLFW_MOD_CONTROL)) {
			this.galaxyRenderingContext.close();
			final var rng = new SplittableRng(new Random().nextLong());
			final var params = new Galaxy.Info(GalaxyType.SPIRAL, rng.uniformLong("seed"),
					Quat.IDENTITY, 13000.0, 5.0, 0.0)
					.createGalaxyParameters();
			this.galaxyRenderingContext = new GalaxyRenderingContext(params);
			return true;
		}
		return false;
	}

	@Override
	public void render3d(CachedCamera camera, RenderContext ctx) {
		this.galaxyRenderingContext.draw(camera, this.originOffset);
	}

	@Override
	public void render(RenderContext ctx) {
		super.render(ctx);

		// setup camera for ortho UI
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final double frustumDepth = 400;

		final var projMat = new Mat4.Mutable();
		final var projLR = aspectRatio * 1.0;
		final var projTB = 1.0;
		Mat4.setOrthographicProjection(projMat, 0, projLR, 0, -projTB, -frustumDepth, 0);

		final var inverseViewMat = new Mat4.Mutable();
		inverseViewMat.loadIdentity();
		inverseViewMat.appendTranslation(Vec3.ZERO.withZ(0.5 * frustumDepth));

		this.camera.load(inverseViewMat, projMat, 1);
		final var snapshot = RenderMatricesSnapshot.capture();
		this.camera.applyProjection();
		this.camera.applyView();

		// final var sink = new TextBuilder();
		final var tfm = new TransformStack();

		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				IndexPattern.VANILLA_LINES,
				BufferLayout.POSITION_COLOR_NORMAL);

		RenderHelper.addLine(lineBuilder, tfm, Vec3.NNN, Vec3.PPP, ColorRgba.WHITE);

		RenderSystem.lineWidth(1f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		// final var tmpMasks = new GalaxyRegionWeights();

		// this.galaxy.parameters.masks.evaluate(this.lastCamera.focus, tmpMasks);
		// final var overall = GalaxyRegionWeights.dot(tmpMasks,
		// this.galaxy.parameters.stellarDensityWeights);

		// sink.emitNewline(tfm.current(), String.format(
		// "core=%.3g, arms=%.3g, disc=%.3g, halo=%.3g, total=%.3g",
		// tmpMasks.core, tmpMasks.arms, tmpMasks.disc, tmpMasks.halo, overall));
		// sink.draw(BufferRenderer.IMMEDIATE_BUILDER,
		// HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);
		snapshot.restore();
	}

	@Override
	public void close() {
		super.close();
		this.galaxyRenderingContext.close();
	}

}
