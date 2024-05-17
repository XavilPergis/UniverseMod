package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.MotionSmoother;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.NumericOps;
import net.xavil.hawklib.math.Rect;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.mixin.accessor.FontAccessor;
import net.xavil.ultraviolet.mixin.accessor.LightTextureAccessor;

public class ScreenLayerStarStatistics extends HawkScreen.Layer2d {

	public final Galaxy galaxy;
	public final SectorTicket<?> ticket;

	private ScatterPlot xyPlot = null;
	private Histogram xHistogram = null, yHistogram = null;

	private Variable xVariable = Variable.TEMPERATURE, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.DISTANCE, yVariable =
	// Variable.TEMPERATURE;
	// private Variable xVariable = Variable.AGE, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.MASS, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.MASS, yVariable = Variable.TEMPERATURE;

	// private AxisMapping xMapping = new AxisMapping.Log(10, 1, 1e5);
	// private AxisMapping xMapping = new AxisMapping.Linear(0, 13000);
	private AxisMapping xMapping = new AxisMapping.Log(10, this.xVariable.interval);
	private AxisMapping yMapping = new AxisMapping.Log(10, this.yVariable.interval);

	public MotionSmoother<Double> scale = new MotionSmoother<>(0.6, NumericOps.DOUBLE, 1.0);
	public MotionSmoother<Vec2> offset = new MotionSmoother<>(0.6, NumericOps.VEC2, Vec2.ZERO);
	public double scaleMin = 0.3, scaleMax = 30;
	public double scrollMultiplier = 1.2;

	private CachedCamera camera = new CachedCamera();
	private Vec3 center;

	private static final ColorRgba AXIS_MARKER_COLOR = new ColorRgba(0.05f, 0.83f, 0.07f, 1f);
	private static final ColorRgba MAJOR_MARKER_COLOR = new ColorRgba(1f, 1f, 1f, 0.8f);
	private static final ColorRgba MINOR_MARKER_COLOR = new ColorRgba(0.5f, 0.5f, 0.5f, 0.1f);
	private static final ColorRgba HISTOGRAM_COLOR = new ColorRgba(0.05f, 0.83f, 0.07f, 0.5f);

	public ScreenLayerStarStatistics(HawkScreen attachedScreen, SectorTicket<?> ticket, Vec3 center) {
		super(attachedScreen);
		this.galaxy = ticket.attachedManager.galaxy;
		this.ticket = ticket;
		this.center = center;
	}

	private static enum Variable {
		LUMINSOITY("Luminosity", "Lsol", 1e-4, 1e5),
		TEMPERATURE("Temperature", "K", 1000, 60000),
		MASS("Mass", "Msol", 0.08, 300),
		AGE("Age", "Myr", 1e-6, 1e10),
		DISTANCE("Distance", "pc", 1, 1e5);

		public final String label;
		public final String units;
		public final Interval interval;

		private Variable(String label, String units, double min, double max) {
			this.label = label;
			this.units = units;
			this.interval = new Interval(min, max);
		}
	}

	private double selectVariable(GalaxySector.ElementHolder elem, Variable variable) {
		return switch (variable) {
			case LUMINSOITY -> elem.luminosityLsol;
			case TEMPERATURE -> elem.temperatureK;
			case MASS -> elem.massYg * Units.Msol_PER_Yg;
			case AGE -> elem.systemAgeMyr;
			case DISTANCE -> elem.systemPosTm.distanceTo(this.center) * Units.pc_PER_Tm;
		};
	}

	private void createScatterPlot() {
		this.xyPlot = new ScatterPlot(
				this.xVariable.label, this.xVariable.units,
				this.yVariable.label, this.yVariable.units);
		this.xHistogram = new Histogram(this.xVariable.label, 1024, this.xMapping);
		this.yHistogram = new Histogram(this.yVariable.label, 1024, this.yMapping);
		final var elem = new GalaxySector.ElementHolder();
		ticket.attachedManager.enumerate(ticket, sector -> {
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				final var x = selectVariable(elem, this.xVariable);
				final var y = selectVariable(elem, this.yVariable);
				this.xyPlot.insert(x, y);
				this.xHistogram.insert(x);
				this.yHistogram.insert(y);
			}
		});
	}

	@FunctionalInterface
	private interface GuideConsumer {
		void accept(double pos, ColorRgba color);
	}

	private void renderGuides(AxisMapping mapping, GuideConsumer consumer) {
		if (mapping instanceof AxisMapping.Log logMapping) {
			final var lmin = Mth.floor(Math.log(logMapping.domain.min) / Math.log(logMapping.base));
			final var lmax = Mth.ceil(Math.log(logMapping.domain.max) / Math.log(logMapping.base));
			if (Math.abs(lmax - lmin) > 100)
				return;
			for (int i = lmin; i < lmax; ++i) {
				final var tl = Math.pow(logMapping.base, i);
				final var th = Math.pow(logMapping.base, i + 1);
				for (int j = 1; j < 10; ++j) {
					final var tMinor = Mth.lerp(j / 10.0, tl, th);
					consumer.accept(logMapping.remap(tMinor), MINOR_MARKER_COLOR);
				}
				final var color = Math.abs(tl - 1.0) < 1e-24
						? AXIS_MARKER_COLOR
						: MAJOR_MARKER_COLOR;
				consumer.accept(logMapping.remap(tl), color);
			}
		} else if (mapping instanceof AxisMapping.Linear linearMapping) {
			final var inc = Math.pow(10,
					Mth.floor(Math.log(linearMapping.domain.max - linearMapping.domain.min) / Math.log(10)));
			final var lmin = inc * Math.floor(linearMapping.domain.min / inc);
			final var lmax = inc * Math.ceil(linearMapping.domain.max / inc);
			for (double t = lmin; t < lmax; t += inc) {
				for (int j = 1; j < 8; ++j) {
					final var tMinor = Mth.lerp(j / 8.0, t, t + inc);
					consumer.accept(linearMapping.remap(tMinor), MINOR_MARKER_COLOR);
				}
				final var color = Math.abs(t) < 1e-24
						? AXIS_MARKER_COLOR
						: MAJOR_MARKER_COLOR;
				consumer.accept(linearMapping.remap(t), color);
			}
		}
	}

	private void setupCamera(RenderContext ctx) {
		final double frustumDepth = 400;
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final var projMat = new Mat4.Mutable();
		final var projLR = aspectRatio * this.scale.get(ctx.partialTick);
		final var projTB = this.scale.get(ctx.partialTick);
		Mat4.setOrthographicProjection(projMat, -projLR, projLR, projTB, -projTB, -frustumDepth, 0);

		final var viewMat = new Mat4.Mutable();
		viewMat.loadIdentity();
		viewMat.appendScale(1.5);
		viewMat.appendTranslation(this.offset.get(ctx.partialTick).withZ(-0.5 * frustumDepth));
		Mat4.invert(viewMat, viewMat);

		this.camera.load(viewMat, projMat, 1);
	}

	private void renderHistogram(RenderContext ctx, AxisMapping mapping, Histogram plot, Rect bounds,
			boolean transpose) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.QUAD_DUPLICATED,
				BufferLayout.POSITION_COLOR_TEX);

		int max = 0;
		for (int i = 0; i < plot.size(); ++i) {
			max = Math.max(max, plot.get(i));
		}

		for (int i = 0; i < plot.size(); ++i) {
			final var percent = plot.get(i) / (double) max;

			double l = mapping.unmap(i / (double) plot.size());
			double h = mapping.unmap((i + 1) / (double) plot.size());

			l = mapping.remap(l);
			h = mapping.remap(h);

			final double lx, hx, ly, hy;
			if (transpose) {
				ly = Mth.lerp(l, bounds.min().y, bounds.max().y);
				hy = Mth.lerp(h, bounds.min().y, bounds.max().y);
				lx = bounds.max().x;
				hx = Mth.lerp(percent, bounds.max().x, bounds.min().x);
			} else {
				lx = Mth.lerp(l, bounds.min().x, bounds.max().x);
				hx = Mth.lerp(h, bounds.min().x, bounds.max().x);
				ly = bounds.max().y;
				hy = Mth.lerp(percent, bounds.max().y, bounds.min().y);
			}

			builder.vertex(hx, ly, 0).color(HISTOGRAM_COLOR).uv0(1, 0).endVertex();
			builder.vertex(lx, ly, 0).color(HISTOGRAM_COLOR).uv0(0, 0).endVertex();
			builder.vertex(lx, hy, 0).color(HISTOGRAM_COLOR).uv0(0, 1).endVertex();
			builder.vertex(hx, hy, 0).color(HISTOGRAM_COLOR).uv0(1, 1).endVertex();
		}

		builder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	private void renderScatterPlot(RenderContext ctx, ScatterPlot plot, Rect bounds) {
		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.LINE_DUPLICATED,
				BufferLayout.POSITION_COLOR_NORMAL);

		// guides
		renderGuides(this.xMapping, (pos, color) -> {
			if (pos <= 0 || pos >= 1)
				return;
			final var x = Mth.lerp(pos, bounds.min().x, bounds.max().x);
			final var l = new Vec3(x, bounds.min().y, 0);
			final var h = new Vec3(x, bounds.max().y, 0);
			RenderHelper.addLine(lineBuilder, l, h, color);
		});
		renderGuides(this.yMapping, (pos, color) -> {
			if (pos <= 0 || pos >= 1)
				return;
			final var y = Mth.lerp(pos, bounds.min().y, bounds.max().y);
			final var l = new Vec3(bounds.min().x, y, 0);
			final var h = new Vec3(bounds.max().x, y, 0);
			RenderHelper.addLine(lineBuilder, l, h, color);
		});

		// bounds
		RenderHelper.addLine(lineBuilder, new Vec3(bounds.min().x, bounds.min().y, 0),
				new Vec3(bounds.min().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, new Vec3(bounds.max().x, bounds.min().y, 0),
				new Vec3(bounds.max().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, new Vec3(bounds.min().x, bounds.min().y, 0),
				new Vec3(bounds.max().x, bounds.min().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, new Vec3(bounds.min().x, bounds.max().y, 0),
				new Vec3(bounds.max().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);

		RenderSystem.lineWidth(1f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		// data
		final var pointBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.POINT_DUPLICATED, BufferLayout.POSITION_COLOR_TEX);
		for (int i = 0; i < this.xyPlot.size(); ++i) {
			double x = this.xyPlot.getX(i), y = this.xyPlot.getY(i);
			x = Mth.lerp(this.xMapping.remap(x), bounds.min().x, bounds.max().x);
			y = Mth.lerp(this.yMapping.remap(y), bounds.min().y, bounds.max().y);
			pointBuilder.vertex(x, y, 0).color(1, 1, 1, 0.1f).uv0(5f, 5f).endVertex();
		}
		pointBuilder.end().draw(UltravioletShaders.SHADER_UI_POINTS.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		// StringRenderOutput stringRenderOutput = new StringRenderOutput(bufferSource,
		// x, y, color, dropShadow, pose, seeThrough, packedLightCoords);
		// return stringRenderOutput.finish(backgroundColor, x);
		// client.font.draw(poseStack, text, 10, this.height, 0xffffffff);

		// final var sink = new TestCharSink();
		// sink.emit(Mat4.IDENTITY, FormattedText.of("among\nus"), TestCharSink.TextAlignment.NATIVE);
		// sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	private static final class TestCharSink implements FormattedCharSink {

		private final Font font = Minecraft.getInstance().font;
		private double x = 0, y = 0;
		private Matrix4f cachedMatrix;
		private MutableMap<ResourceLocation, Vector<PositionedGlyph>> builtGlyphs = MutableMap.hashMap();

		public enum TextAlignment {
			NATIVE, CENTER, ANTI_NATIVE, LEFT, RIGHT
		}

		public enum TextOrigin {
			NEG_NEG, CEN_NEG, POS_NEG,
			NEG_CEN, CEN_CEN, POS_CEN,
			NEG_POS, CEN_POS, POS_POS,
		}

		public TestCharSink() {
		}

		record PositionedGlyph(BakedGlyph glyph, float x, float y, Style style, float r, float g, float b, float a) {
			public void draw(Matrix4f matrix, VertexConsumer builder) {
				// final var mat = new Matrix4f();
				// mat.setIdentity();
				// final var scale = 0.0075f;
				// final var mat = Matrix4f.createScaleMatrix(scale, -scale, scale);
				this.glyph.render(style.isItalic(), x, -y, matrix, builder, r, g, b, a, 0xF000F0);
			}
		}

		private static final Matrix4f IDENTITY = new Matrix4f();
		static {
			IDENTITY.setIdentity();
		}

		// lmfao
		private static ResourceLocation extractTextureFromRenderType(RenderType renderType) {
			if (renderType instanceof RenderType.CompositeRenderType composite) {
				if (composite.state.textureState instanceof RenderStateShard.TextureStateShard shard) {
					if (shard.texture.isPresent()) {
						return shard.texture.get();
					}
				}
			}
			throw new IllegalArgumentException(String.format(
					"RenderType {} has no TextureStateShard",
					renderType.toString()));
		}

		@Override
		public boolean accept(int var1, Style style, int character) {
			final var fontSet = FontAccessor.getFontSet(this.font, style.getFont());
			final var glyphInfo = fontSet.getGlyphInfo(character);
			final var bakedGlyph = style.isObfuscated() && !Character.isSpaceChar(character)
					? fontSet.getRandomGlyph(glyphInfo)
					: fontSet.getGlyph(character);

			float r = 1f, g = 1f, b = 1f, a = 1f;
			final var textColor = style.getColor();
			if (textColor != null) {
				final var k = textColor.getValue();
				r = (float) (k >> 16 & 0xFF) / 255f;
				g = (float) (k >> 8 & 0xFF) / 255f;
				b = (float) (k & 0xFF) / 255f;
			}

			final var renderType = bakedGlyph.renderType(Font.DisplayMode.NORMAL);
			final var texture = extractTextureFromRenderType(renderType);
			if (!(bakedGlyph instanceof EmptyGlyph)) {
				final var glyphs = this.builtGlyphs.entry(texture).orInsertWith(Vector::new);
				glyphs.push(new PositionedGlyph(bakedGlyph, (float) this.x, (float) this.y, style, r, g, b, a));
			}

			this.x += glyphInfo.getAdvance(style.isBold());
			return true;
		}

		public void emit(Mat4Access tfm, FormattedText text, TextAlignment align) {
			tfm.storeMinecraft(this.cachedMatrix);
			final var lines = this.font.split(text, Integer.MAX_VALUE);
			final var height = this.font.lineHeight * lines.size();
			this.y = 0;
			for (final var cs : lines) {
				final var width = this.font.width(cs);
				this.x = 0;
				// FIXME: right-to-left text?
				switch (align) {
					case NATIVE -> {
					}
					case ANTI_NATIVE -> {
					}
					case LEFT -> {
					}
					case CENTER -> {
					}
					case RIGHT -> {
					}
				}
				// this.x =
				cs.accept(this);
				this.y += this.font.lineHeight;
			}
			// StringDecomposer.iterateFormatted(cs, style, this);
		}

		public void draw(VertexBuilder builder, DrawState drawState) {
			for (final var entry : this.builtGlyphs.entries().iterable()) {
				final var fontTexture = GlTexture2d.importTexture(entry.key);
				fontTexture.setMinFilter(GlTexture.MinFilter.NEAREST);
				fontTexture.setMagFilter(GlTexture.MagFilter.NEAREST);

				final var shader = UltravioletShaders.SHADER_VANILLA_RENDERTYPE_TEXT.get();
				shader.setupDefaultShaderUniforms();
				shader.setUniformSampler("Sampler2", LightTextureAccessor.get());
				shader.setUniformf("ColorModulator", ColorRgba.WHITE);
				shader.setUniformSampler("Sampler0", fontTexture);

				final var glyphBuilder = builder.beginGeneric(PrimitiveType.QUAD_DUPLICATED,
						BufferLayout.POSITION_COLOR_TEX_LIGHTMAP);
				final var consumer = glyphBuilder.asVanilla();
				for (final var glyph : entry.getOrThrow().iterable()) {
					glyph.draw(this.cachedMatrix, consumer);
				}
				glyphBuilder.end().draw(shader, drawState);
			}

			this.builtGlyphs.clear();
		}

	}

	@Override
	public void render(RenderContext ctx) {
		if (this.xyPlot == null)
			createScatterPlot();
		setupCamera(ctx);

		final var snapshot = RenderMatricesSnapshot.capture();
		this.camera.applyProjection();
		this.camera.applyView();

		renderScatterPlot(ctx, this.xyPlot, Rect.BIPOLAR);
		renderHistogram(ctx, this.xMapping, this.xHistogram, new Rect(-1, -1.3, 1, -1.05), false);
		renderHistogram(ctx, this.yMapping, this.yHistogram, new Rect(-1.3, -1, -1.05, 1), true);

		snapshot.restore();
	}

}
