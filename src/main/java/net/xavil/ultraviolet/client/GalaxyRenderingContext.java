package net.xavil.ultraviolet.client;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexBuffer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;

public class GalaxyRenderingContext implements Disposable {

	private final Galaxy galaxy;
	private FlexibleVertexBuffer pointsBuffer = new FlexibleVertexBuffer();
	private boolean isInitialized = false;

	public GalaxyRenderingContext(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	private static double aabbVolume(Vec3 a, Vec3 b) {
		double res = 1;
		res *= Math.abs(a.x - b.x);
		res *= Math.abs(a.y - b.y);
		res *= Math.abs(a.z - b.z);
		return res;
	}

	private static final double Tm3_PER_ly3 = 1.0 / Math.pow(Units.ly_PER_Tm, 3.0);

	private double metersPerUnit = -1;
	private int attemptCount = -1;
	private int particleLimit = -1;

	private void buildGalaxyPoints(DensityFields densityFields, double metersPerUnit) {
		if (!needsRebuild(metersPerUnit))
			return;

		final var random = new Random();

		final var volumeMin = Vec3.broadcast(-densityFields.galaxyRadius).mul(1, 0.25, 1);
		final var volumeMax = Vec3.broadcast(densityFields.galaxyRadius).mul(1, 0.25, 1);

		int successfulPlacements = 0;
		double highestSeenDensity = Double.NEGATIVE_INFINITY;
		double lowestSeenDensity = Double.POSITIVE_INFINITY;
		final var maxDensity = 1e21 * this.attemptCount / aabbVolume(volumeMin, volumeMax);

		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		for (var i = 0; i < this.attemptCount; ++i) {
			if (successfulPlacements > this.particleLimit)
				break;

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			final var samplePos = Vec3.random(random, volumeMin, volumeMax);
			final var density = Tm3_PER_ly3 * densityFields.stellarDensity.sample(samplePos);

			if (density > highestSeenDensity)
				highestSeenDensity = density;
			if (density < lowestSeenDensity)
				lowestSeenDensity = density;

			if (density >= random.nextDouble(0, maxDensity)) {
				double size = 4e7 * (density / 5.0);
				size = Mth.clamp(size, 5e6, 3e7);
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
		buildGalaxyPoints(this.galaxy.densityFields, camera.metersPerUnit);

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
