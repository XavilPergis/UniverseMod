package net.xavil.universal.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.client.screen.Color;
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
	public static final ResourceLocation FEATURE_CRATERS_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/craters.png");
	public static final ResourceLocation FEATURE_EARTH_LIKE_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/earth_like.png");

	private static ResourceLocation getBaseLayer(PlanetNode node) {
		var missing = MissingTextureAtlasSprite.getLocation();
		return switch (node.type) {
			case EARTH_LIKE_WORLD -> BASE_WATER_LOCATION;
			case GAS_GIANT -> BASE_ROCKY_LOCATION;
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
		var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD, node.massYg,
				Units.METERS_PER_RSOL * node.radiusRsol / Units.METERS_PER_REARTH, node.temperatureK);
		renderPlanet(planetNode, poseStack, pos, scale, tintColor);
	}

	public void renderPlanet(PlanetNode node, PoseStack poseStack, Vec3 pos, double scale, Color tintColor) {

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		final var sortedLights = new ArrayList<>(this.pointLights);
		sortedLights.sort(Comparator.comparingDouble(light -> light.pos.distanceTo(pos)));
		// Collections.reverse(sortedLights);

		final var lightCount = Math.min(4, sortedLights.size());

		var planetShader = ModRendering.getShader(ModRendering.PLANET_SHADER);

		// if (sortedLights.size() > 0) sortedLights.set(0, new PointLight(sortedLights.get(0).pos, Color.RED, 1));
		// if (sortedLights.size() > 1) sortedLights.set(1, new PointLight(sortedLights.get(1).pos, Color.GREEN, 1));
		// if (sortedLights.size() > 2) sortedLights.set(2, new PointLight(sortedLights.get(2).pos, Color.BLUE, 1));

		for (var i = 0; i < 4; ++i) {
			var lightColor = planetShader.getUniform("LightColor" + i);
			if (lightColor != null)
				lightColor.set(new Vector4f(0, 0, 0, -1));
		}

		for (var i = 0; i < lightCount; ++i) {
			final var light = sortedLights.get(i);
			var lightPos = planetShader.getUniform("LightPos" + i);
			var aaa = new Vector4f(new Vector3f(light.pos.asMinecraft()));
			aaa.normalize();
			if (lightPos != null)
				lightPos.set(aaa);
			var lightColor = planetShader.getUniform("LightColor" + i);
			if (lightColor != null)
				lightColor
						.set(new Vector4f(light.color.r(), light.color.g(), light.color.b(), (float) light.luminosity));
		}

		// var planetShader = GameRenderer.getPositionTexColorNormalShader();

		RenderSystem.setShader(() -> planetShader);
		RenderSystem.defaultBlendFunc();
		var baseTexture = getBaseLayer(node);
		var radiusM = scale * 2 * Units.METERS_PER_REARTH * node.radiusRearth;
		// var radiusM = 2 * Units.METERS_PER_REARTH * node.radiusRearth;
		renderTexturedCube(builder, baseTexture, poseStack, pos, radiusM, tintColor);
		if (node.type == PlanetNode.Type.EARTH_LIKE_WORLD) {
			renderTexturedCube(builder, FEATURE_EARTH_LIKE_LOCATION, poseStack, pos, radiusM, tintColor);
		}
	}

	private static void renderTexturedCube(BufferBuilder builder, ResourceLocation texture, PoseStack poseStack,
			Vec3 center, double radius, Color tintColor) {
		RenderSystem.setShader(() -> ModRendering.getShader(ModRendering.PLANET_SHADER));
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
		builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal(pose.normal(), (float) nx, (float) ny, (float) nz)
				.endVertex();
	}

}
