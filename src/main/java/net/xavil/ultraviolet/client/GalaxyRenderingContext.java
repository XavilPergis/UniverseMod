package net.xavil.ultraviolet.client;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyRegionWeights;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec3;

public class GalaxyRenderingContext implements Disposable {

	private final GalaxyParameters params;
	private Mesh pointsBuffer = new Mesh();
	private boolean isInitialized = false;

	public GalaxyRenderingContext(GalaxyParameters params) {
		this.params = params;
	}

	private double metersPerUnit = -1;
	private int attemptCount = -1;
	private int particleLimit = -1;

	private void buildGalaxyPoints(GalaxyParameters params, double metersPerUnit) {
		if (!needsRebuild(metersPerUnit))
			return;
		this.isInitialized = true;

		final var rng = Rng.wrap(new Random());

		final var volumeMin = Vec3.broadcast(-params.galaxyRadius).mul(1, 0.25, 1);
		final var volumeMax = Vec3.broadcast(params.galaxyRadius).mul(1, 0.25, 1);

		final var samplePos = new Vec3.Mutable();
		final var weights = new GalaxyRegionWeights();
		int successfulPlacements = 0;
		final var builder = BufferRenderer.IMMEDIATE_BUILDER
				.beginGeneric(PrimitiveType.POINT_DUPLICATED,
						UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD_REALISTIC);

		// final var coreColor = new ColorRgba(1.000f, 0.945f, 0.576f, 0.01f * 1.00f);
		// final var armsColor = new ColorRgba(0.839f, 0.910f, 0.988f, 0.01f * 0.80f);
		// final var discColor = new ColorRgba(0.988f, 0.980f, 0.937f, 0.01f * 0.10f);
		final var coreColor = new ColorRgba(1.000f, 0.945f, 0.576f, 10f * 1.00f);
		final var armsColor = new ColorRgba(0.839f, 0.910f, 0.988f, 10f * 0.80f);
		final var discColor = new ColorRgba(0.988f, 0.980f, 0.937f, 10f * 0.10f);

		for (var i = 0; i < this.attemptCount; ++i) {
			if (successfulPlacements >= this.particleLimit)
				break;

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			params.masks.evaluate(samplePos, weights);
			weights.core *= Math.min(100, params.stellarDensityWeights.core);
			weights.arms *= Math.min(100, params.stellarDensityWeights.arms);
			weights.disc *= Math.min(100, params.stellarDensityWeights.disc);
			weights.halo *= Math.min(100, params.stellarDensityWeights.halo);

			final var density = weights.totalWeight();

			ColorRgba colorRes = ColorRgba.TRANSPARENT;
			// colorRes = colorRes.add(coreColor.mul((float) (weights.core / (weights.core + weights.arms + weights.disc))));
			// colorRes = colorRes.add(armsColor.mul((float) (weights.arms / (weights.disc + weights.core + weights.arms))));
			// colorRes = colorRes.add(discColor.mul((float) (weights.disc / (weights.arms + weights.disc + weights.core))));
			colorRes = colorRes.add(coreColor.mul((float) weights.core));
			colorRes = colorRes.add(armsColor.mul((float) weights.arms));
			colorRes = colorRes.add(discColor.mul((float) weights.disc));

			// double maxT = 2.0 / Math.PI * Math.atan(10.0 * density);
			float r, g, b, a;
			r = (float) (2.0 / Math.PI * Math.atan(colorRes.r));
			g = (float) (2.0 / Math.PI * Math.atan(colorRes.g));
			b = (float) (2.0 / Math.PI * Math.atan(colorRes.b));
			a = (float) (2.0 / Math.PI * Math.atan(colorRes.a));
			colorRes = new ColorRgba(r, g, b, a);

			if (density >= 0.01) {
				// double t = rng.uniformDouble(0, maxT);
				// t = Mth.clamp(t, 0, 1);
				// double size = 8e7;
				double size = 4.5e7;
				size = size * (1e12 / metersPerUnit);
				builder.vertex(samplePos).color(colorRes).uv0((float) size, 0).endVertex();
				successfulPlacements += 1;
			}
		}

		this.pointsBuffer.setupAndUpload(builder.end());

		Mod.LOGGER.debug("Built galaxy particle buffer, {} particles", successfulPlacements);
	}

	private boolean needsRebuild(double metersPerUnit) {
		boolean res = false;
		res |= !this.isInitialized;
		res |= this.metersPerUnit != metersPerUnit;
		this.metersPerUnit = metersPerUnit;

		final var attemptCount2 = ClientConfig.get(ConfigKey.GALAXY_PARTILE_ATTEMPT_COUNT);
		res |= this.attemptCount != attemptCount2;
		this.attemptCount = attemptCount2;

		final var particleLimit2 = ClientConfig.get(ConfigKey.GALAXY_PARTILE_MAX_PARTICLES);
		res |= this.particleLimit != particleLimit2;
		this.particleLimit = particleLimit2;

		return res;
	}

	public void draw(CachedCamera camera, Vec3 originOffset) {
		buildGalaxyPoints(this.params, camera.metersPerUnit);

		final var snapshot = RenderMatricesSnapshot.capture();
		camera.applyProjection();

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			final var offset = originOffset.mul(1e12 / camera.metersPerUnit).sub(camera.pos);
			poseStack.mulPose(camera.orientation.asMinecraft());
			poseStack.translate(offset.x, offset.y, offset.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		final var shader = UltravioletShaders.SHADER_GALAXY_PARTICLE.get();
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		shader.setupDefaultShaderUniforms();
		// this.pointsBuffer.draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ADDITIVE_BLENDING);

		snapshot.restore();
	}

	@Override
	public void close() {
		if (this.pointsBuffer != null)
			this.pointsBuffer.close();
	}

}
