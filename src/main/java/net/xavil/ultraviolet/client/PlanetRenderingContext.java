package net.xavil.ultraviolet.client;

import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import static net.xavil.hawklib.client.HawkDrawStates.*;

import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.gl.texture.GlClientTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture1d;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.ColorAccess;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class PlanetRenderingContext implements Disposable {

	private double celestialTime = 0;
	private Vec3 origin = Vec3.ZERO;

	private boolean isDrawing = false;

	private final Mesh sphereMesh = new Mesh();
	private final MutableMap<UnaryCelestialNode, ClientNodeInfo> clientInfos = MutableMap.identityHashMap();
	private final Light[] lights = new Light[4];

	private static final class ClientNodeInfo implements Disposable {
		public GlTexture1d gasGiantGradient;
		public boolean wasUsedThisFrame = false;

		public ClientNodeInfo() {
		}

		@Override
		public void close() {
			if (this.gasGiantGradient != null)
				this.gasGiantGradient.close();
		}
	}

	public static final class Light {
		public final Vec3.Mutable pos = new Vec3.Mutable();
		public final Color.Mutable color = new Color.Mutable();
	}

	public PlanetRenderingContext() {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.QUADS,
				BufferLayout.POSITION_TEX_COLOR_NORMAL);
		RenderHelper.addCubeSphere(builder, Vec3.ZERO, 1, 16);
		this.sphereMesh.upload(builder.end());
	}

	@Override
	public void close() {
		this.sphereMesh.close();
		this.clientInfos.values().forEach(ClientNodeInfo::close);
	}

	public void begin(double celestialTime) {
		if (this.isDrawing) {
			throw new IllegalStateException(String.format(
					"cannot call begin() while already drawing!"));
		}
		this.isDrawing = true;
		this.celestialTime = celestialTime;

		resetLights();
	}

	public void end() {
		if (!this.isDrawing) {
			throw new IllegalStateException(String.format(
					"cannot call end() while not already drawing!"));
		}
		this.isDrawing = false;
		final MutableSet<UnaryCelestialNode> removalSet = MutableSet.identityHashSet();
		this.clientInfos.entries().forEach(entry -> {
			final var node = entry.key;
			final var info = entry.get().unwrap();
			if (!info.wasUsedThisFrame)
				removalSet.insert(node);
			info.wasUsedThisFrame = false;
		});
		removalSet.forEach(node -> this.clientInfos.remove(node).unwrap().close());
	}

	public void setSystemOrigin(Vec3 origin) {
		this.origin = origin;
	}

	public static int getNodeTypeInt(UnaryCelestialNode node) {
		if (node instanceof StellarCelestialNode pNode) {
			return switch (pNode.type) {
				case MAIN_SEQUENCE -> 0;
				case GIANT -> 1;
				case WHITE_DWARF -> 2;
				case NEUTRON_STAR -> 3;
				case BLACK_HOLE -> 4;
			};
		} else if (node instanceof PlanetaryCelestialNode pNode) {
			return switch (pNode.type) {
				case BROWN_DWARF -> 5;
				case GAS_GIANT -> 6;
				case ICE_WORLD -> 7;
				case ROCKY_WORLD -> 8;
				case ROCKY_ICE_WORLD -> 9;
				case WATER_WORLD -> 10;
				case EARTH_LIKE_WORLD -> 11;
			};
		}

		return -1;
	}

	private GlTexture1d generateGasGiantGradientTexture(PlanetaryCelestialNode node) {
		try (final var disposer = Disposable.scope()) {
			final var tex = disposer.attach(new GlClientTexture());
			tex.createStorage(512, 1, 1);

			final var rng = Rng.fromSeed(node.seed);

			// high weirdness values mean high color spread
			final var weirdness = rng.weightedDouble(4.0, 10.0, 1.0);

			final var colorSpline = new ColorSpline();
			colorSpline.addControlPoint(0f, new Color(0.22f, 0.145f, 0.047f));
			colorSpline.addControlPoint(0.5f, new Color(0.22f, 0.145f, 0.047f).mul(3));
			colorSpline.addControlPoint(0.7f, new Color(0.922f, 0.847f, 0.765f));
			colorSpline.addControlPoint(0.75f, new Color(0.22f, 0.145f, 0.047f));
			colorSpline.addControlPoint(1f, new Color(1f, 0.98f, 0.957f));
			// colorSpline.addControlPoint(0f, new Color(1f, 0f, 0f));
			// colorSpline.addControlPoint(0.5f, new Color(0f, 1f, 0f));
			// colorSpline.addControlPoint(1f, new Color(0f, 0f, 1f));

			colorSpline.sample(0, 1, tex.sizeX(), (i, r, g, b, a) -> {
				tex.setPixel(i, 0, 0, r, g, b, a);
			});

			return tex.create1d(GlTexture.Format.RGBA8_UINT_NORM);
		}
	}

	private ClientNodeInfo makeClientInfoIfNeeded(UnaryCelestialNode node) {
		if (this.clientInfos.containsKey(node))
			return this.clientInfos.getOrNull(node);

		final var clientInfo = new ClientNodeInfo();

		if (node instanceof PlanetaryCelestialNode planetNode
				&& planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
			clientInfo.gasGiantGradient = generateGasGiantGradientTexture(planetNode);
		}

		this.clientInfos.insert(node, clientInfo);

		return this.clientInfos.getOrNull(node);
	}

	public void resetLights() {
		for (var i = 0; i < this.lights.length; ++i) {
			setupLight(i, Vec3.ZERO, Color.TRANSPARENT);
		}
	}

	public void setupLight(int i, Vec3Access pos, ColorAccess color) {
		if (i >= this.lights.length)
			return;
		if (this.lights[i] == null)
			this.lights[i] = new Light();
		final var shader = UltravioletShaders.SHADER_CELESTIAL_NODE.get();
		Vec3.set(this.lights[i].pos, pos);
		Color.set(this.lights[i].color, color);
		shader.setUniformf("uLightPos" + i, pos.x(), pos.y(), pos.z(), 1);
		shader.setUniformf("uLightColor" + i, color.r(), color.g(), color.b(), color.a());
	}

	public void setupLights(StarSystem system, CachedCamera camera) {
		final var stars = new Vector<StellarCelestialNode>();
		stars.extend(system.rootNode.iter().filterCast(StellarCelestialNode.class));

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		stars.sort(Comparator.comparingDouble(star -> star.position.xyz().distanceTo(camera.posTm)));

		final var starCount = Math.min(this.lights.length, stars.size());
		for (var i = 0; i < starCount; ++i) {
			final var star = stars.get(i);
			final var pos = camera.toCameraSpace(this.origin.add(star.position));
			setupLight(i, pos, star.getColor().withA((float) star.luminosityLsol));
		}
	}

	public void render(VertexBuilder builder, CachedCamera camera, UnaryCelestialNode node, TransformStack transform,
			boolean skip) {
		if (!this.isDrawing) {
			throw new IllegalStateException(String.format(
					"cannot render celestial node before begin() was called!"));
		}

		final var clientInfo = makeClientInfoIfNeeded(node);
		clientInfo.wasUsedThisFrame = true;

		final var shader = UltravioletShaders.SHADER_CELESTIAL_NODE.get();

		shader.setUniformi("uRenderingSeed", new StableRandom(node.getId()).uniformInt("seed"));
		shader.setUniformi("uNodeType", getNodeTypeInt(node));

		if (clientInfo.gasGiantGradient != null) {
			shader.setUniformSampler("uGasGiantColorGradient", clientInfo.gasGiantGradient);
		}

		if (node instanceof PlanetaryCelestialNode planetNode) {
		} else if (node instanceof StellarCelestialNode starNode) {
			shader.setUniformf("uStarColor", starNode.getColor().withA((float) starNode.luminosityLsol));
		}

		shader.setUniformf("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniformf("uTime", this.celestialTime);

		transform.push();
		transform.appendTranslation(this.origin);
		shader.setUniformf("uModelMatrix", transform.current());
		transform.pop();

		BufferRenderer.setupCameraUniforms(shader, camera);
		BufferRenderer.setupDefaultShaderUniforms(shader);

		if (!skip) {
			this.sphereMesh.draw(shader, DRAW_STATE_OPAQUE);
		}
	}

	private static void addRing(FlexibleVertexConsumer builder, CachedCamera camera,
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

	private static void ringVertex(FlexibleVertexConsumer builder, CachedCamera camera,
			Vec3 center, double x, double y, double z, float u, float v) {
		final var pos = new Vec3(x, 0, z);
		var norm = y > 0 ? Vec3.YN : Vec3.YP;
		norm = norm.normalize();
		final var p = camera.toCameraSpace(pos).add(center);
		builder.vertex(p).uv0(u, v).color(Color.WHITE).normal(norm).endVertex();
	}

}
