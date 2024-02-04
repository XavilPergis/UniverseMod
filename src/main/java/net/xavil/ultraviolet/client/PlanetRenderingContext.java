package net.xavil.ultraviolet.client;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.ColorSpline;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.WeightedList;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;

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
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.ColorAccess;
import net.xavil.hawklib.math.ColorHsva;
import net.xavil.hawklib.math.ColorOklch;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
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
				case STAR -> 0;
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

	private static abstract sealed class ColorPalette {

		public final String exclusionGroup;

		public ColorPalette(String exclusionGroup) {
			this.exclusionGroup = exclusionGroup;
		}

		@Nullable
		public abstract ColorRgba pick(Rng rng);

		@Override
		public abstract ColorPalette clone();

		public static final class AnyOf extends ColorPalette {
			public final WeightedList<ColorPalette> children;

			public AnyOf(String exclusionGroup, WeightedList<ColorPalette> children) {
				super(exclusionGroup);
				this.children = children;
			}

			@Override
			@Nullable
			public ColorRgba pick(Rng rng) {
				final var node = this.children.pick(rng.uniformDouble());
				// we ran out of stuff to pick from, abort!
				if (node == null)
					return null;

				// throw away any other nodes in the same exclusion group as the chosen node
				if (node.exclusionGroup != null)
					this.children.retain(child -> child.value == node
							|| !node.exclusionGroup.equals(child.value.exclusionGroup));

				if (!(node instanceof AnyOf anyOf && !anyOf.children.isEmpty()))
					this.children.remove(node);

				ColorRgba color = node.pick(rng);
				if (color == null) {
					// i love recursion
					color = pick(rng);
				}

				return color;
			}

			@Override
			public ColorPalette clone() {
				return new AnyOf(this.exclusionGroup, new WeightedList<>(
						this.children.iter().map(child -> child.withValue(child.value.clone()))));
			}
		}

		public static final class Color extends ColorPalette {
			public final ColorRgba color;

			public Color(String exclusionGroup, ColorRgba color) {
				super(exclusionGroup);
				this.color = color;
			}

			@Override
			public ColorRgba pick(Rng rng) {
				return this.color;
			}

			@Override
			public ColorPalette clone() {
				return new Color(this.exclusionGroup, this.color);
			}
		}
	}

	private static final class ColorTableBuilder {
		private record Entry(String exclusionGroup, double weight, WeightedList<ColorPalette> nodes) {
			public ColorPalette makeNode() {
				return new ColorPalette.AnyOf(this.exclusionGroup, this.nodes);
			}
		}

		private final Vector<Entry> stack = new Vector<>();
		private Entry current;

		public ColorTableBuilder() {
			this.current = new Entry(null, -1, new WeightedList<>());
		}

		public void anyOf(double weight) {
			anyOf(null, weight);
		}

		public void anyOf(String exclusionGroup, double weight) {
			this.stack.push(this.current);
			this.current = new Entry(exclusionGroup, weight, new WeightedList<>());
		}

		public void end() {
			final var prev = this.current;
			this.current = this.stack.popOrThrow();
			this.current.nodes.push(prev.weight, prev.makeNode());
		}

		public void colorSrgb(double weight, double r, double g, double b, double a) {
			colorSrgb(null, weight, r, g, b, a);
		}

		public void colorSrgb(String exclusionGroup, double weight, double r, double g, double b, double a) {
			var color = new ColorRgba((float) r, (float) g, (float) b, (float) a);
			color = ColorRgba.srgbToLinear(color);
			final var node = new ColorPalette.Color(exclusionGroup, color);
			this.current.nodes.push(weight, node);
		}

		public void colorOklch(double weight, double L, double C, double h, double a) {
			colorOklch(null, weight, L, C, h, a);
		}

		public void colorOklch(String exclusionGroup, double weight, double L, double C, double h, double a) {
			final var color = ColorOklch.toLinearSrgb((float) L, (float) C, (float) h, (float) a);
			final var node = new ColorPalette.Color(exclusionGroup, color);
			this.current.nodes.push(weight, node);
		}

		public void colorHsv(double weight, double h, double s, double v, double a) {
			colorHsv(null, weight, h, s, v, a);
		}

		public void colorHsv(String exclusionGroup, double weight, double h, double s, double v, double a) {
			var color = ColorHsva.toRgba((float) h, (float) s, (float) v, (float) a);
			// idk????????
			color = ColorRgba.srgbToLinear(color);
			final var node = new ColorPalette.Color(exclusionGroup, color);
			this.current.nodes.push(weight, node);
		}

		public ColorPalette build() {
			return this.current.makeNode();
		}
	}

	private void makeColorTable(ColorTableBuilder builder) {

		// @formatter:off
		// browns
		builder.anyOf("base", 20);
			builder.anyOf("lightness", 2);
				builder.colorHsv(2, 25, 0.4, 0.2, 0);
				builder.colorHsv(2, 25, 0.4, 0.3, 0);
				builder.colorHsv(1, 25, 0.4, 0.4, 0);
			builder.end();
			builder.anyOf("lightness", 1);
				builder.colorHsv(2, 25, 0.4, 0.5, 0);
				builder.colorHsv(2, 25, 0.4, 0.6, 0);
				builder.colorHsv(2, 25, 0.4, 0.8, 0);
				builder.colorSrgb("white", 2, 1, 1, 1, 0);
			builder.end();
		builder.end();

		// beiges
		builder.anyOf("base", 20);
		builder.colorHsv(2, 25, 0.4, 0.8, 0);
		builder.colorHsv(2, 25, 0.4, 0.7, 0);
		builder.colorHsv(2, 25, 0.4, 0.6, 0);
		builder.colorHsv(2, 25, 0.7, 0.5, 0);
		// builder.colorSrgb(2, 0.8 * 1.000f, 0.8 * 0.918f, 0.8 * 0.796f, 0f); // bright beige
		// builder.colorSrgb(2, 1.000f, 0.918f, 0.796f, 0f); // bright beige
		// builder.colorSrgb(2, 0.890f, 0.812f, 0.690f, 0f); // bright beige
		builder.end();

		// blue/violet
		builder.anyOf("base", 10);
		builder.colorHsv(2, 260, 0.4, 0.2, 0f);
		builder.colorHsv(2, 260, 0.4, 0.3, 0f);
		builder.colorHsv(2, 260, 0.4, 0.4, 0f);
		// builder.colorSrgb("white", 2, 1, 1, 1, 0);
		builder.end();

		// snowball
		builder.anyOf("base", 8);
			builder.colorSrgb(200, 1, 1, 1, 0);
			builder.colorSrgb(200, 0.9, 0.9, 0.9, 0);
			builder.anyOf("accent", 10);
				builder.colorHsv(2, 25, 0.4, 0.2, 0);
				builder.colorHsv(2, 25, 0.4, 0.3, 0);
				builder.colorHsv(1, 25, 0.4, 0.4, 0);
			builder.end();
			builder.colorHsv("accent", 1, 280, 1, 0.9, 0);
			builder.colorHsv("accent", 1, 35, 1, 0.9, 0);
			builder.colorHsv("accent", 1, 200, 1, 0.9, 0);
		builder.end();

		builder.anyOf("base", 3);
			builder.colorSrgb("white", 2, 1, 1, 1, 0);
			for (double h = 0; h < 360; h += 50) {
				if (h >= 50 && h <= 150) continue;
				builder.anyOf("bright", 1);
				builder.colorHsv(         1, h, 0.3, 0.8, 0);
				builder.colorHsv(         1, h, 0.3, 0.7, 0);
				builder.colorHsv("shade", 1, h, 0.8, 0.8, 0);
				builder.colorHsv("shade", 1, h, 0.3, 0.5, 0);
				builder.end();
			}
			// builder.colorSrgb(1, 0.239f, 0.255f, 0.878f, 0f); // bright violet-blue
			// builder.colorSrgb(1, 0.145f, 0.235f, 0.929f, 0f); // bright blue
			// builder.colorSrgb(1, 0.145f, 0.918f, 0.929f, 0f); // bright cyan
			// builder.colorSrgb(1, 0.176f, 0.651f, 0.902f, 0f); // bright blue
			// builder.colorSrgb(0.05, 0.000f, 1.000f, 0.055f, 1f); // bright green
		builder.end();

		builder.colorSrgb(0.1, 0, 1, 0, 2);
		// @formatter:on

		// builder.oneOf();
		// builder.colorSrgb(1, 1, 0, 0, 0);
		// builder.colorSrgb(1, 0, 1, 0, 0);
		// builder.colorSrgb(1, 0, 0, 1, 0);
		// builder.end();

		// builder.oneOf();
		// builder.colorSrgb(1, 0, 1, 1, 0);
		// builder.colorSrgb(1, 1, 0, 1, 0);
		// builder.colorSrgb(1, 1, 1, 0, 0);
		// builder.end();
	}

	private ColorPalette getColorTable() {
		final var builder = new ColorTableBuilder();
		makeColorTable(builder);
		return builder.build();
	}

	private GlTexture1d createGradientTextureFromSpline(ColorSpline spline) {
		try (final var disposer = Disposable.scope()) {
			final var tex = disposer.attach(new GlClientTexture());
			tex.createStorage(GlClientTexture.ClientFormat.RGBA32_FLOAT, 512, 1, 1);
			spline.sample(0, 1, tex.sizeX(), tex::setPixel);
			return tex.create1d(GlTexture.Format.RGBA16_FLOAT);
		}
	}

	private GlTexture1d generateGasGiantGradientTexture(PlanetaryCelestialNode node) {
		final var rng = new SplittableRng(node.seed);

		final var palette = getColorTable();

		final var colorSpline = new ColorSpline();

		final var pickingRng = rng.rng("picking");
		ColorRgba endColor = palette.pick(pickingRng);
		endColor = endColor == null ? ColorRgba.MAGENTA : endColor;

		float t = 0;
		rng.push("spline");
		while (t < 1) {
			final var color = palette.pick(pickingRng);
			if (color == null)
				break;
			colorSpline.addControlPoint(t, color);
			t += rng.weightedDouble("t", 4.0, 0.6, 0.3);
		}
		rng.pop();
		colorSpline.addControlPoint(1f, endColor);

		return createGradientTextureFromSpline(colorSpline);
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
			setupLight(i, pos, star.getColor().withA(star.getBrightnessMultiplier()));
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

		final var nodeShader = UltravioletShaders.SHADER_CELESTIAL_NODE.get();
		final var pointShader = UltravioletShaders.SHADER_STAR_BILLBOARD_REALISTIC.get();

		nodeShader.setUniformi("uRenderingSeed", new SplittableRng(node.getId()).uniformInt("seed"));
		nodeShader.setUniformi("uNodeType", getNodeTypeInt(node));

		if (clientInfo.gasGiantGradient != null) {
			nodeShader.setUniformSampler("uGasGiantColorGradient", clientInfo.gasGiantGradient);
		}

		if (node instanceof PlanetaryCelestialNode) {
		} else if (node instanceof StellarCelestialNode starNode) {
			nodeShader.setUniformf("uStarColor", starNode.getColor().withA(starNode.getBrightnessMultiplier()));
		}

		nodeShader.setUniformf("uMetersPerUnit", camera.metersPerUnit);
		nodeShader.setUniformf("uTime", this.celestialTime);

		transform.push();
		transform.appendTranslation(this.origin);
		nodeShader.setUniformf("uModelMatrix", transform.current());
		transform.pop();

		BufferRenderer.setupCameraUniforms(nodeShader, camera);
		BufferRenderer.setupCameraUniforms(pointShader, camera);
		nodeShader.setupDefaultShaderUniforms();
		pointShader.setupDefaultShaderUniforms();
		StarRenderManager.setupStarShader(pointShader, camera);

		if (!skip && !(node instanceof StellarCelestialNode starNode
				&& starNode.type == StellarCelestialNode.Type.BLACK_HOLE)) {

			final var builder2 = builder.beginGeneric(PrimitiveType.POINT_QUADS,
					UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD);

			// final var actualOrigin = this.floatingOrigin;

			// don't render the stars that are behind the camera in immediate mode
			// if (ctx.isImmediateMode) {
			// 	Vec3.set(toStar, elem.systemPosTm);
			// 	Vec3.sub(toStar, toStar, ctx.centerPos);
			// 	if (toStar.dot(ctx.camera.forward) == 0)
			// 		return;
			// }

			// Vec3.sub(elem.systemPosTm, elem.systemPosTm, actualOrigin);
			// Vec3.add(elem.systemPosTm, elem.systemPosTm, this.originOffset);
			// Vec3.mul(elem.systemPosTm, elem.systemPosTm, 1e12 / ctx.camera.metersPerUnit);


			// if (this.mode == Mode.REALISTIC) {
			// 	ctx.builder.vertex(elem.systemPosTm)
			// 			.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
			// 			.uv0((float) elem.luminosityLsol, 0)
			// 			.endVertex();
			// } else if (this.mode == Mode.MAP) {
			// 	ctx.builder.vertex(elem.systemPosTm)
			// 			.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
			// 			.endVertex();	
			// }

			// final var nodePos = node.position.sub(camera.posTm).sub(this.origin).div(1e12).div(1e12 / camera.metersPerUnit);
			// final var nodePos = node.position.sub(camera.posTm).sub(this.origin).div(1e12 / camera.metersPerUnit);

			transform.push();
			transform.appendTranslation(this.origin);
			// final var nodePos = Mat4.mul(transform.current(), node.position, 1);
			final var nodePos = Mat4.mul(transform.current(), Vec3.ZERO, 1);
			// final var nodePos = node.position.sub(camera.posTm);
			transform.pop();

			// final var nodePos = node.position.div(1e12 / camera.metersPerUnit);

			if (node instanceof StellarCelestialNode starNode) {
				builder2.vertex(nodePos)
						.color(starNode.getColor())
						.uv0((float) starNode.luminosityLsol, 0)
						.endVertex();
			} else {
				builder2.vertex(nodePos)
						// TODO: determine color and luminosity from reflected light
						.color(ColorRgba.WHITE)
						.uv0(0.0000000018554f, 0)
						.endVertex();
			}
			// builder2.vertex(Vec3.ZN)
			// 		// TODO: determine color and luminosity from reflected light
			// 		.color(ColorRgba.RED)
			// 		.uv0(100000f, 0)
			// 		.endVertex();
			builder2.end().draw(pointShader, DRAW_STATE_ADDITIVE_BLENDING);

			this.sphereMesh.draw(nodeShader, DRAW_STATE_OPAQUE);
		}
	}

	// TODO: draw rings again
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
