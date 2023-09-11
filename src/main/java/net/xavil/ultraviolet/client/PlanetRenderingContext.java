package net.xavil.ultraviolet.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;

import static net.xavil.hawklib.client.HawkDrawStates.*;

import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Vec3;

public final class PlanetRenderingContext {

	public static final ResourceLocation PLANET_ATLAS_LOCATION = Mod.namespaced("textures/atlas/planets");

	public static final ResourceLocation BASE_ROCKY_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_rocky.png");
	public static final ResourceLocation BASE_WATER_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_water.png");
	public static final ResourceLocation BASE_GAS_GIANT_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_gas_giant.png");
	public static final ResourceLocation FEATURE_CRATERS_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/craters.png");
	public static final ResourceLocation FEATURE_EARTH_LIKE_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/earth_like.png");

	private static ResourceLocation getBaseLayer(PlanetaryCelestialNode node) {
		final var missing = MissingTextureAtlasSprite.getLocation();
		return switch (node.type) {
			case EARTH_LIKE_WORLD -> BASE_WATER_LOCATION;
			case GAS_GIANT -> BASE_GAS_GIANT_LOCATION;
			case ICE_WORLD -> BASE_WATER_LOCATION;
			case ROCKY_ICE_WORLD -> BASE_WATER_LOCATION;
			case ROCKY_WORLD -> BASE_ROCKY_LOCATION;
			case WATER_WORLD -> BASE_WATER_LOCATION;
			default -> missing;
		};
	}

	private final MutableList<StellarCelestialNode> systemStars = new Vector<>();
	private final Minecraft client = Minecraft.getInstance();
	private double celestialTime = 0;
	private Vec3 origin = Vec3.ZERO;
	private int renderedPlanetCount = 0;
	private int renderedStarCount = 0;

	public PlanetRenderingContext() {
	}

	public void begin(StarSystem system, double celestialTime) {
		this.celestialTime = celestialTime;
		this.renderedPlanetCount = 0;
		this.renderedStarCount = 0;

		this.systemStars.clear();
		this.systemStars.extend(system.rootNode.iter().filterCast(StellarCelestialNode.class));
	}

	public void end() {
	}

	public void setSystemOrigin(Vec3 origin) {
		this.origin = origin;
	}

	public int getRenderedPlanetCount() {
		return this.renderedPlanetCount;
	}

	public int getRenderedStarCount() {
		return this.renderedStarCount;
	}

	public void render(VertexBuilder builder, CachedCamera<?> camera, CelestialNode node, boolean skip) {
		if (node instanceof StellarCelestialNode starNode)
			renderStar(builder, camera, starNode, skip);
		if (node instanceof PlanetaryCelestialNode planetNode)
			renderPlanet(builder, camera, planetNode, skip);
	}

	public void renderStar(VertexBuilder vertexBuilder, CachedCamera<?> camera, StellarCelestialNode node,
			boolean skip) {
		var radiusM = ClientConfig.get(ConfigKey.PLANET_EXAGGERATION_FACTOR) * Units.u_PER_ku * node.radius;

		// FIXME: this is a garbage ass hack cuz i cant figure out why the interpolated
		// position is fucked in the starmap
		var pos = node.position.xyz();
		if (!(this.client.screen instanceof HawkScreen)) {
			final var partialTick = this.client.getFrameTime();
			pos = node.getPosition(partialTick);
		}

		var nodePosUnits = pos.add(this.origin).mul(1e12 / camera.metersPerUnit);

		// var distanceFromCamera = camera.pos.mul(1e12 /
		// camera.metersPerUnit).distanceTo(nodePosUnits);
		// var distanceRatio = radiusM / (distanceFromCamera * camera.metersPerUnit);

		// if (distanceRatio < 0.0001)
		// return;

		if (!skip) {
			var radiusUnits = 1.0 * radiusM / camera.metersPerUnit;
			final var builder = vertexBuilder
					.beginGeneric(PrimitiveType.QUADS, BufferLayout.POSITION_TEX_COLOR_NORMAL);
			addNormSphere(builder, camera, nodePosUnits.mul(camera.metersPerUnit / 1e12), radiusUnits);
			final var shader = UltravioletShaders.SHADER_STAR.get();
			shader.setUniform("uStarColor", node.getColor().withA(node.luminosityLsol));
			shader.setUniform("uTime", this.celestialTime % 10000.0);
			builder.end().draw(shader, DRAW_STATE_NO_CULL);
		}

		// tfm.push();
		// tfm.prependTranslation(this.origin);
		// RenderHelper.renderStarBillboard(builder, camera, tfm, node);
		// tfm.pop();

		this.renderedStarCount += 1;
	}

	private void setupShaderCommon(CachedCamera<?> camera, Vec3 sortOrigin, ShaderProgram shader) {

		final int maxStarCount = 4;

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		final var sortedStars = new Vector<>(this.systemStars);
		// var nodePosUnits = pos.add(this.origin).mul(1e12 / camera.metersPerUnit);

		sortedStars.sort(Comparator.comparingDouble(star -> star.position.xyz().distanceTo(sortOrigin)));

		final var starCount = Math.min(maxStarCount, sortedStars.size());

		shader.setUniform("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniform("uTime", this.celestialTime);

		for (var i = 0; i < maxStarCount; ++i) {
			shader.setUniform("uLightColor" + i, 0f, 0f, 0f, -1f);
		}

		for (var i = 0; i < starCount; ++i) {
			final var star = sortedStars.get(i);

			shader.setUniform("uLightColor" + i, star.getColor().withA(star.luminosityLsol));

			final var pos = camera.toCameraSpace(this.origin.add(star.position).mul(1 / camera.metersPerUnit));
			shader.setUniform("uLightPos" + i, pos.withW(1));

			shader.setUniform("uLightRadius" + i, star.radius * Units.u_PER_ku / camera.metersPerUnit);
		}

		BufferRenderer.setupCameraUniforms(shader, camera);
	}

	private void setupPlanetShader(PlanetaryCelestialNode node, ShaderProgram shader) {
		shader.setUniform("uPlanetType", switch (node.type) {
			case EARTH_LIKE_WORLD -> 0;
			case GAS_GIANT -> 1;
			case ICE_WORLD -> 2;
			case ROCKY_ICE_WORLD -> 3;
			case ROCKY_WORLD -> 4;
			case WATER_WORLD -> 5;
			case BROWN_DWARF -> 6;
		});
		shader.setUniform("uRenderingSeed", (float) (FastHasher.hashInt(node.getId()) % 1000000L));
	}

	public void renderPlanet(VertexBuilder vertexBuilder, CachedCamera<?> camera, PlanetaryCelestialNode node,
			boolean skip) {

		var radiusM = ClientConfig.get(ConfigKey.PLANET_EXAGGERATION_FACTOR) * Units.u_PER_ku * node.radius;
		// var radiusM = 200 * Units.METERS_PER_REARTH * node.radiusRearth;
		// final var metersPerUnit = 1 / unitsPerMeter;

		// FIXME: this is a garbage ass hack cuz i cant figure out why the interpolated
		// position is fucked in the starmap
		var pos = node.position.xyz();
		if (!(this.client.screen instanceof HawkScreen)) {
			final var partialTick = this.client.getFrameTime();
			pos = node.getPosition(partialTick);
		}

		var nodePosUnits = pos.mul(1e12 / camera.metersPerUnit)
				.add(this.origin.mul(1e12 / camera.metersPerUnit));

		var distanceFromCamera = camera.pos.mul(1e12 / camera.metersPerUnit).distanceTo(nodePosUnits);
		var distanceRatio = radiusM / (distanceFromCamera * camera.metersPerUnit);

		if (distanceRatio < 0.0001)
			return;

		setupShaderCommon(camera, pos, UltravioletShaders.SHADER_PLANET.get());
		setupShaderCommon(camera, pos, UltravioletShaders.SHADER_RING.get());
		setupPlanetShader(node, UltravioletShaders.SHADER_PLANET.get());

		final var modelTfm = new TransformStack();
		modelTfm.prependRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));
		modelTfm.prependRotation(Quat.axisAngle(Vec3.YP, -node.rotationalRate * this.celestialTime));

		final var planetShader = UltravioletShaders.SHADER_PLANET.get();
		planetShader.setUniform("uModelMatrix", modelTfm.current());
		planetShader.setUniform("uRotationAxis", Vec3.YP.rotateX(node.obliquityAngle));
		planetShader.setUniform("uRotationAngle", node.rotationalRate * this.celestialTime);
		UltravioletShaders.SHADER_RING.get().setUniform("uModelMatrix", modelTfm.current());

		if (!skip) {
			var baseTexture = getBaseLayer(node);
			Minecraft.getInstance().getTextureManager().getTexture(baseTexture).setFilter(false, false);

			var radiusUnits = 1.0 * radiusM / camera.metersPerUnit;
			// var radiusUnits = 1000 * 1000;
			// var radiusM = scale * 200 * Units.METERS_PER_REARTH * node.radiusRearth;
			// renderPlanetLayer(builder, camera, baseTexture, tfm, nodePosUnits,
			// radiusUnits, tintColor);
			// if (node.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
			// }
			renderPlanetLayer(vertexBuilder, camera, BASE_ROCKY_LOCATION, nodePosUnits.mul(camera.metersPerUnit / 1e12),
					radiusUnits);
		}

		final var builder = vertexBuilder.beginGeneric(PrimitiveType.QUADS, BufferLayout.POSITION_TEX_COLOR_NORMAL);
		if (node instanceof UnaryCelestialNode unaryNode) {
			for (final var ring : unaryNode.rings.iterable()) {
				// tfm.push();
				modelTfm.prependRotation(ring.orbitalPlane.rotationFromReference());
				addRing(builder, camera, nodePosUnits.mul(camera.metersPerUnit / 1e12), ring.interval.lower,
						ring.interval.higher);
				// tfm.pop();
			}
		}

		final var ringShader = UltravioletShaders.SHADER_RING.get();
		builder.end().draw(ringShader, DRAW_STATE_OPAQUE);

		this.renderedPlanetCount += 1;
	}

	private static void renderPlanetLayer(VertexBuilder vertexBuilder, CachedCamera<?> camera,
			ResourceLocation texture, Vec3 center, double radius) {
		final var planetShader = UltravioletShaders.SHADER_PLANET.get();
		// planetShader.setUniformSampler("uSurfaceAlbedo",
		// GlTexture2d.importTexture(texture));
		final var builder = vertexBuilder
				.beginGeneric(PrimitiveType.QUADS, BufferLayout.POSITION_TEX_COLOR_NORMAL);
		addNormSphere(builder, camera, center, radius);
		builder.end().draw(planetShader, DRAW_STATE_OPAQUE);
	}

	private static void addRing(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3 center, double innerRadius, double outerRadius) {
		int segmentCount = 60;
		for (var i = 0; i < segmentCount; ++i) {
			double percentL = i / (double) segmentCount;
			double percentH = (i + 1) / (double) segmentCount;
			double angleL = 2 * Math.PI * percentL;
			double angleH = 2 * Math.PI * percentH;
			double llx = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleL);
			double lly = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleL);
			double lhx = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleH);
			double lhy = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleH);
			double hlx = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleL);
			double hly = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleL);
			double hhx = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleH);
			double hhy = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleH);

			// clockwise
			ringVertex(builder, camera, center, lhx, 1, lhy, (float) percentH, 0);
			ringVertex(builder, camera, center, llx, 1, lly, (float) percentL, 0);
			ringVertex(builder, camera, center, hlx, 1, hly, (float) percentL, 10);
			ringVertex(builder, camera, center, hhx, 1, hhy, (float) percentH, 10);

			// counter-clockwise
			ringVertex(builder, camera, center, hhx, -1, hhy, (float) percentH, 10);
			ringVertex(builder, camera, center, hlx, -1, hly, (float) percentL, 10);
			ringVertex(builder, camera, center, llx, -1, lly, (float) percentL, 0);
			ringVertex(builder, camera, center, lhx, -1, lhy, (float) percentH, 0);
		}
	}

	private static void addNormSphere(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 center,
			double radius) {

		// final var pose = tfm == null ? null : tfm.get();
		final int subdivisions = 10;

		// -X
		double nxlu = 0.00f, nxlv = 0.5f, nxhu = 0.25f, nxhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lv = (float) Mth.lerp(lpa, nxlv, nxhv);
			var hv = (float) Mth.lerp(hpa, nxlv, nxhv);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lu = (float) Mth.lerp(lpb, nxlu, nxhu);
				var hu = (float) Mth.lerp(hpb, nxlu, nxhu);
				normSphereVertex(builder, camera, center, radius, -1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, center, radius, -1, la, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, -1, la, hb, hu, lv);
				normSphereVertex(builder, camera, center, radius, -1, ha, hb, hu, hv);
			}
		}

		// +X
		double pxlu = 0.75f, pxlv = 0.50f, pxhu = 0.50f, pxhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lv = (float) Mth.lerp(lpa, pxlv, pxhv);
			var hv = (float) Mth.lerp(hpa, pxlv, pxhv);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lu = (float) Mth.lerp(lpb, pxlu, pxhu);
				var hu = (float) Mth.lerp(hpb, pxlu, pxhu);
				normSphereVertex(builder, camera, center, radius, 1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, center, radius, 1, la, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, 1, la, hb, hu, lv);
				normSphereVertex(builder, camera, center, radius, 1, ha, hb, hu, hv);
			}
		}

		// -Y
		double nylu = 0.25f, nylv = 0.75f, nyhu = 0.50f, nyhv = 0.50f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, nylu, nyhu);
			var hu = (float) Mth.lerp(hpa, nylu, nyhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, nylv, nyhv);
				var hv = (float) Mth.lerp(hpb, nylv, nyhv);
				normSphereVertex(builder, camera, center, radius, ha, -1, lb, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, -1, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, -1, hb, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, -1, hb, hu, hv);
			}
		}

		// +Y
		double pylu = 0.25f, pylv = 0.00f, pyhu = 0.50f, pyhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, pylu, pyhu);
			var hu = (float) Mth.lerp(hpa, pylu, pyhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, pylv, pyhv);
				var hv = (float) Mth.lerp(hpb, pylv, pyhv);
				normSphereVertex(builder, camera, center, radius, ha, 1, lb, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, 1, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, 1, hb, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, 1, hb, hu, hv);
			}
		}

		// -Z
		double nzlu = 1.00f, nzlv = 0.50f, nzhu = 0.75f, nzhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, nzlu, nzhu);
			var hu = (float) Mth.lerp(hpa, nzlu, nzhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, nzlv, nzhv);
				var hv = (float) Mth.lerp(hpb, nzlv, nzhv);
				normSphereVertex(builder, camera, center, radius, ha, lb, -1, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, lb, -1, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, hb, -1, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, hb, -1, hu, hv);
			}
		}

		// +Z
		double pzlu = 0.25f, pzlv = 0.50f, pzhu = 0.50f, pzhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, pzlu, pzhu);
			var hu = (float) Mth.lerp(hpa, pzlu, pzhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, pzlv, pzhv);
				var hv = (float) Mth.lerp(hpb, pzlv, pzhv);
				normSphereVertex(builder, camera, center, radius, ha, lb, 1, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, lb, 1, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, hb, 1, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, hb, 1, hu, hv);
			}
		}
	}

	private static void ringVertex(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3 center, double x, double y, double z, float u, float v) {
		final var pos = new Vec3(x, 0, z);
		var norm = y > 0 ? Vec3.YN : Vec3.YP;
		norm = norm.normalize();
		final var p = camera.toCameraSpace(pos).add(center);
		builder.vertex(p).uv0(u, v).color(Color.WHITE).normal(norm).endVertex();
	}

	private static void normSphereVertex(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3 center, double radius, double x, double y, double z,
			float u, float v) {
		final var pos = new Vec3(x, y, z);
		final var n = pos.normalize();
		final var p = camera.toCameraSpace(n.mul(radius)).add(center);
		builder.vertex(p).normal(n).uv0(u, v).color(Color.WHITE).endVertex();
	}

	// private static void cubeVertex(VertexConsumer builder, CachedCamera<?>
	// camera, PoseStack.Pose pose, Color color,
	// double x, double y, double z, double nx, double ny, double nz, float u, float
	// v) {
	// final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
	// builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
	// .uv(u, v)
	// .color(r, g, b, a)
	// .normal(pose.normal(), (float) nx, (float) ny, (float) nz)
	// .endVertex();
	// }

}
