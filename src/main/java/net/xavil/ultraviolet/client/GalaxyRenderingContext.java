package net.xavil.ultraviolet.client;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyRegionWeights;

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

	private void buildGalaxyPointsDensityWave(GalaxyParameters params, double metersPerUnit) {
		if (!needsRebuild(metersPerUnit))
			return;
		this.isInitialized = true;

		final var rng = Rng.wrap(new Random());

		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(IndexPattern.QUADS,
				UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD_REALISTIC);

		// final var color = new ColorRgba(0.000f, 0.200f, 1.000f, 0.30f);
		final var color = new ColorRgba(1.000f, 1.000f, 2.000f, 1.00f).mul(0.01f);

		// final double squishFactor = rng.uniformDouble(0.4, 0.9);
		// final double squishFactor = 0.9;
		final double squishFactor = 0.01;
		// final double spiralWrapFactor = rng.uniformDouble(0.5, 2);
		// final double spiralWrapPower = rng.uniformDouble(0.5, 2);
		// final double barSize = rng.uniformDouble(0.0, 0.4);
		// final double asymmetry = rng.weightedDouble(20, 0, 0.6);
		// final double irreg1 = rng.weightedDouble(20, 0, 0.6);
		final double spiralWrapFactor = 3.7/2;
		final double spiralWrapPower = 1.0;
		final double barSize = 0.3;
		final double asymmetry = 0.03;
		final double irreg1 = 0.0;
		final double irreg2 = 0.03;
		final double irreg3 = 0.001;

		for (var i = 0; i < this.particleLimit; ++i) {

			final var r = rng.uniformDouble();
			final var angle = rng.uniformDouble(0, 2 * Math.PI);

			final var h = Mth.lerp(Math.exp(-1 * r * r), 0.02, 0.1);
			// final var h = 0.05;
			final var y = rng.weightedDouble(3, 0, h) * Math.signum(rng.uniformDouble(Interval.BIPOLAR));

			// generate random point on unoriented ellipse
			Vec3 pos = new Vec3(r * Math.cos(angle), y, r * Math.sin(angle) * Mth.lerp(r, 1, squishFactor) + irreg1);

			// rotate each ellipse based on its size
			final var wrapAmount = Math.max(0, Math.pow(r, spiralWrapPower) - barSize);
			// final var axis = Vec3.YP;
			final var axis = Vec3.YP.add(Vec3.PCP.mul(r * irreg2)).add(Vec3.PCP.mul(r * wrapAmount * irreg3)).normalize();
			final var wrapAngle = 2.0 * Math.PI * spiralWrapFactor * wrapAmount;
			pos = Quat.axisAngle(axis, wrapAngle).transform(pos);
			pos = pos.withZ(pos.z + asymmetry * r);
			pos = pos.mul(params.galaxyRadius);

			Vec3 dir = Vec3.ZERO;
			while (dir.lengthSquared() <= 1e-6) {
				dir = Vec3.random(rng, Vec3.NNN, Vec3.PPP).normalize();
			}
			final var rotation = Quat.axisAngle(dir, 2 * Math.PI * rng.uniformDouble());

			// double size = 4.5e7;
			// double size = Mth.lerp(Math.exp(-1 * r * r), 2e7, 6e7);
			double size = 3e7;
			size = size * (1e12 / metersPerUnit);
			double sp = size, sn = -size;

			builder.vertex(pos.add(rotation.transform(new Vec3(sp, 0, sn)))).color(color).uv0(1f, 0f).endVertex();
			builder.vertex(pos.add(rotation.transform(new Vec3(sp, 0, sp)))).color(color).uv0(1f, 1f).endVertex();
			builder.vertex(pos.add(rotation.transform(new Vec3(sn, 0, sp)))).color(color).uv0(0f, 1f).endVertex();
			builder.vertex(pos.add(rotation.transform(new Vec3(sn, 0, sn)))).color(color).uv0(0f, 0f).endVertex();
		}

		this.pointsBuffer.setupAndUpload(builder.end());

		Mod.LOGGER.debug("Built galaxy particle buffer, {} particles", this.particleLimit);
	}

	private void buildGalaxyPointsFromSdf(GalaxyParameters params, double metersPerUnit) {
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
				.beginGeneric(IndexPattern.QUADS,
						UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD_REALISTIC);

		final var coreColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.30f);
		final var armsColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.90f);
		final var discColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.90f);

		for (var i = 0; i < this.attemptCount; ++i) {
			if (successfulPlacements >= this.particleLimit)
				break;

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			params.masks.evaluate(samplePos, weights);
			weights.core *= Math.min(1, params.stellarDensityWeights.core);
			weights.arms *= Math.min(1, params.stellarDensityWeights.arms);
			weights.disc *= Math.min(1, params.stellarDensityWeights.disc);
			weights.halo *= Math.min(1, params.stellarDensityWeights.halo);

			final var density = weights.totalWeight();

			ColorRgba colorRes = ColorRgba.TRANSPARENT;
			// colorRes = colorRes.add(coreColor.mul((float) (weights.core / (weights.core +
			// weights.arms + weights.disc))));
			// colorRes = colorRes.add(armsColor.mul((float) (weights.arms / (weights.disc +
			// weights.core + weights.arms))));
			// colorRes = colorRes.add(discColor.mul((float) (weights.disc / (weights.arms +
			// weights.disc + weights.core))));
			colorRes = colorRes.add(coreColor.mul((float) Math.max(0.1, weights.core)));
			colorRes = colorRes.add(armsColor.mul((float) Math.max(0.1, weights.arms)));
			colorRes = colorRes.add(discColor.mul((float) Math.max(0.1, weights.disc)));

			double maxT = 2.0 / Math.PI * Math.atan(100.0 * density);
			float r, g, b, a;
			r = (float) (2.0 / Math.PI * Math.atan(colorRes.r));
			g = (float) (2.0 / Math.PI * Math.atan(colorRes.g));
			b = (float) (2.0 / Math.PI * Math.atan(colorRes.b));
			a = (float) (2.0 / Math.PI * Math.atan(colorRes.a));
			colorRes = new ColorRgba(r, g, b, a);

			if (density >= 0.001) {
				double t = rng.uniformDouble(0, maxT);
				t = Mth.clamp(t, 0, 1);
				// double size = 8e7;
				// double size = 4.5e7;
				// double size = rng.uniformDouble(4.5e7, 8.5e7);
				double size = Mth.lerp(t, 2.0e7, 15.5e7);
				size = size * (1e12 / metersPerUnit);
				double sp = size, sn = -size;

				Vec3 dir = Vec3.ZERO;
				while (dir.lengthSquared() <= 1e-6) {
					dir = Vec3.random(rng, Vec3.NNN, Vec3.PPP).normalize();
				}
				final var rotation = Quat.axisAngle(dir, 2 * Math.PI * rng.uniformDouble());

				builder.vertex(samplePos.add(rotation.transform(new Vec3(sp, 0, sn)))).color(colorRes).uv0(1f, 0f)
						.endVertex();
				builder.vertex(samplePos.add(rotation.transform(new Vec3(sp, 0, sp)))).color(colorRes).uv0(1f, 1f)
						.endVertex();
				builder.vertex(samplePos.add(rotation.transform(new Vec3(sn, 0, sp)))).color(colorRes).uv0(0f, 1f)
						.endVertex();
				builder.vertex(samplePos.add(rotation.transform(new Vec3(sn, 0, sn)))).color(colorRes).uv0(0f, 0f)
						.endVertex();
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
		// buildGalaxyPointsFromSdf(this.params, camera.metersPerUnit);
		buildGalaxyPointsDensityWave(this.params, camera.metersPerUnit);

		final var snapshot = RenderMatricesSnapshot.capture();
		camera.applyProjection();
		final var offset = originOffset.mul(1e12 / camera.metersPerUnit).neg().sub(camera.pos);
		camera.applyView(offset);

		final var shader = UltravioletShaders.SHADER_GALAXY_PARTICLE.get();
		final var texture = GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION);
		texture.setMagFilter(GlTexture.MagFilter.LINEAR);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		shader.setUniformf("uParticleMultiplier", ClientConfig.get(ConfigKey.GALAXY_SHADER_PARTICLE_BRIGHTNESS));
		shader.setupDefaultShaderUniforms();
		this.pointsBuffer.draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ADDITIVE_BLENDING);

		snapshot.restore();
	}

	@Override
	public void close() {
		if (this.pointsBuffer != null)
			this.pointsBuffer.close();
	}

}
