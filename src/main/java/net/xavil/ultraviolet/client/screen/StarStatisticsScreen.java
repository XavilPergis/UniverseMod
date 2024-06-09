package net.xavil.ultraviolet.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.MotionSmoother;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.vertex.VertexDispatcher;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.NumericOps;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Rect;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.layer.AxisMapping;
import net.xavil.ultraviolet.client.screen.layer.Histogram;
import net.xavil.ultraviolet.client.screen.layer.ScatterPlot;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.client.screen.layer.TextBuilder;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;

public class StarStatisticsScreen extends HawkScreen {

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
	public double scaleMin = 0.05, scaleMax = 5;
	public double scrollMultiplier = 1.2;

	private CachedCamera camera = new CachedCamera();
	private Vec3 center;
	private Mesh pointsMesh;

	private static final ColorRgba AXIS_MARKER_COLOR = new ColorRgba(0.05f, 0.83f, 0.07f, 1f);
	private static final ColorRgba MAJOR_MARKER_COLOR = new ColorRgba(1f, 1f, 1f, 0.8f);
	private static final ColorRgba MINOR_MARKER_COLOR = new ColorRgba(0.5f, 0.5f, 0.5f, 0.1f);
	private static final ColorRgba HISTOGRAM_BIN_COLOR = new ColorRgba(0.05f, 0.83f, 0.07f, 0.5f);
	private static final ColorRgba HISTOGRAM_MEAN_COLOR = HISTOGRAM_BIN_COLOR.invertRgb();
	private static final ColorRgba HISTOGRAM_MAX_COLOR = new ColorRgba(0.05f, 0.90f, 0.80f, 0.5f);

	public StarStatisticsScreen(Screen previousScreen, SectorTicket<?> ticket, Vec3 center) {
		super(new TranslatableComponent("narrator.screen.star_statistics"), previousScreen);

		this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));
		// this.layers.push(new ScreenLayerStarStatistics(this, ticket, center));

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

	// public interface

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

				if (this.ticket.info instanceof SectorTicketInfo.Multi multi) {
					if (elem.systemPosTm.distanceTo(this.center) > multi.radiusForLevel(sector.level))
						continue;
				}

				final var x = selectVariable(elem, this.xVariable);
				final var y = selectVariable(elem, this.yVariable);
				this.xyPlot.insert(x, y);
				this.xHistogram.insert(x);
				this.yHistogram.insert(y);
			}
		});
	}

	private void createMesh(Rect bounds) {
		this.pointsMesh = new Mesh();

		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.POINT,
				BufferLayout.POSITION_COLOR_TEX);

		final var color = new Vec3.Mutable();
		for (int i = 0; i < this.xyPlot.size(); ++i) {
			double x = this.xyPlot.getX(i), y = this.xyPlot.getY(i);

			Vec3.set(color, 1, 1, 1);
			if (this.xVariable == Variable.TEMPERATURE)
				StellarCelestialNode.BLACK_BODY_COLOR_TABLE.lookupColor(color, x);
			else if (this.yVariable == Variable.TEMPERATURE)
				StellarCelestialNode.BLACK_BODY_COLOR_TABLE.lookupColor(color, y);

			x = Mth.lerp(this.xMapping.remap(x), bounds.min().x, bounds.max().x);
			y = Mth.lerp(this.yMapping.remap(y), bounds.min().y, bounds.max().y);
			builder.vertex(x, y, 0)
					.color((float) color.x, (float) color.y, (float) color.z, 0.2f)
					.uv0(4f, 4f)
					.endVertex();
		}

		this.pointsMesh.setupAndUpload(builder.end());
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

		this.scale.tick(ctx.deltaTime);
		this.offset.tick(ctx.deltaTime);

		// setup camera for ortho UI
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final double frustumDepth = 400;

		final var projMat = new Mat4.Mutable();
		final var projLR = aspectRatio * this.scale.current;
		final var projTB = this.scale.current;
		Mat4.setOrthographicProjection(projMat, -projLR, projLR, projTB, -projTB, -frustumDepth, 0);

		final var inverseViewMat = new Mat4.Mutable();
		inverseViewMat.loadIdentity();
		inverseViewMat.appendTranslation(this.offset.current.withZ(0.5 * frustumDepth));

		this.camera.load(inverseViewMat, projMat, 1);
	}

	private void renderHistogramMarker(VertexDispatcher.Generic builder,
			double t, Rect bounds, boolean transpose, ColorRgba color) {
		final double x1, y1, x2, y2;
		if (transpose) {
			y1 = y2 = Mth.lerp(t, bounds.min().y, bounds.max().y);
			x1 = bounds.min().x;
			x2 = bounds.max().x;
		} else {
			x1 = x2 = Mth.lerp(t, bounds.min().x, bounds.max().x);
			y1 = bounds.min().y;
			y2 = bounds.max().y;
		}

		RenderHelper.addLine(builder, new Vec3(x1, y1, 0), new Vec3(x2, y2, 0), color);
	}

	private void renderHistogram(RenderContext ctx, AxisMapping mapping, Histogram plot, Rect bounds,
			boolean transpose) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.QUAD_DUPLICATED,
				BufferLayout.POSITION_COLOR_TEX);

		int max = 0;
		final var maxIndices = new VectorInt();
		for (int i = 0; i < plot.size(); ++i) {
			final var binCount = plot.get(i);
			if (binCount > max)
				maxIndices.clear();
			if (binCount >= max) {
				maxIndices.push(i);
				max = binCount;
			}
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

			builder.vertex(hx, ly, 0).color(HISTOGRAM_BIN_COLOR).uv0(1, 0).endVertex();
			builder.vertex(lx, ly, 0).color(HISTOGRAM_BIN_COLOR).uv0(0, 0).endVertex();
			builder.vertex(lx, hy, 0).color(HISTOGRAM_BIN_COLOR).uv0(0, 1).endVertex();
			builder.vertex(hx, hy, 0).color(HISTOGRAM_BIN_COLOR).uv0(1, 1).endVertex();
		}

		builder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		if (plot.hasMean()) {
			final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.LINE_DUPLICATED,
					BufferLayout.POSITION_COLOR_NORMAL);

			for (int i = 0; i < maxIndices.size(); ++i) {
				final var t = maxIndices.get(i) / (plot.size() - 1.0);
				renderHistogramMarker(lineBuilder, t, bounds, transpose, HISTOGRAM_MAX_COLOR);
			}
			renderHistogramMarker(lineBuilder, mapping.remap(plot.getMean()), bounds, transpose, HISTOGRAM_MEAN_COLOR);

			RenderSystem.lineWidth(1f);
			lineBuilder.end().draw(
					UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
		}

		if (plot.hasMean()) {
			final double x, y;
			y = bounds.min().x;
			x = bounds.min().y;
			// if (transpose) {
			// } else {
			// 	x = bounds.min().x;
			// 	y = bounds.min().y;
			// }

			final var sink = new TextBuilder();
			final var tfm = new TransformStack();
			if (!transpose) {
				tfm.appendRotation(Quat.axisAngle(Vec3.ZN, -Math.PI / 2));
			}
			tfm.appendScale(0.005f);
			tfm.appendTranslation(new Vec3(x, y, 0));
			sink.emit(tfm.current(), FormattedText.of(String.format("Mean: %f", plot.getMean())));
			sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);
		}
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
		UltravioletShaders.SHADER_UI_POINTS.get().setupDefaultShaderUniforms();
		this.pointsMesh.draw(UltravioletShaders.SHADER_UI_POINTS.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ADDITIVE_BLENDING);
	}

	@Override
	public void renderScreenPostLayers(RenderContext ctx) {
		if (this.xyPlot == null)
			createScatterPlot();
		if (this.pointsMesh == null)
			createMesh(Rect.BIPOLAR);
		setupCamera(ctx);

		final var snapshot = RenderMatricesSnapshot.capture();
		this.camera.applyProjection();
		this.camera.applyView();

		renderScatterPlot(ctx, this.xyPlot, Rect.BIPOLAR);
		renderHistogram(ctx, this.xMapping, this.xHistogram, new Rect(-1, -1.3, 1, -1.05), false);
		renderHistogram(ctx, this.yMapping, this.yHistogram, new Rect(-1.3, -1, -1.05, 1), true);
		
		final var sink = new TextBuilder();
		final var tfm = new TransformStack();
		tfm.appendScale(0.005f);
		tfm.appendTranslation(new Vec3(1.02, 0.98, 0));
		sink.emitNewline(tfm.current(), FormattedText.of(String.format("n=%d", this.xyPlot.size())));
		sink.emitNewline(tfm.current(), FormattedText.of(String.format("avg(X)=%.2f %s", this.xHistogram.getMean(), this.xVariable.units)));
		sink.emitNewline(tfm.current(), FormattedText.of(String.format("avg(Y)=%.2f %s", this.yHistogram.getMean(), this.yVariable.units)));
		sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);

		snapshot.restore();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (this.pointsMesh != null)
			this.pointsMesh.close();
	}

	@Override
	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		if (scrollDelta > 0) {
			this.scale.target = Math.max(this.scale.target / scrollMultiplier, this.scaleMin);
			return true;
		} else if (scrollDelta < 0) {
			this.scale.target = Math.min(this.scale.target * scrollMultiplier, this.scaleMax);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(Vec2 mousePos, Vec2 delta, int button) {
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final var sizeXp = (double) window.getWidth();
		final var sizeYp = (double) window.getHeight();
		final var sizeXu = 4.0 * aspectRatio * this.scale.current;
		final var sizeYu = 4.0 * this.scale.current;

		final var dx = delta.x * (sizeXu / sizeXp);
		final var dy = delta.y * (sizeYu / sizeYp);

		this.setDragging(true);
		this.offset.target = this.offset.target.add(new Vec2(dx, -dy));
		return true;
	}

}
