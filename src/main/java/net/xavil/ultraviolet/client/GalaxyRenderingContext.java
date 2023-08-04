package net.xavil.ultraviolet.client;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexBuffer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;

public class GalaxyRenderingContext implements Disposable {

	private final DensityFields densityFields;
	private FlexibleVertexBuffer pointsBuffer = new FlexibleVertexBuffer();
	private boolean isInitialized = false;

	public GalaxyRenderingContext(DensityFields densityFields) {
		this.densityFields = densityFields;
	}

	private double metersPerUnit = -1;
	private int attemptCount = -1;
	private int particleLimit = -1;

	private void buildGalaxyPoints(DensityFields densityFields, double metersPerUnit) {
		if (!needsRebuild(metersPerUnit))
			return;

		final var rng = Rng.wrap(new Random());

		final var volumeMin = Vec3.broadcast(-densityFields.galaxyRadius).mul(1, 0.25, 1);
		final var volumeMax = Vec3.broadcast(densityFields.galaxyRadius).mul(1, 0.25, 1);

		double highestSeenDensity = Double.NEGATIVE_INFINITY;
		double lowestSeenDensity = Double.POSITIVE_INFINITY;
		double averageDensity = 0;

		final int initialDensitySampleCount = 10000;

		final var samplePos = new Vec3.Mutable();
		for (var i = 0; i < initialDensitySampleCount; ++i) {
			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			final var density = densityFields.stellarDensity.sample(samplePos);

			if (density > highestSeenDensity)
				highestSeenDensity = density;
			if (density < lowestSeenDensity)
				lowestSeenDensity = density;
			averageDensity += density;
		}

		averageDensity /= initialDensitySampleCount;

		int successfulPlacements = 0;
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		for (var i = 0; i < this.attemptCount; ++i) {
			if (successfulPlacements >= this.particleLimit)
				break;

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			final var density = densityFields.stellarDensity.sample(samplePos);

			if (density >= rng.uniformDouble(0, 0.1 * averageDensity)) {
				double maxT = 2.0 / Math.PI * Math.atan(density / averageDensity);
				double t = rng.uniformDouble(0, maxT);
				// size = Math.max(size, 2e7);
				// size = Mth.clamp(size, 5e6, 7e7);
				// size = Mth.clamp(size, 1e7, 1e7);
				t = Mth.clamp(t, 0, 1);
				double size = Mth.lerp(t, 5e6, 5e7);
				size = size * (1e12 / metersPerUnit);
				builder.vertex(samplePos).color(Color.WHITE.withA(0.1)).uv0((float) size, 0).endVertex();
				successfulPlacements += 1;
			}
		}

		this.pointsBuffer.upload(builder.end());
		this.isInitialized = true;

		Mod.LOGGER.info("built galaxy dust buffer: {} placements, density in [{},{}]", successfulPlacements,
				lowestSeenDensity, highestSeenDensity);
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

	public void draw(CachedCamera<?> camera, Vec3 originOffset) {
		buildGalaxyPoints(this.densityFields, camera.metersPerUnit);

		final var snapshot = camera.setupRenderMatrices();

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			final var offset = originOffset.mul(1e12 / camera.metersPerUnit).sub(camera.pos);
			poseStack.mulPose(camera.orientation.toMinecraft());
			poseStack.translate(offset.x, offset.y, offset.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		final var shader = UltravioletShaders.getShader(UltravioletShaders.SHADER_GALAXY_PARTICLE);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		BufferRenderer.setupDefaultShaderUniforms(shader);
		this.pointsBuffer.draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ADDITIVE_BLENDING);

		snapshot.restore();
	}

	@Override
	public void close() {
		if (this.pointsBuffer != null)
			this.pointsBuffer.close();
	}

}
