package net.xavil.ultraviolet.client;

import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.ColorAccess;
import net.xavil.hawklib.math.ColorHsva;
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
		public final ColorRgba.Mutable color = new ColorRgba.Mutable();
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

	private record ColorTableInfo(Rng rng, Vector<ColorRgba> colors, float vibrancy, float weirdness) {}

	private void addColor(ColorTableInfo info, double r, double g, double b, double a) {
		final var hsva = ColorHsva.fromRgba((float) r, (float) g, (float) b, (float) a);
		// hsva.s *= info.rng.weightedDouble(2.0, info.vibrancy, 0.3);
		// hsva.s *= Mth.lerp(info.vibrancy, 0.6, 1.0);
		// hsva.v *= info.rng.uniformDouble(0.1, 1.0);
		// hsva.h -= 360 * info.rng.weightedDouble(2.0, 0.0, info.weirdness);
		// if (hsva.h < 0)
		// 	hsva.h += 360;
		info.colors.push(hsva.toRgba());
	}

	private void makeColorTable(Rng rng, Vector<ColorRgba> colors, float vibrancy, float weirdness) {
		final var info = new ColorTableInfo(rng, colors, vibrancy, weirdness);

		// rgb, a is emissive strength

		if (rng.chance(0.5)) {
			// crimson/red
			addColor(info, 0.122, 0.031, 0.008, 0f); // crimson
			addColor(info, 0.122, 0.031, 0.008, 0f); // crimson
			addColor(info, 0.122, 0.031, 0.008, 0f); // crimson
			addColor(info, 0.122, 0.031, 0.008, 0f); // crimson
			addColor(info, 0.122, 0.031, 0.008, 0f); // crimson
			addColor(info, 0.412f, 0.090f, 0.035f, 0f); // brighter red
			addColor(info, 0.380f, 0.157f, 0.086f, 0f); // brighter red (less saturation)
		} else {
			// beige
			addColor(info, 1.000f, 0.918f, 0.796f, 0f); // bright beige
			addColor(info, 1.000f, 0.918f, 0.796f, 0f); // bright beige
			addColor(info, 0.890f, 0.812f, 0.690f, 0f); // bright beige
			addColor(info, 0.890f, 0.812f, 0.690f, 0f); // bright beige
			addColor(info, 0.929f, 0.678f, 0.361f, 0f); // bright orangeish
			addColor(info, 0.890f, 0.616f, 0.275f, 0f); // bright orangeish
		}
		// bright
		addColor(info, 0.239f, 0.255f, 0.878f, 0f); // bright violet-blue
		addColor(info, 0.145f, 0.235f, 0.929f, 0f); // bright blue
		addColor(info, 0.145f, 0.918f, 0.929f, 0f); // bright cyan
		addColor(info, 0.176f, 0.651f, 0.902f, 0f); // bright blue
		if (rng.chance(0.1)) {
			addColor(info, 0.000f, 1.000f, 0.055f, 1f); // bright green
		}
		// white
		addColor(info, 0.969f, 0.949f, 0.929f, 0f); // white
		addColor(info, 1.000f, 0.980f, 0.957f, 0f); // white
		addColor(info, 0.969f, 0.965f, 0.949f, 0f); // white

		// addColor(info, 0.169, 0.000, 1.000, 0); // violet
		// addColor(info, 0.000, 0.012, 1.000, 0); // blue
		// addColor(info, 0.000, 0.776, 1.000, 0); // cyan-blue
		// addColor(info, 0.000, 1.000, 0.071, 1); // green
		// addColor(info, 1.000, 0.000, 0.000, 0); // red
	}

	private ColorRgba pickColor(Rng rng, Vector<ColorRgba> colors) {
		final var randomIndex = rng.uniformInt(0, colors.size());
		final var color = colors.remove(randomIndex);
		Mod.LOGGER.info("picked index #{}", randomIndex);

		final var hsva = color.toHsva();

		// small chance for any color to become emissive
		if (rng.chance(0.01)) {
			hsva.a = (float) rng.weightedDouble(2.0, 0.1, 2.5);
		}

		// hsva.s *= rng.weightedDouble(2.0, 0.1, 2.5);
		// hsva.v *= rng.weightedDouble(2.0, 0.1, 2.5);

		return hsva.toRgba();
	}

	private GlTexture1d generateGasGiantGradientTexture(PlanetaryCelestialNode node) {
		try (final var disposer = Disposable.scope()) {
			final var tex = disposer.attach(new GlClientTexture());
			tex.createStorage(512, 1, 1);
			final var colorSpline = new ColorSpline();			

			final var rng = Rng.fromSeed(node.seed);

			final var colors = new Vector<ColorRgba>();
			final var vibrancy = (float) rng.weightedDouble(4.0, 0.0, 1.0);
			final var weirdness = (float) rng.weightedDouble(16.0, 0.0, 1.0);
			makeColorTable(rng, colors, vibrancy, weirdness);

			Mod.LOGGER.info("----------");
			
			final var minStep = Mth.lerp(weirdness, 0.3, 0.1);
			float t = 0;
			while (t < 1 && colors.size() > 1) {
				colorSpline.addControlPoint(t, pickColor(rng, colors));
				t += rng.uniformDouble(minStep, 0.6);
			}
			colorSpline.addControlPoint(1f, pickColor(rng, colors));

			colorSpline.sample(0, 1, tex.sizeX(), tex::setPixel);
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
			setupLight(i, Vec3.ZERO, ColorRgba.TRANSPARENT);
		}
	}

	public void setupLight(int i, Vec3Access pos, ColorAccess color) {
		if (i >= this.lights.length)
			return;
		if (this.lights[i] == null)
			this.lights[i] = new Light();
		final var shader = UltravioletShaders.SHADER_CELESTIAL_NODE.get();
		Vec3.set(this.lights[i].pos, pos);
		ColorRgba.set(this.lights[i].color, color);
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

		if (!skip && !(node instanceof StellarCelestialNode starNode && starNode.type == StellarCelestialNode.Type.BLACK_HOLE)) {
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
		builder.vertex(p).uv0(u, v).color(ColorRgba.WHITE).normal(norm).endVertex();
	}

}
