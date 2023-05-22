package net.xavil.ultraviolet.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.ultraviolet.Mod;
import static net.xavil.ultraviolet.client.Shaders.*;
import static net.xavil.ultraviolet.client.DrawStates.*;
import net.xavil.ultraviolet.client.ModRendering;
import net.xavil.ultraviolet.client.camera.CachedCamera;
import net.xavil.ultraviolet.client.camera.OrbitCamera;
import net.xavil.ultraviolet.client.flexible.FlexibleBufferBuilder;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexConsumer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
import net.xavil.ultraviolet.client.gl.DrawState;
import net.xavil.ultraviolet.client.gl.GlState;
import net.xavil.ultraviolet.client.gl.GlManager;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.matrices.Mat4;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.interfaces.Vec3Access;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
	public static final ResourceLocation GALAXY_GLOW_LOCATION = Mod.namespaced("textures/misc/galaxyglow.png");
	public static final ResourceLocation SELECTION_CIRCLE_ICON_LOCATION = Mod
			.namespaced("textures/misc/selection_circle.png");

	public static final ResourceLocation BASE_ROCKY_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_rocky.png");
	public static final ResourceLocation BASE_WATER_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_water.png");
	public static final ResourceLocation FEATURE_CRATERS_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/craters.png");
	public static final ResourceLocation FEATURE_EARTH_LIKE_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/earth_like.png");

	private static final Minecraft CLIENT = Minecraft.getInstance();

	public static final DrawState DRAW_STATE_STAR_BILLBOARD = DrawState.builder()
			.depthMask(false)
			.enableDepthTest(GlState.DepthFunc.LESS)
			.enableCulling(false)
			.enableAdditiveBlending()
			.build();

	public static void renderStarBillboard(FlexibleBufferBuilder builder, CachedCamera<?> camera, TransformStack tfm,
			CelestialNode node) {
		builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);
		addBillboard(builder, camera, tfm, node);
		builder.end();
		final var shader = getShader(SHADER_STAR_BILLBOARD);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(STAR_ICON_LOCATION));
		builder.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, TransformStack tfm,
			CelestialNode node) {
		final var partialTick = CLIENT.getFrameTime();
		final var nodePos = node.getPosition(partialTick);
		final var np = tfm.applyTransform(nodePos.mul(1e12 / camera.metersPerUnit), 1);
		double d = (1e12 / camera.metersPerUnit) * getCelestialBodySize(camera.posTm, node, np);
		// addBillboard(builder, camera, tfm, node, d, nodePos.mul(1e12 /
		// camera.metersPerUnit));
		addBillboard(builder, camera, node, d, np);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, CelestialNode node,
			Vec3 pos) {
		final var distanceFromCameraTm = camera.posTm.distanceTo(pos);

		double minAngularDiameterRad = Math.toRadians(0.15);

		// how many times we need to scale the billboard by to render the star at the
		// correct size. if the billboard texture were a circle with a diameter of the
		// size of the image, then this would be 1. if the diameter were 1/2 the size,
		// then this would be 2.
		final double billboardFactor = 9.0;

		double radius = 0;
		if (node instanceof StellarCelestialNode starNode) {
			// radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e8;
			radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e12;
			// if (radius > 1) radius = 1;
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			radius = planetNode.radiusRearth * Units.m_PER_Rearth / 1e12;
			// if (planetNode.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
			// radius *= 2e3;
			// }
		}

		final var idealAngularRadius = (radius / distanceFromCameraTm);
		var angularRadius = Math.max(idealAngularRadius, minAngularDiameterRad / 2);
		double k = 1.0;

		if (node instanceof StellarCelestialNode starNode) {
			final var brightnessThreshold = 100;
			final var apparentBrightness = 1e11 * starNode.luminosityLsol
					/ (4 * Math.PI * distanceFromCameraTm * distanceFromCameraTm);

			if (apparentBrightness > brightnessThreshold && angularRadius > idealAngularRadius) {
				angularRadius *= Math.pow(apparentBrightness / brightnessThreshold, 0.1);
				angularRadius = Math.min(angularRadius, Math.toRadians(0.1));
			} else if (angularRadius > idealAngularRadius) {
				k *= Math.pow(apparentBrightness / brightnessThreshold, 0.08);
			}
		}

		final var d = billboardFactor * distanceFromCameraTm * (angularRadius / 2);

		var color = Color.WHITE;
		if (node instanceof StellarCelestialNode starNode) {
			color = starNode.getColor().withA(k);
		}

		RenderHelper.addBillboardWorldspace(builder, camera.pos, camera.up, camera.left,
				pos.mul(1e12 / camera.metersPerUnit), d, color);
	}

	// public static void addBillboard(FlexibleVertexConsumer builder, Vec3 viewPos,
	// CelestialNode node, Vec3 pos) {
	// final var distanceFromCameraTm = viewPos.distanceTo(pos);

	// double minAngularDiameterRad = Math.toRadians(0.15);

	// // how many times we need to scale the billboard by to render the star at the
	// // correct size. if the billboard texture were a circle with a diameter of
	// the
	// // size of the image, then this would be 1. if the diameter were 1/2 the
	// size,
	// // then this would be 2.
	// final double billboardFactor = 18.0;

	// double radius = 0;
	// if (node instanceof StellarCelestialNode starNode) {
	// // radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e8;
	// radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e12;
	// // if (radius > 1) radius = 1;
	// } else if (node instanceof PlanetaryCelestialNode planetNode) {
	// radius = planetNode.radiusRearth * Units.m_PER_Rearth / 1e12;
	// // if (planetNode.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
	// // radius *= 2e3;
	// // }
	// }

	// final var idealAngularRadius = (radius / distanceFromCameraTm);
	// var angularRadius = Math.max(idealAngularRadius, minAngularDiameterRad / 2);
	// double k = 1.0;

	// if (node instanceof StellarCelestialNode starNode) {
	// final var brightnessThreshold = 100;
	// final var apparentBrightness = 1e11 * starNode.luminosityLsol
	// / (4 * Math.PI * distanceFromCameraTm * distanceFromCameraTm);

	// if (apparentBrightness > brightnessThreshold && angularRadius >
	// idealAngularRadius) {
	// angularRadius *= Math.pow(apparentBrightness / brightnessThreshold, 0.1);
	// angularRadius = Math.min(angularRadius, Math.toRadians(0.1));
	// } else {
	// k *= Math.pow(apparentBrightness / brightnessThreshold, 0.08);
	// }
	// }

	// final var d = billboardFactor * distanceFromCameraTm * (angularRadius / 2);

	// var color = Color.WHITE;
	// if (node instanceof StellarCelestialNode starNode) {
	// color = starNode.getColor().withA(k);
	// }

	// final var forward =
	// final var up = pos.sub(viewPos).cross(Vec3.XP);
	// final var left = pos.sub(viewPos).cross(Vec3.XP);

	// RenderHelper.addBillboardWorldspace(builder, viewPos, camera.up, camera.left,
	// pos.mul(1e12 / camera.metersPerUnit), d, color);
	// }

	public static double getCelestialBodySize(Vec3 camPos, CelestialNode node, Vec3 pos) {
		final var distanceFromCameraTm = camPos.distanceTo(pos);

		double minAngularDiameterRad = Math.toRadians(0.1);
		if (node instanceof StellarCelestialNode starNode) {
			final var apparentBrightness = 1e4 * starNode.luminosityLsol
					/ (4 * Math.PI * distanceFromCameraTm * distanceFromCameraTm);
			final var s = apparentBrightness;
			minAngularDiameterRad = Math.max(minAngularDiameterRad, Math.min(s, Math.toRadians(0.3)));
		}

		// how many times we need to scale the billboard by to render the star at the
		// correct size. if the billboard texture were a circle with a diameter of the
		// size of the image, then this would be 1. if the diameter were 1/2 the size,
		// then this would be 2.
		final double billboardFactor = 16.0;

		double radius = 0;
		if (node instanceof StellarCelestialNode starNode) {
			// radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e8;
			radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e12;
			// if (radius > 1) radius = 1;
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			radius = planetNode.radiusRearth * Units.m_PER_Rearth / 1e12;
			// if (planetNode.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
			// radius *= 2e3;
			// }
		}

		var angularRadius = (radius / distanceFromCameraTm);
		angularRadius = Math.max(angularRadius, minAngularDiameterRad / 2);
		return billboardFactor * distanceFromCameraTm * (angularRadius / 2);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, CelestialNode node,
			double d, Vec3 pos) {

		var color = Color.WHITE;
		if (node instanceof StellarCelestialNode starNode) {
			color = starNode.getColor();
		}

		RenderHelper.addBillboardWorldspace(builder, camera.pos, camera.up, camera.left, pos, d, color);
		// RenderHelper.addBillboardWorldspace(builder, camera.pos, tfm, camera.up,
		// camera.left, pos, d, color);
	}

	public static void renderUiBillboard(DrawState drawState, FlexibleBufferBuilder builder,
			CachedCamera<?> camera, TransformStack tfm,
			Vec3 center, double scale, Color color, ResourceLocation texture, ShaderProgram shader) {
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		addBillboard(builder, camera, tfm, center, scale, color);
		builder.end();
		// CLIENT.getTextureManager().getTexture(texture).setFilter(true, false);
		// RenderSystem.setShaderTexture(0, texture);
		GlManager.enableBlend(true);
		GlManager.blendFunc(GlState.BlendFactor.SRC_ALPHA, GlState.BlendFactor.ONE);
		GlManager.depthMask(false);
		GlManager.enableDepthTest(false);
		GlManager.enableCull(false);
		builder.draw(shader, drawState);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, TransformStack tfm,
			Vec3 center, double scale, Color color) {
		addBillboardWorldspace(builder, camera.pos, tfm, camera.up, camera.left, center, scale, color);
	}

	public static void addBillboardCamspace(FlexibleVertexConsumer builder, TransformStack tfm, Vec3 up,
			Vec3 right, Vec3 center, double scale, Color color) {
		// final var bu = up.mul(scale);
		// final var br = right.mul(scale);

		final var p = tfm.get();
		final var n = new Vec3.Mutable(0, 0, 0);

		n.load(Vec3.ZERO).addAssign(right).subAssign(up);
		n.mulAssign(scale).addAssign(center);
		Mat4.transform(n, 1, p);
		builder.vertex(n).color(color).uv0(1, 0).endVertex();
		n.load(Vec3.ZERO).subAssign(right).subAssign(up);
		n.mulAssign(scale).addAssign(center);
		Mat4.transform(n, 1, p);
		builder.vertex(n).color(color).uv0(0, 0).endVertex();
		n.load(Vec3.ZERO).subAssign(right).addAssign(up);
		n.mulAssign(scale).addAssign(center);
		Mat4.transform(n, 1, p);
		builder.vertex(n).color(color).uv0(0, 1).endVertex();
		n.load(Vec3.ZERO).addAssign(right).addAssign(up);
		n.mulAssign(scale).addAssign(center);
		Mat4.transform(n, 1, p);
		builder.vertex(n).color(color).uv0(1, 1).endVertex();

		// v.load(up).addAssign(right).mulAssign(scale).addAssign(center);
		// v.load(up).addAssign(right).negAssign().mulAssign(scale).addAssign(center);
		// v.load(up).negAssign().addAssign(right).mulAssign(scale).addAssign(center);
		// v.load(right).negAssign().addAssign(up).mulAssign(scale).addAssign(center);

		// final var qhl = center.add(bu).sub(br).transformBy(p);
		// final var qll = center.sub(bu).sub(br).transformBy(p);
		// final var qlh = center.sub(bu).add(br).transformBy(p);
		// final var qhh = center.add(bu).add(br).transformBy(p);
		// builder.vertex(qhl).color(color).uv0(1, 0).endVertex();
		// builder.vertex(qll).color(color).uv0(0, 0).endVertex();
		// builder.vertex(qlh).color(color).uv0(0, 1).endVertex();
		// builder.vertex(qhh).color(color).uv0(1, 1).endVertex();
	}

	public static void addBillboardCamspace(FlexibleVertexConsumer builder, Vec3 up,
			Vec3 right, Vec3 center, double scale, Color color) {
		final var n = new Vec3.Mutable(0, 0, 0);
		n.load(Vec3.ZERO).addAssign(right).subAssign(up);
		n.mulAssign(scale).addAssign(center);
		builder.vertex(n).color(color).uv0(1, 0).endVertex();
		n.load(Vec3.ZERO).subAssign(right).subAssign(up);
		n.mulAssign(scale).addAssign(center);
		builder.vertex(n).color(color).uv0(0, 0).endVertex();
		n.load(Vec3.ZERO).subAssign(right).addAssign(up);
		n.mulAssign(scale).addAssign(center);
		builder.vertex(n).color(color).uv0(0, 1).endVertex();
		n.load(Vec3.ZERO).addAssign(right).addAssign(up);
		n.mulAssign(scale).addAssign(center);
		builder.vertex(n).color(color).uv0(1, 1).endVertex();
	}

	public static void addBillboardWorldspace(FlexibleVertexConsumer builder, Vec3Access camPos,
			TransformStack tfm, Vec3Access up, Vec3Access right, Vec3Access center, double scale, Color color) {
		final var p = tfm.get();
		final var n = new Vec3.Mutable(0, 0, 0);

		n.load(Vec3.ZERO).addAssign(center).subAssign(camPos);
		Mat4.transform(n, 1, p);
		builder.vertex(n).color(color).uv0((float) scale, 0).endVertex();

		// n.load(Vec3.ZERO).addAssign(right).subAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// Mat4.transform(n, 1, p);
		// builder.vertex(n).color(color).uv0(1, 0).endVertex();

		// n.load(Vec3.ZERO).subAssign(right).subAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// Mat4.transform(n, 1, p);
		// builder.vertex(n).color(color).uv0(0, 0).endVertex();

		// n.load(Vec3.ZERO).subAssign(right).addAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// Mat4.transform(n, 1, p);
		// builder.vertex(n).color(color).uv0(0, 1).endVertex();

		// n.load(Vec3.ZERO).addAssign(right).addAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// Mat4.transform(n, 1, p);
		// builder.vertex(n).color(color).uv0(1, 1).endVertex();
	}

	public static void addBillboardWorldspace(FlexibleVertexConsumer builder, Vec3Access camPos, Vec3Access up,
			Vec3Access right, Vec3Access center, double scale, Color color) {
		final var n = new Vec3.Mutable(0, 0, 0);

		n.load(Vec3.ZERO).addAssign(center).subAssign(camPos);
		builder.vertex(n).color(color).uv0((float) scale, 0).endVertex();
		// builder.vertex(n).color(color).uv0(1, 0).endVertex();
		// builder.vertex(n).color(color).uv0(1, 0).endVertex();
		// builder.vertex(n).color(color).uv0(1, 0).endVertex();

		// n.load(Vec3.ZERO).addAssign(right).subAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// builder.vertex(n).color(color).uv0(1, 0).endVertex();

		// n.load(Vec3.ZERO).subAssign(right).subAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// builder.vertex(n).color(color).uv0(0, 0).endVertex();

		// n.load(Vec3.ZERO).subAssign(right).addAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// builder.vertex(n).color(color).uv0(0, 1).endVertex();

		// n.load(Vec3.ZERO).addAssign(right).addAssign(up);
		// n.mulAssign(scale).addAssign(center).subAssign(camPos);
		// builder.vertex(n).color(color).uv0(1, 1).endVertex();
	}

	public static void addBillboardWorldspaceFacing(FlexibleVertexConsumer builder,
			Vec3Access viewPos, Vec3Access center, double angle, Color color) {
	}
	// public static void addBillboardWorldspaceFacing(FlexibleVertexConsumer
	// builder,
	// Vec3Access viewPos, Vec3Access center, double angle, Color color) {

	// final var offset = center.sub(viewPos);
	// final var forward = offset.normalize();

	// final var rotationAngle = rng.uniformDouble(0, 2.0 * Math.PI);
	// final var rotation = Quat.axisAngle(forward, rotationAngle);

	// final var du = forward.dot(Vec3.YP);
	// final var df = forward.dot(Vec3.ZN);
	// final var v1 = Math.abs(du) < Math.abs(df) ? Vec3.YP : Vec3.ZN;
	// final var right = rotation.transform(v1.cross(forward).neg());
	// final var up = forward.cross(right).neg();
	// // RenderHelper.addBillboardCamspace(builder, up, right, forward.mul(100), s,
	// 0,
	// // Color.WHITE.withA(0.1));
	// RenderHelper.addBillboardCamspace(builder, up, right, offset, offset.length()
	// * s, Color.WHITE.withA(0.1));

	// final var n = new Vec3.Mutable(0, 0, 0);

	// n.load(Vec3.ZERO).addAssign(right).subAssign(up);
	// n.mulAssign(scale).addAssign(center).subAssign(camPos);
	// builder.vertex(n).color(color).uv0(1, 0).endVertex();

	// n.load(Vec3.ZERO).subAssign(right).subAssign(up);
	// n.mulAssign(scale).addAssign(center).subAssign(camPos);
	// builder.vertex(n).color(color).uv0(0, 0).endVertex();

	// n.load(Vec3.ZERO).subAssign(right).addAssign(up);
	// n.mulAssign(scale).addAssign(center).subAssign(camPos);
	// builder.vertex(n).color(color).uv0(0, 1).endVertex();

	// n.load(Vec3.ZERO).addAssign(right).addAssign(up);
	// n.mulAssign(scale).addAssign(center).subAssign(camPos);
	// builder.vertex(n).color(color).uv0(1, 1).endVertex();
	// }

	public static double getGridScale(OrbitCamera.Cached camera, double tmPerUnit, double scaleFactor,
			float partialTick) {
		var currentThreshold = tmPerUnit;
		var scale = tmPerUnit;
		for (var i = 0; i < 10; ++i) {
			currentThreshold *= scaleFactor;
			if (camera.camera.scale.get(partialTick) > currentThreshold)
				scale = currentThreshold;
		}
		return scale;
	}

	public static void renderGrid(FlexibleBufferBuilder builder, OrbitCamera.Cached camera,
			double tmPerUnit, double gridUnits, int scaleFactor, int gridLineCount, float partialTick) {
		var focusPos = camera.focus.div(tmPerUnit);
		var gridScale = getGridScale(camera, gridUnits, scaleFactor, partialTick);
		renderGrid(builder, camera, focusPos, gridScale * gridLineCount, scaleFactor, gridLineCount);
	}

	public static final DrawState GRID_STATE = DrawState.builder()
			.depthMask(false)
			.enableCulling(false)
			.enableAdditiveBlending()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.build();

	public static void renderGrid(FlexibleBufferBuilder builder, CachedCamera<?> camera,
			Vec3 focusPos, double gridDiameter, int subcellsPerCell, int gridLineCount) {
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		addGrid(builder, camera, focusPos, gridDiameter, subcellsPerCell, gridLineCount);
		builder.end();
		RenderSystem.lineWidth(1);
		builder.draw(getVanillaShader(SHADER_VANILLA_RENDERTYPE_LINES), GRID_STATE);
	}

	public static void addGrid(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 focusPos,
			double gridDiameter, int subcellsPerCell, int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.5f, g = 0.5f, b = 0.5f, a1 = 0.1f, a2 = 0.33f;
		var color = new Color(r, g, b, 1);
		final double gridFadeFactor = 2.3;

		var gridOffset = gridCellResolution * gridLineCount / 2;

		// NOTE: each line needs to be divided into sections, because the lines will
		// become distorted if they are too long.
		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridCellResolution - gridOffset;
			double lx = gridMinX - gridOffset, hx = lx + gridDiameter;

			var zMark = (int) Math.floor(gridMinZ / gridCellResolution + i - gridLineCount / 2);
			var la = zMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				var lp = Vec3.from(Mth.lerp(lt, lx, hx), focusPos.y, z);
				var hp = Vec3.from(Mth.lerp(ht, lx, hx), focusPos.y, z);

				var ld = lp.distanceTo(focusPos);
				var hd = hp.distanceTo(focusPos);
				if (ld <= gridDiameter / gridFadeFactor) {
					var rla = la * Mth.clamp(5 * Mth.inverseLerp(ld, gridDiameter / gridFadeFactor, 0), 0, 1);
					var rha = la * Mth.clamp(5 * Mth.inverseLerp(hd, gridDiameter / gridFadeFactor, 0), 0, 1);
					var start = camera.toCameraSpace(Vec3.from(Mth.lerp(lt, lx, hx), focusPos.y, z));
					var end = camera.toCameraSpace(Vec3.from(Mth.lerp(ht, lx, hx), focusPos.y, z));
					addLine(builder, start, end, color.withA(rla), color.withA(rha));
				}
			}
		}
		// Z
		for (var i = 1; i < gridLineCount; ++i) {
			var x = gridMinX + i * gridCellResolution - gridOffset;
			double lz = gridMinZ - gridOffset, hz = lz + gridDiameter;

			var xMark = (int) Math.floor(gridMinX / gridCellResolution + i - gridLineCount / 2);
			var la = xMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				var lp = Vec3.from(x, focusPos.y, Mth.lerp(lt, lz, hz));
				var hp = Vec3.from(x, focusPos.y, Mth.lerp(ht, lz, hz));

				var ld = lp.distanceTo(focusPos);
				var hd = hp.distanceTo(focusPos);
				if (ld <= gridDiameter / gridFadeFactor) {
					var rla = la * Mth.clamp(5 * Mth.inverseLerp(ld, gridDiameter / gridFadeFactor, 0), 0, 1);
					var rha = la * Mth.clamp(5 * Mth.inverseLerp(hd, gridDiameter / gridFadeFactor, 0), 0, 1);
					var start = camera.toCameraSpace(Vec3.from(x, focusPos.y, Mth.lerp(lt, lz, hz)));
					var end = camera.toCameraSpace(Vec3.from(x, focusPos.y, Mth.lerp(ht, lz, hz)));
					addLine(builder, start, end, color.withA(rla), color.withA(rha));
				}
			}
		}

	}

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 start, Vec3 end,
			Color color) {
		addLine(builder, camera, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, Color color) {
		addLine(builder, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 start, Vec3 end,
			Color startColor,
			Color endColor) {
		addLine(builder, camera.toCameraSpace(start), camera.toCameraSpace(end), startColor, endColor);
	}

	public static void addQuad(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
			Color color) {
		addQuad(builder, camera.toCameraSpace(a),
				camera.toCameraSpace(b), camera.toCameraSpace(c), camera.toCameraSpace(d), color);
	}

	public static void addQuad(FlexibleVertexConsumer builder, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Color color) {
		builder.vertex(a).color(color).endVertex();
		builder.vertex(b).color(color).endVertex();
		builder.vertex(c).color(color).endVertex();
		builder.vertex(d).color(color).endVertex();
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, Color startColor, Color endColor) {
		var normal = end.sub(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

	public static void addAxisAlignedBox(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 p0, Vec3 p1,
			Color color) {
		p0 = camera.toCameraSpace(p0);
		p1 = camera.toCameraSpace(p1);
		double lx = p0.x < p1.x ? p0.x : p1.x;
		double ly = p0.y < p1.y ? p0.y : p1.y;
		double lz = p0.z < p1.z ? p0.z : p1.z;
		double hx = p0.x >= p1.x ? p0.x : p1.x;
		double hy = p0.y >= p1.y ? p0.y : p1.y;
		double hz = p0.z >= p1.z ? p0.z : p1.z;

		// X axis
		builder.vertex(lx, ly, lz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, lz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(lx, ly, hz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, hz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, lz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, lz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, hz).color(color).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, hz).color(color).normal(1, 0, 0).endVertex();
		// Y axis
		builder.vertex(lx, ly, lz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, lz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(lx, ly, hz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, hz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, lz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, lz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, hz).color(color).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, hz).color(color).normal(0, 1, 0).endVertex();
		// Z axis
		builder.vertex(lx, ly, lz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(lx, ly, hz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, lz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, hz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, lz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, hz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, lz).color(color).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, hz).color(color).normal(0, 0, 1).endVertex();
	}

}