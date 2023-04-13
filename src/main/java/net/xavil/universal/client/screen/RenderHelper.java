package net.xavil.universal.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universal.client.flexible.FlexibleVertexConsumer;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec3;

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

	public static void renderStarBillboard(FlexibleBufferBuilder builder, CachedCamera<?> camera, PoseStack poseStack,
			CelestialNode node) {
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		addBillboard(builder, camera, poseStack, node);
		builder.end();
		CLIENT.getTextureManager().getTexture(STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack,
			CelestialNode node) {
		double d = 1 * (1e12 / camera.metersPerUnit)
				* getCelestialBodySize(camera.pos.mul(camera.metersPerUnit / 1e12), node, node.position);
		addBillboard(builder, camera, poseStack, node, d, node.position);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack,
			CelestialNode node, Vec3 pos) {
		double d = 1 * (1e12 / camera.metersPerUnit)
				* getCelestialBodySize(camera.pos.mul(camera.metersPerUnit / 1e12), node, pos);
		addBillboard(builder, camera, poseStack, node, d, pos);
	}

	public static double getCelestialBodySize(Vec3 camPos, CelestialNode node, Vec3 pos) {

		double minAngularDiameterRad = Math.toRadians(0.5);
		if (node instanceof StellarCelestialNode starNode) {
			minAngularDiameterRad = Math.toRadians(Mth.clamp(Math.log(starNode.luminosityLsol), 0.5, 0.7));
		}

		// how many times we need to scale the billboard by to render the star at the
		// correct size. if the billboard texture were a circle with a diameter of the
		// size of the image, then this would be 1. if the diameter were 1/2 the size,
		// then this would be 2.
		final double billboardFactor = 3.5;

		double radius = 0;
		if (node instanceof StellarCelestialNode starNode) {
			radius = starNode.radiusRsol * Units.m_PER_Rsol / 1e8;
			if (radius > 3) radius = 3;
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			radius = planetNode.radiusRearth * Units.m_PER_Rearth / 1e12;
		}

		var distanceFromCameraTm = camPos.distanceTo(pos);
		var angularRadius = (radius / distanceFromCameraTm);
		angularRadius = Math.max(angularRadius, minAngularDiameterRad / 2);
		return billboardFactor * distanceFromCameraTm * (angularRadius / 2);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack,
			CelestialNode node, double d, Vec3 pos) {

		var color = Color.WHITE;
		if (node instanceof StellarCelestialNode starNode) {
			color = starNode.getColor();
		}

		RenderHelper.addBillboardCamspace(builder, poseStack, camera.up, camera.right.neg(),
				camera.toCameraSpace(pos), d, 0, color);
	}

	public static void renderBillboard(FlexibleBufferBuilder builder, CachedCamera<?> camera, PoseStack poseStack,
		Vec3 center, double scale, Color color, ResourceLocation texture, ShaderInstance shader) {
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		addBillboard(builder, camera, poseStack, center, scale, color);
		builder.end();
		CLIENT.getTextureManager().getTexture(texture).setFilter(true, false);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		builder.draw(shader);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack,
			Vec3 center, double scale, Color color) {
		addBillboardCamspace(builder, poseStack, camera.up, camera.right.neg(),
				camera.toCameraSpace(center), scale, 0, color);
	}

	public static void addBillboard(FlexibleVertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack,
			Vec3 up, Vec3 right, Vec3 center, double scale, double zOffset, Color color) {
		addBillboardCamspace(builder, poseStack, camera.toCameraSpace(up), camera.toCameraSpace(right),
				camera.toCameraSpace(center), scale, zOffset, color);
	}

	public static void addBillboardCamspace(FlexibleVertexConsumer builder, PoseStack poseStack, Vec3 up,
			Vec3 right, Vec3 center, double scale, double zOffset, Color color) {
		var backwards = up.cross(right).mul(zOffset);
		var bu = up.mul(scale);
		var br = right.mul(scale);

		var p = poseStack.last().pose();
		var qll = center.sub(bu).sub(br).add(backwards).transformBy(p);
		var qlh = center.sub(bu).add(br).add(backwards).transformBy(p);
		var qhl = center.add(bu).sub(br).add(backwards).transformBy(p);
		var qhh = center.add(bu).add(br).add(backwards).transformBy(p);
		builder.vertex(qhl).color(color).uv0(1, 0).endVertex();
		builder.vertex(qll).color(color).uv0(0, 0).endVertex();
		builder.vertex(qlh).color(color).uv0(0, 1).endVertex();
		builder.vertex(qhh).color(color).uv0(1, 1).endVertex();
	}

	public static void addBillboardCamspace(FlexibleVertexConsumer builder, Vec3 up, Vec3 right, Vec3 center,
			double scale, double zOffset, Color color) {
		var backwards = up.cross(right).mul(zOffset);
		var bu = up.mul(scale);
		var br = right.mul(scale);

		var qll = center.sub(bu).sub(br).add(backwards);
		var qlh = center.sub(bu).add(br).add(backwards);
		var qhl = center.add(bu).sub(br).add(backwards);
		var qhh = center.add(bu).add(br).add(backwards);
		builder.vertex(qhl).color(color).uv0(1, 0).endVertex();
		builder.vertex(qll).color(color).uv0(0, 0).endVertex();
		builder.vertex(qlh).color(color).uv0(0, 1).endVertex();
		builder.vertex(qhh).color(color).uv0(1, 1).endVertex();
	}

	// public static void addBillboard(VertexConsumer builder, CachedCamera<?>
	// camera, PoseStack poseStack, Vec3 center,
	// Vec3 up, Vec3 right, Vec3 forward, Color color) {
	// var p = poseStack.last().pose();
	// var qll = center.sub(up).sub(right).add(forward);
	// var qlh = center.sub(up).add(right).add(forward);
	// var qhl = center.add(up).sub(right).add(forward);
	// var qhh = center.add(up).add(right).add(forward);
	// float r = color.r(), g = color.g(), b = color.b(), a = color.a();
	// builder.vertex(p, (float) qhl.x, (float) qhl.y, (float) qhl.z).color(r, g, b,
	// a).uv(1, 0).endVertex();
	// builder.vertex(p, (float) qll.x, (float) qll.y, (float) qll.z).color(r, g, b,
	// a).uv(0, 0).endVertex();
	// builder.vertex(p, (float) qlh.x, (float) qlh.y, (float) qlh.z).color(r, g, b,
	// a).uv(0, 1).endVertex();
	// builder.vertex(p, (float) qhh.x, (float) qhh.y, (float) qhh.z).color(r, g, b,
	// a).uv(1, 1).endVertex();
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

	public static void renderGrid(FlexibleBufferBuilder builder, OrbitCamera.Cached camera, double tmPerUnit,
			double gridUnits,
			int scaleFactor, int gridLineCount, float partialTick) {
		var focusPos = camera.focus.div(tmPerUnit);
		var gridScale = getGridScale(camera, gridUnits, scaleFactor, partialTick);
		renderGrid(builder, camera, focusPos, gridScale * gridLineCount, scaleFactor, gridLineCount);
	}

	public static void renderGrid(FlexibleBufferBuilder builder, CachedCamera<?> camera, Vec3 focusPos,
			double gridDiameter,
			int subcellsPerCell, int gridLineCount) {
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		addGrid(builder, camera, focusPos, gridDiameter, subcellsPerCell, gridLineCount);
		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		builder.draw(GameRenderer.getRendertypeLinesShader());
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

	// public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end,
	// double lineWidth, Color color) {
	// renderLine(builder, start, end, lineWidth, color, color);
	// }

	// public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end,
	// double lineWidth, Color startColor,
	// Color endColor) {
	// RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
	// builder.begin(VertexFormat.Mode.LINES,
	// DefaultVertexFormat.POSITION_COLOR_NORMAL);
	// var normal = end.sub(start).normalize();
	// builder.vertex(start.x, start.y, start.z)
	// .color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
	// .normal((float) normal.x, (float) normal.y, (float) normal.z)
	// .endVertex();
	// builder.vertex(end.x, end.y, end.z)
	// .color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
	// .normal((float) normal.x, (float) normal.y, (float) normal.z)
	// .endVertex();
	// builder.end();
	// RenderSystem.enableBlend();
	// RenderSystem.disableTexture();
	// RenderSystem.defaultBlendFunc();
	// RenderSystem.disableCull();
	// RenderSystem.depthMask(false);
	// RenderSystem.lineWidth((float) lineWidth);
	// BufferUploader.end(builder);
	// }

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
