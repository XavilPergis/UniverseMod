package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.MotionSmoother;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.NumericOps;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;

public class ScreenLayerStarStatistics extends HawkScreen.Layer2d {

	public final Galaxy galaxy;
	public final SectorTicket<?> ticket;

	private ScatterPlot plot = null;
	private Variable xVariable = Variable.TEMPERATURE, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.AGE, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.MASS, yVariable = Variable.LUMINSOITY;
	// private Variable xVariable = Variable.MASS, yVariable = Variable.TEMPERATURE;
	// private AxisMapping xMapping = AxisMapping.LOGE, yMapping = AxisMapping.LOGE;

	public MotionSmoother<Double> scale = new MotionSmoother<>(0.6, NumericOps.DOUBLE, 1.0);
	public MotionSmoother<Vec2> offset = new MotionSmoother<>(0.6, NumericOps.VEC2, Vec2.ZERO);
	public double scaleMin = 0.3, scaleMax = 30;
	public double scrollMultiplier = 1.2;

	private double animationTimer = 0.0;

	private CachedCamera camera = new CachedCamera();

	public ScreenLayerStarStatistics(HawkScreen attachedScreen, SectorTicket<?> ticket) {
		super(attachedScreen);
		this.galaxy = ticket.attachedManager.galaxy;
		this.ticket = ticket;
	}

	// private void renderScatterPlotToTexture(RenderContext ctx, ScatterPlot plot,
	// RenderTexture target) {
	// target.framebuffer.bind();
	// ctx.currentTexture.framebuffer.bind();
	// }

	private static enum Variable {
		LUMINSOITY("Luminosity", "Lsol", 1e-4, 1e5),
		TEMPERATURE("Temperature", "K", 1900, 60000),
		MASS("Mass", "Msol", 0.08, 300),
		AGE("Age", "Myr", 1e-6, 1e10);

		public final String label;
		public final String units;
		public final double min, max;

		private Variable(String label, String units, double min, double max) {
			this.label = label;
			this.units = units;
			this.min = min;
			this.max = max;
		}
	}

	private static enum DisplayMapping {
		LINEAR(0),
		LOG2(2),
		LOGE(Math.E),
		LOG10(10);

		public double logBase;

		private DisplayMapping(double logBase) {
			this.logBase = logBase;
		}
	}

	private double selectVariable(GalaxySector.ElementHolder elem, Variable variable) {
		return switch (variable) {
			case LUMINSOITY -> elem.luminosityLsol;
			case TEMPERATURE -> elem.temperatureK;
			case MASS -> elem.massYg * Units.Msol_PER_Yg;
			case AGE -> elem.systemAgeMyr;
		};
	}

	private void createScatterPlot() {
		this.plot = new ScatterPlot(
				this.xVariable.label, this.xVariable.units,
				this.yVariable.label, this.yVariable.units);
		final var elem = new GalaxySector.ElementHolder();
		ticket.attachedManager.enumerate(ticket, sector -> {
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				final var x = selectVariable(elem, this.xVariable);
				final var y = selectVariable(elem, this.yVariable);
				this.plot.insert(x, y);
			}
		});
	}

	private static abstract sealed class AxisMapping {

		public abstract double remap(double t);

		public static final class Linear extends AxisMapping {
			public double min = 0, max = 1000;

			public Linear() {
			}

			public Linear(double min, double max) {
				this.min = min;
				this.max = max;
			}

			@Override
			public double remap(double t) {
				return Mth.inverseLerp(t, this.min, this.max);
			}
		}

		public static final class Log extends AxisMapping {
			public double base = 10;
			public double min = 1e-3, max = 1e5;

			public Log() {
			}

			public Log(double base, double min, double max) {
				this.base = base;
				this.min = min;
				this.max = max;
			}

			@Override
			public double remap(double t) {
				t = Math.log(t) / Math.log(this.base);
				final var min = Math.log(this.min) / Math.log(this.base);
				final var max = Math.log(this.max) / Math.log(this.base);
				return Mth.inverseLerp(t, min, max);
			}
		}

	}

	@FunctionalInterface
	private interface GuideConsumer {
		void accept(double pos, boolean isMajor);
	}

	private void renderGuidesLogarithmic(AxisMapping.Log mapping, GuideConsumer consumer) {
		// final var lmin = Math.pow(Math.floor(Math.log(mapping.min) / Math.log(mapping.base)), mapping.base);
		// final var lmax = Math.pow(Math.ceil(Math.log(mapping.max) / Math.log(mapping.base)), mapping.base);
		final var lmin = Mth.floor(Math.log(mapping.min) / Math.log(mapping.base));
		final var lmax = Mth.ceil(Math.log(mapping.max) / Math.log(mapping.base));
		if (Math.abs(lmax - lmin) > 100)
			return;
		for (int i = lmin; i < lmax; ++i) {
			final var tl = Math.pow(mapping.base, i);
			final var th = Math.pow(mapping.base, i + 1);
			for (int j = 1; j < 10; ++j) {
				final var tMinor = Mth.lerp(j / 10.0, tl, th);
				consumer.accept(mapping.remap(tMinor), false);
			}
			consumer.accept(mapping.remap(tl), true);
		}

	}

	private void renderScatterPlot(RenderContext ctx, ScatterPlot plot) {

		final double frustumDepth = 400;
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final var projMat = new Mat4.Mutable();
		final var projLR = aspectRatio * this.scale.get(ctx.partialTick);
		final var projTB = this.scale.get(ctx.partialTick);
		Mat4.setOrthographicProjection(projMat, 0, projLR, projTB, 0, -frustumDepth, 0);

		final var viewMat = new Mat4.Mutable();
		viewMat.loadIdentity();
		viewMat.appendTranslation(this.offset.get(ctx.partialTick).withZ(-0.5 * frustumDepth));
		Mat4.invert(viewMat, viewMat);

		this.camera.load(viewMat, projMat, 1);

		final var snapshot = RenderMatricesSnapshot.capture();
		this.camera.applyProjection();
		this.camera.applyView();

		final var xMapping = new AxisMapping.Log(10, this.xVariable.min, this.xVariable.max);
		final var yMapping = new AxisMapping.Log(10, this.yVariable.min, this.yVariable.max);

		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.LINES,
				BufferLayout.POSITION_COLOR_NORMAL);

		renderGuidesLogarithmic(xMapping, (pos, isMajor) -> {
			final var color = isMajor ? MAJOR_MARKER_COLOR : MINOR_MARKER_COLOR.withA(0.1f);
			RenderHelper.addLine(lineBuilder, new Vec3(pos, 0, 0), new Vec3(pos, 1, 0), color);
		});

		renderGuidesLogarithmic(yMapping, (pos, isMajor) -> {
			final var color = isMajor ? MAJOR_MARKER_COLOR : MINOR_MARKER_COLOR.withA(0.1f);
			RenderHelper.addLine(lineBuilder, new Vec3(0, pos, 0), new Vec3(1, pos, 0), color);
		});

		RenderSystem.lineWidth(1f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		final var pointBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.POINT_QUADS, BufferLayout.POSITION_COLOR_NORMAL);

		for (int i = 0; i < this.plot.size(); ++i) {
			double x = this.plot.getX(i), y = this.plot.getY(i);
			x = xMapping.remap(x);
			y = yMapping.remap(y);
			pointBuilder.vertex(x, y, 0)
					.color(1, 1, 1, 0.1f)
					.endVertex();
		}

		final var shader = UltravioletShaders.SHADER_UI_POINTS.get();
		shader.setUniformf("uPointSize", 1f);

		pointBuilder.end().draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		snapshot.restore();
	}

	private static final ColorRgba MAJOR_MARKER_COLOR = new ColorRgba(1f, 1f, 1f, 0.8f);
	private static final ColorRgba MINOR_MARKER_COLOR = new ColorRgba(0.5f, 0.5f, 0.5f, 0.8f);

	@Override
	public void render(RenderContext ctx) {
		if (this.plot == null)
			createScatterPlot();
		renderScatterPlot(ctx, this.plot);
	}

}
