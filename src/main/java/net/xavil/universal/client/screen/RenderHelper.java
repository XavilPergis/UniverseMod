package net.xavil.universal.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
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

	private static ResourceLocation getBaseLayer(PlanetNode node) {
		var missing = MissingTextureAtlasSprite.getLocation();
		return switch (node.type) {
			case EARTH_LIKE_WORLD -> BASE_WATER_LOCATION;
			case GAS_GIANT -> missing;
			case ICE_WORLD -> missing;
			case ROCKY_ICE_WORLD -> missing;
			case ROCKY_WORLD -> BASE_ROCKY_LOCATION;
			case WATER_WORLD -> BASE_WATER_LOCATION;
			default -> missing;
		};
	}

	public static void renderPlanet(BufferBuilder builder, PlanetNode node, Vec3 camPos, double scale,
			PoseStack poseStack, Vec3 center, Color tintColor) {
		RenderSystem.defaultBlendFunc();
		// RenderSystem.depthMask(true);
		// RenderSystem.enableDepthTest();
		var baseTexture = getBaseLayer(node);

		double d = 0.02 * getCelestialBodySize(node, camPos, center);

		var radius = scale * Math.cbrt((node.massYg / Units.mearth(1)) / 1);
		renderTexturedCube(builder, baseTexture, poseStack, center, radius * d, tintColor);
		if (node.type == PlanetNode.Type.EARTH_LIKE_WORLD) {
			renderTexturedCube(builder, FEATURE_EARTH_LIKE_LOCATION, poseStack, center, radius * d, tintColor);
		}
	}

	private static void renderTexturedCube(BufferBuilder builder, ResourceLocation texture, PoseStack poseStack,
			Vec3 center, double radius, Color tintColor) {
		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		addCube(builder, poseStack, center, radius, tintColor);
		builder.end();
		RenderSystem.setShaderTexture(0, texture);
		BufferUploader.end(builder);
	}

	private static void addCube(VertexConsumer builder, PoseStack poseStack, Vec3 center, double radius,
			Color tintColor) {

		final double nr = -radius, pr = radius;
		final var nnn = center.add(nr, nr, nr);
		final var nnp = center.add(nr, nr, pr);
		final var npn = center.add(nr, pr, nr);
		final var npp = center.add(nr, pr, pr);
		final var pnn = center.add(pr, nr, nr);
		final var pnp = center.add(pr, nr, pr);
		final var ppn = center.add(pr, pr, nr);
		final var ppp = center.add(pr, pr, pr);

		final var pose = poseStack.last();

		// -X
		cubeVertex(builder, pose, tintColor, npn.x, npn.y, npn.z, -1, 0, 0, 0.00f, 0.25f);
		cubeVertex(builder, pose, tintColor, nnn.x, nnn.y, nnn.z, -1, 0, 0, 0.00f, 0.50f);
		cubeVertex(builder, pose, tintColor, nnp.x, nnp.y, nnp.z, -1, 0, 0, 0.25f, 0.50f);
		cubeVertex(builder, pose, tintColor, npp.x, npp.y, npp.z, -1, 0, 0, 0.25f, 0.25f);
		// +X
		cubeVertex(builder, pose, tintColor, pnn.x, pnn.y, pnn.z, 1, 0, 0, 0.75f, 0.50f);
		cubeVertex(builder, pose, tintColor, ppn.x, ppn.y, ppn.z, 1, 0, 0, 0.75f, 0.25f);
		cubeVertex(builder, pose, tintColor, ppp.x, ppp.y, ppp.z, 1, 0, 0, 0.50f, 0.25f);
		cubeVertex(builder, pose, tintColor, pnp.x, pnp.y, pnp.z, 1, 0, 0, 0.50f, 0.50f);
		// -Y
		cubeVertex(builder, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, -1, 0, 0.25f, 0.75f);
		cubeVertex(builder, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, -1, 0, 0.50f, 0.75f);
		cubeVertex(builder, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, -1, 0, 0.50f, 0.50f);
		cubeVertex(builder, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, -1, 0, 0.25f, 0.50f);
		// +Y
		cubeVertex(builder, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 1, 0, 0.50f, 0.00f);
		cubeVertex(builder, pose, tintColor, npn.x, npn.y, npn.z, 0, 1, 0, 0.25f, 0.00f);
		cubeVertex(builder, pose, tintColor, npp.x, npp.y, npp.z, 0, 1, 0, 0.25f, 0.25f);
		cubeVertex(builder, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 1, 0, 0.50f, 0.25f);
		// -Z
		cubeVertex(builder, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, 0, -1, 0.75f, 0.50f);
		cubeVertex(builder, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, 0, -1, 1.00f, 0.50f);
		cubeVertex(builder, pose, tintColor, npn.x, npn.y, npn.z, 0, 0, -1, 1.00f, 0.25f);
		cubeVertex(builder, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 0, -1, 0.75f, 0.25f);
		// +Z
		cubeVertex(builder, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, 0, 1, 0.25f, 0.50f);
		cubeVertex(builder, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, 0, 1, 0.50f, 0.50f);
		cubeVertex(builder, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 0, 1, 0.50f, 0.25f);
		cubeVertex(builder, pose, tintColor, npp.x, npp.y, npp.z, 0, 0, 1, 0.25f, 0.25f);
	}

	private static void cubeVertex(VertexConsumer builder, PoseStack.Pose pose, Color color,
			double x, double y, double z, double nx, double ny, double nz, float u, float v) {
		final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
		builder
				.vertex(pose.pose(), (float) x, (float) y, (float) z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal(pose.normal(), (float) nx, (float) ny, (float) nz)
				.endVertex();
	}

	public static void renderStarBillboard(BufferBuilder builder, Camera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		addBillboard(builder, camera, node, center, tmPerUnit, partialTick);
		builder.end();
		CLIENT.getTextureManager().getTexture(STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);
	}

	public static void renderStarBillboard(BufferBuilder builder, OrbitCamera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		addBillboard(builder, camera, node, center, tmPerUnit, partialTick);
		builder.end();
		CLIENT.getTextureManager().getTexture(STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);
	}

	public static void addBillboard(VertexConsumer builder, Camera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {
		double d = getCelestialBodySize(node, camera.getPosition(), center);
		var up = new Vec3(camera.getUpVector());
		var right = new Vec3(camera.getLeftVector()).reverse();
		addBillboard(builder, up, right, node, center, d);
	}

	public static void addBillboard(VertexConsumer builder, OrbitCamera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {
		double d = getCelestialBodySize(node, camera.getPos(partialTick), center);
		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		addBillboard(builder, up, right, node, center, d);
	}

	public static double getCelestialBodySize(StarSystemNode node, Vec3 camPos, Vec3 bodyPos) {
		final double starMinSize = 0.01, starBaseSize = 0.05, starRadiusFactor = 0.5;
		final double otherMinSize = 0.01, otherBaseSize = 0.000075;

		var distanceFromCamera = camPos.distanceTo(bodyPos);

		double d = 0;
		if (node instanceof StarNode starNode) {
			var r = 0.1 * Math.min(10, starNode.radiusRsol);
			d = Math.max(starMinSize * distanceFromCamera,
					Math.max(starBaseSize, starRadiusFactor * r));
		} else if (node instanceof PlanetNode planetNode) {
			d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
		} else if (!(node instanceof BinaryNode)) {
			d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
		}

		return d;
	}

	public static void addBillboard(VertexConsumer builder, Vec3 up, Vec3 right, StarSystemNode node, Vec3 center,
			double d) {

		var color = Color.WHITE;
		if (node instanceof StarNode starNode) {
			color = starNode.getColor();
		}

		final double brightBillboardSizeFactor = 0.5;
		RenderHelper.addBillboard(builder, new PoseStack(), up, right, center, d, 0, color);
		RenderHelper.addBillboard(builder, new PoseStack(), up, right, center, brightBillboardSizeFactor * d, 0, Color.WHITE);
	}

	public static void addBillboard(VertexConsumer builder, PoseStack poseStack, Vec3 up, Vec3 right, Vec3 center,
			double scale,
			double zOffset, Color color) {

		var backwards = up.cross(right).scale(zOffset);
		var billboardUp = up.scale(scale);
		var billboardRight = right.scale(scale);
		addBillboard(builder, poseStack, center, billboardUp, billboardRight, backwards,
				color.r(), color.g(), color.b(), color.a());

	}

	public static void addBillboard(VertexConsumer builder, PoseStack poseStack, Vec3 center, Vec3 up, Vec3 right,
			Vec3 forward,
			float r, float g, float b, float a) {
		var p = poseStack.last().pose();
		var qll = center.subtract(up).subtract(right).add(forward);
		var qlh = center.subtract(up).add(right).add(forward);
		var qhl = center.add(up).subtract(right).add(forward);
		var qhh = center.add(up).add(right).add(forward);
		builder.vertex(p, (float) qhl.x, (float) qhl.y, (float) qhl.z).color(r, g, b, a).uv(1, 0).endVertex();
		builder.vertex(p, (float) qll.x, (float) qll.y, (float) qll.z).color(r, g, b, a).uv(0, 0).endVertex();
		builder.vertex(p, (float) qlh.x, (float) qlh.y, (float) qlh.z).color(r, g, b, a).uv(0, 1).endVertex();
		builder.vertex(p, (float) qhh.x, (float) qhh.y, (float) qhh.z).color(r, g, b, a).uv(1, 1).endVertex();
	}

	public static double getGridScale(OrbitCamera camera, double tmPerUnit, double scaleFactor, float partialTick) {
		var currentThreshold = tmPerUnit;
		var scale = tmPerUnit;
		for (var i = 0; i < 10; ++i) {
			currentThreshold *= scaleFactor;
			if (camera.scale.get(partialTick) > currentThreshold)
				scale = currentThreshold;
		}
		return scale;
	}

	public static void renderGrid(BufferBuilder builder, OrbitCamera camera, double tmPerUnit, double gridUnits,
			int scaleFactor, int gridLineCount, float partialTick) {
		var focusPos = camera.focus.get(partialTick).scale(1 / tmPerUnit);
		var gridScale = getGridScale(camera, gridUnits, scaleFactor, partialTick);
		renderGrid(builder, focusPos, gridScale * gridLineCount, scaleFactor, gridLineCount);
	}

	public static void renderGrid(BufferBuilder builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		addGrid(builder, focusPos, gridDiameter, subcellsPerCell, gridLineCount);
		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);
	}

	public static void addGrid(VertexConsumer builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.5f, g = 0.5f, b = 0.5f, a1 = 0.2f, a2 = 0.5f;

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
				builder.vertex(Mth.lerp(lt, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
				builder.vertex(Mth.lerp(ht, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
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
				builder.vertex(x, focusPos.y, Mth.lerp(lt, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
				builder.vertex(x, focusPos.y, Mth.lerp(ht, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
			}
		}

	}

	public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end, double lineWidth, Color color) {
		renderLine(builder, start, end, lineWidth, color, color);
	}

	public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end, double lineWidth, Color startColor,
			Color endColor) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		var normal = end.subtract(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.lineWidth((float) lineWidth);
		BufferUploader.end(builder);
	}

	public static void addLine(VertexConsumer builder, Vec3 start, Vec3 end, Color color) {
		addLine(builder, start, end, color, color);
	}

	public static void addLine(VertexConsumer builder, Vec3 start, Vec3 end, Color startColor, Color endColor) {
		var normal = end.subtract(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

	public static void addAxisAlignedBox(VertexConsumer builder, Vec3 p0, Vec3 p1, Color color) {
		double lx = p0.x < p1.x ? p0.x : p1.x;
		double ly = p0.y < p1.y ? p0.y : p1.y;
		double lz = p0.z < p1.z ? p0.z : p1.z;
		double hx = p0.x >= p1.x ? p0.x : p1.x;
		double hy = p0.y >= p1.y ? p0.y : p1.y;
		double hz = p0.z >= p1.z ? p0.z : p1.z;

		float r = color.r(), g = color.g(), b = color.b(), a = color.a();

		// X axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		// Y axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		// Z axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
	}

}
