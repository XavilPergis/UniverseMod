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
import net.xavil.ultraviolet.client.screen.layer.AxisMapping;
import net.xavil.ultraviolet.client.screen.layer.Histogram;
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
				UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD);

		// final var color = new ColorRgba(0.000f, 0.200f, 1.000f, 0.30f);
		// final var discColor = new ColorRgba(1.000f, 1.000f, 2.000f, 1.00f).mul(0.4f);
		final var discColor = new ColorRgba(0.000f, 0.200f, 1.000f, 1.00f).mul(0.4f);
		final var bulgeColor = new ColorRgba(2.000f, 1.000f, 1.000f, 1.00f).mul(0.2f);

		// final double squishFactor = rng.uniformDouble(0.4, 0.9);
		// final double squishFactor = 0.9;
		final double outerSquishFactor = 0.01;
		// final double spiralWrapFactor = rng.uniformDouble(0.5, 2);
		// final double spiralWrapPower = rng.uniformDouble(0.5, 2);
		// final double barSize = rng.uniformDouble(0.0, 0.4);
		// final double asymmetry = rng.weightedDouble(20, 0, 0.6);
		// final double irreg1 = rng.weightedDouble(20, 0, 0.6);
		final double spiralWrapFactor = 2.0;
		final double spiralWrapPower = 1.0;
		final double coreSize = 0.3;
		final double coreSquishFactor = 0.3;
		final double asymmetry = 0.03;
		final double irreg1 = 0.0;
		final double irreg2 = 0.03;
		final double irreg3 = 0.001;
		final double bulgeHeight = 0.35;
		final double bulgePower = 1.0;
		final double bulgePercent = 0.7;

		for (var i = 0; i < this.particleLimit; ++i) {
			Vec3 pos = Vec3.ZERO;
			ColorRgba color = ColorRgba.WHITE;

			if (rng.chance(0.25)) {
				// bulge owo
				final var r = Math.pow(rng.uniformDouble(), bulgePower);
				while (pos.lengthSquared() <= 1e-12 || pos.lengthSquared() > 1) {
					// pos = Vec3.random(rng, Vec3.NNN, Vec3.PPP).normalize();
					pos = Vec3.random(rng, Vec3.NNN, Vec3.PPP);
				}
				pos = pos.mul(bulgePercent * r * params.galaxyRadius);
				pos = pos.withY(pos.y * bulgeHeight);
				color = bulgeColor;
			} else {
				// arms/disc
				final var r = rng.uniformDouble();
				final var angle = rng.uniformDouble(0, 2 * Math.PI);

				final var h = Mth.lerp(Math.exp(-1 * r * r), 0.02, 0.1);
				// final var h = 0.05;
				final var y = rng.weightedDouble(3, 0, h) * Math.signum(rng.uniformDouble(Interval.BIPOLAR));

				// rotate each ellipse based on its size
				final var wrapAmount = Math.max(0, Math.pow(r, spiralWrapPower) - coreSize) / (1 - coreSize);
				final var axis = Vec3.YP
						.add(Vec3.PCP.mul(r * irreg2))
						.add(Vec3.PCP.mul(r * wrapAmount * irreg3))
						.normalize();
				final var wrapAngle = Math.PI * spiralWrapFactor * wrapAmount;

				final var zSquish = Mth.lerp(r, coreSquishFactor, outerSquishFactor);
				// generate random point on unoriented ellipse
				pos = new Vec3(r * Math.cos(angle), y, r * Math.sin(angle) * zSquish + irreg1);
				pos = Quat.axisAngle(axis, wrapAngle).transform(pos);
				pos = pos.withZ(pos.z + asymmetry * r);
				pos = pos.mul(params.galaxyRadius);
				color = discColor;
			}

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
						UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD);

		final var coreColor = new ColorRgba(2.000f, 1.000f, 1.000f, 1.00f).mul(0.8f);
		final var discColor = new ColorRgba(0.000f, 0.200f, 1.000f, 1.00f).mul(0.4f);
		final var armsColor = new ColorRgba(0.000f, 0.200f, 1.000f, 1.00f).mul(0.8f);
		// final var coreColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.30f);
		// final var armsColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.90f);
		// final var discColor = new ColorRgba(0.000f, 0.200f, 1.000f, 0.90f);

		final var densityHistogram = new Histogram("Density", 64, new AxisMapping.Log(10, 1e-10, 1e2));
		double densityMin = Double.POSITIVE_INFINITY, densityMax = Double.NEGATIVE_INFINITY;
		for (var i = 0; i < 100000; ++i) {
			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			params.stellarDensityField.evaluate(samplePos, weights);
			// weights.core *= Math.min(1, params.stellarDensityWeights.core);
			// weights.arms *= Math.min(1, params.stellarDensityWeights.arms);
			// weights.disc *= Math.min(1, params.stellarDensityWeights.disc);
			// weights.halo *= Math.min(1, params.stellarDensityWeights.halo);

			final var density = weights.totalWeight();
			densityHistogram.insert(density);
			densityMin = Math.min(densityMin, density);
			densityMax = Math.max(densityMax, density);
		}

		int maxBin = -1, maxBinCount = 0;
		double weigtedSum = 0.0;
		for (var i = 0; i < densityHistogram.size(); ++i) {
			final var binCount = densityHistogram.get(i);
			final var binValue = densityHistogram.mapping.unmap(i / (double) densityHistogram.size());
			weigtedSum += i * binCount * binValue;
			if (binCount > maxBinCount) {
				maxBinCount = binCount;
				maxBin = i;
			}
		}

		final var weightedAverage = weigtedSum / densityHistogram.total();

		final var densityMode = densityHistogram.mapping.unmap(maxBin / (double) densityHistogram.size());

		Mod.LOGGER.info("Density mean={}, min={}, max={}, mode={}, weighted={}", densityHistogram.getMean(), densityMin,
				densityMax,
				densityMode, weightedAverage);

		for (var i = 0; i < this.attemptCount; ++i) {
			if (successfulPlacements >= this.particleLimit)
				break;

			Vec3.loadRandom(samplePos, rng, volumeMin, volumeMax);
			params.stellarDensityField.evaluate(samplePos, weights);

			final var density = weights.totalWeight();
			if (weightedAverage * rng.uniformDouble() > density)
				continue;

			ColorRgba colorRes = ColorRgba.TRANSPARENT;
			double w = rng.uniformDouble(0, weights.totalWeight());
			colorRes = weights.pick(w, coreColor, armsColor, discColor, ColorRgba.BLACK);

			double maxT = 2.0 / Math.PI * Math.atan(100.0 * density);
			float r, g, b, a;
			r = (float) (2.0 / Math.PI * Math.atan(colorRes.r));
			g = (float) (2.0 / Math.PI * Math.atan(colorRes.g));
			b = (float) (2.0 / Math.PI * Math.atan(colorRes.b));
			a = (float) (2.0 / Math.PI * Math.atan(colorRes.a));
			a *= weights.totalWeight();
			colorRes = new ColorRgba(r, g, b, a);

			// final var density = info.averageSectorDensity * rng.uniformDouble("density");
			// if (density < masks.totalWeight())
			// break;

			double t = rng.uniformDouble(0, maxT);
			t = Mth.clamp(t, 0, 1);
			// double size = 8e7;
			// double size = 4.5e7;
			double size = this.params.galaxyRadius / 10;
			// double size = rng.uniformDouble(4.5e7, 8.5e7);
			// double size = Mth.lerp(t, 2.0e7, 15.5e7);
			size = size * (1e12 / metersPerUnit);
			double sp = size, sn = -size;

			Vec3 dir = Vec3.ZERO;
			while (dir.lengthSquared() <= 1e-6) {
				dir = Vec3.random(rng, Vec3.NNN, Vec3.PPP).normalize();
			}
			final var rotation = Quat.axisAngle(dir, 2 * Math.PI * rng.uniformDouble());

			builder.vertex(samplePos.add(rotation.transform(new Vec3(sp, 0, sn))))
					.color(colorRes).uv0(1f, 0f).endVertex();
			builder.vertex(samplePos.add(rotation.transform(new Vec3(sp, 0, sp))))
					.color(colorRes).uv0(1f, 1f).endVertex();
			builder.vertex(samplePos.add(rotation.transform(new Vec3(sn, 0, sp))))
					.color(colorRes).uv0(0f, 1f).endVertex();
			builder.vertex(samplePos.add(rotation.transform(new Vec3(sn, 0, sn))))
					.color(colorRes).uv0(0f, 0f).endVertex();
			successfulPlacements += 1;
		}

		this.pointsBuffer.setupAndUpload(builder.end());

		Mod.LOGGER.info("Built galaxy particle buffer, {} particles", successfulPlacements);
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
		buildGalaxyPointsFromSdf(this.params, camera.metersPerUnit);
		// buildGalaxyPointsDensityWave(this.params, camera.metersPerUnit);

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
