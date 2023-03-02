package net.xavil.universal.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.screen.Color;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;

public final class PlanetRenderingContext {

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

	private static ResourceLocation getBaseLayer(PlanetNode node) {
		var missing = MissingTextureAtlasSprite.getLocation();
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

	public record PointLight(Vec3 pos, Color color, double luminosity) {
	}

	public final List<PointLight> pointLights = new ArrayList<>();
	private final BufferBuilder builder;
	private final Minecraft client = Minecraft.getInstance();

	public PlanetRenderingContext(BufferBuilder builder) {
		this.builder = builder;
	}

	public void render(StarSystemNode node, PoseStack poseStack, Vec3 pos, double scale, Color tintColor) {
		if (node instanceof StarNode starNode)
			renderStar(starNode, poseStack, pos, scale, tintColor);
		if (node instanceof PlanetNode planetNode)
			renderPlanet(planetNode, poseStack, pos, scale, tintColor);
	}

	public void renderStar(StarNode node, PoseStack poseStack, Vec3 pos, double scale, Color tintColor) {
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		// addBillboard(builder, camera, node, center, tmPerUnit, partialTick);
		double d = RenderHelper.getCelestialBodySize(node, pos, Vec3.ZERO);

		var aaa = new PoseStack();
		aaa.last().pose().multiply(RenderSystem.getModelViewMatrix());
		aaa.last().pose().multiply(poseStack.last().pose());
		// var aaa = RenderSystem.getModelViewMatrix().copy();
		RenderHelper.addBillboard(builder, Vec3.YP, Vec3.XP, node, pos, d);

		builder.end();
		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);
		// var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, node.massYg,
		// 		Units.METERS_PER_RSOL * node.radiusRsol / Units.METERS_PER_REARTH, node.temperatureK);
		// renderPlanet(planetNode, poseStack, pos, scale, tintColor);
	}

	public void renderPlanet(PlanetNode node, PoseStack poseStack, Vec3 pos, double metersPerUnit, Color tintColor) {

		var radiusM = 2 * Units.METERS_PER_REARTH * node.radiusRearth;
		// final var metersPerUnit = 1 / unitsPerMeter;

		var posView = node.position.transformBy(RenderSystem.getModelViewMatrix());
		if (posView.length() * metersPerUnit > 800 * radiusM)
			return;

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		final var sortedLights = new ArrayList<>(this.pointLights);
		sortedLights.sort(Comparator.comparingDouble(light -> light.pos.distanceTo(pos)));

		final var lightCount = Math.min(4, sortedLights.size());

		var planetShader = ModRendering.getShader(ModRendering.PLANET_SHADER);

		for (var i = 0; i < 4; ++i) {
			var lightColor = planetShader.getUniform("LightColor" + i);
			if (lightColor != null)
				lightColor.set(new Vector4f(0, 0, 0, -1));
		}

		for (var i = 0; i < lightCount; ++i) {
			final var light = sortedLights.get(i);
			var lightPos = planetShader.getUniform("LightPos" + i);
			var aaa = new Vector4f(new Vector3f(light.pos.asMinecraft()));
			if (lightPos != null)
				lightPos.set(aaa);
			var lightColor = planetShader.getUniform("LightColor" + i);
			if (lightColor != null) {
				float r = light.color.r(), g = light.color.g(), b = light.color.b();
				float luminosity = (float) Math.max(light.luminosity, 0.4);
				lightColor.set(new Vector4f(r, g, b, luminosity));
			}
		}

		if (node.type == PlanetNode.Type.GAS_GIANT) {
			Minecraft.getInstance().getTextureManager().getTexture(BASE_GAS_GIANT_LOCATION).setFilter(true, false);
		}

		RenderSystem.defaultBlendFunc();
		var baseTexture = getBaseLayer(node);
		var radiusUnits = radiusM / metersPerUnit;
		// var radiusM = scale * 200 * Units.METERS_PER_REARTH * node.radiusRearth;
		renderTexturedCube(builder, baseTexture, poseStack, pos, radiusUnits, tintColor);
		if (node.type == PlanetNode.Type.EARTH_LIKE_WORLD) {
			renderTexturedCube(builder, FEATURE_EARTH_LIKE_LOCATION, poseStack, pos, radiusUnits, tintColor);
		}
		if (node.type == PlanetNode.Type.ROCKY_WORLD) {
			renderTexturedCube(builder, FEATURE_CRATERS_LOCATION, poseStack, pos, radiusUnits, tintColor);
		}
	}

	private static void renderTexturedCube(BufferBuilder builder, ResourceLocation texture, PoseStack poseStack,
			Vec3 center, double radius, Color tintColor) {
		// RenderSystem.setShader(() -> GameRenderer.getPositionTexColorNormalShader());
		RenderSystem.setShader(() -> ModRendering.getShader(ModRendering.PLANET_SHADER));
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		// addCube(builder, poseStack, center, radius, tintColor);
		addNormSphere(builder, poseStack, center, radius, tintColor);
		builder.end();
		RenderSystem.setShaderTexture(0, texture);
		BufferUploader.end(builder);
	}

	private static void addNormSphere(VertexConsumer builder, PoseStack poseStack, Vec3 center, double radius,
			Color tintColor) {

		poseStack.pushPose();
		poseStack.translate(center.x, center.y, center.z);
		final var pose = poseStack.last();
		final var subdivisions = 10;

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
				normSphereVertex(builder, pose, tintColor, radius, -1, ha, lb, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, -1, la, lb, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, -1, la, hb, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, -1, ha, hb, hu, hv);
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
				normSphereVertex(builder, pose, tintColor, radius, 1, ha, lb, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, 1, la, lb, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, 1, la, hb, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, 1, ha, hb, hu, hv);
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
				normSphereVertex(builder, pose, tintColor, radius, ha, -1, lb, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, -1, lb, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, -1, hb, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, ha, -1, hb, hu, hv);
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
				normSphereVertex(builder, pose, tintColor, radius, ha, 1, lb, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, 1, lb, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, 1, hb, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, ha, 1, hb, hu, hv);
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
				normSphereVertex(builder, pose, tintColor, radius, ha, lb, -1, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, lb, -1, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, hb, -1, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, ha, hb, -1, hu, hv);
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
				normSphereVertex(builder, pose, tintColor, radius, ha, lb, 1, hu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, lb, 1, lu, lv);
				normSphereVertex(builder, pose, tintColor, radius, la, hb, 1, lu, hv);
				normSphereVertex(builder, pose, tintColor, radius, ha, hb, 1, hu, hv);
			}
		}

		poseStack.popPose();
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

	private static void normSphereVertex(VertexConsumer builder, PoseStack.Pose pose, Color color, double radius,
			double x, double y, double z, float u, float v) {
		final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
		var pos = Vec3.from(x, y, z);
		var n = pos.normalize();
		var p = n.mul(radius);
		builder.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal(pose.normal(), (float) n.x, (float) n.y, (float) n.z)
				.endVertex();
	}

	private static void cubeVertex(VertexConsumer builder, PoseStack.Pose pose, Color color,
			double x, double y, double z, double nx, double ny, double nz, float u, float v) {
		final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
		builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal(pose.normal(), (float) nx, (float) ny, (float) nz)
				.endVertex();
	}

}
