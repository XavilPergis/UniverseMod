package net.xavil.ultraviolet.client.screen;

import java.util.function.Consumer;

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
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.vertex.VertexDispatcher;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Rect;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
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
import net.xavil.ultraviolet.common.universe.system.StellarProperties;

public class StarStatisticsScreen extends HawkScreen {

	public final Galaxy galaxy;
	public final SectorTicket<?> ticket;

	private ScatterPlot xyPlot = null;
	private Histogram xHistogram = null, yHistogram = null;
	private Histogram[] levelHistogramsX = {}, levelHistogramsY = {};

	// private Variable xVariable = Variable.TEMPERATURE, yVariable =
	// Variable.ILLUMINANCE;
	private Variable xVariable = Variable.TEMPERATURE, yVariable = Variable.LUMINOUS_FLUX;
	// private Variable xVariable = Variable.AGE, yVariable =
	// Variable.LUMINOUS_FLUX;
	// private Variable xVariable = Variable.AGE, yVariable = Variable.TEMPERATURE;
	// private Variable xVariable = Variable.AGE, yVariable = Variable.MASS;
	// private Variable xVariable = Variable.LUMINOUS_FLUX, yVariable =
	// Variable.ILLUMINANCE;
	// private AxisMapping xMapping = new AxisMapping.Log(10, 1e3, 1e9);
	// private AxisMapping yMapping = new AxisMapping.Log(10, 1e-10, 1e0);

	// private Variable xVariable = Variable.TEMPERATURE, yVariable =
	// Variable.RADIANT_FLUX;
	// private Variable xVariable = Variable.DISTANCE, yVariable =
	// Variable.TEMPERATURE;
	// private Variable xVariable = Variable.AGE, yVariable = Variable.RADIANT_FLUX;
	// private Variable xVariable = Variable.MASS, yVariable =
	// Variable.RADIANT_FLUX;
	// private Variable xVariable = Variable.MASS, yVariable = Variable.TEMPERATURE;

	// private AxisMapping xMapping = new AxisMapping.Log(10, 1, 1e5);
	// private AxisMapping xMapping = new AxisMapping.Linear(0, 13000);
	private AxisMapping xMapping = new AxisMapping.Log(10, this.xVariable.interval);
	// private AxisMapping yMapping = new AxisMapping.Log(10,
	// this.yVariable.interval);
	private AxisMapping yMapping = new AxisMapping.Log(10, new Interval(1e1, 1e12));

	public MotionSmoother<Double> scale = new MotionSmoother<>(0.6, MotionSmoother.Interpolator.DOUBLE, 1.4);
	public MotionSmoother<Vec2> offset = new MotionSmoother<>(0.6, MotionSmoother.Interpolator.VEC2, Vec2.ZERO);
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
		RADIANT_FLUX("Radiant Flux", "Lsol", 1e-4, 1e5),
		LUMINOUS_FLUX("Luminous Flux", "Ylm", 1e1, 1e12),
		IRRADIANCE("Irradiance", "W/m^2", 1e-10, 1e0),
		ILLUMINANCE("Illuminance", "lx", 1e-10, 1e0),
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

	// conversion factor from `Lsol pc^-2` to `W m^-2`
	private static final double IRRADIANCE_FACTOR = Units.W_PER_Lsol * 1e-24 * Units.pc_PER_Tm * Units.pc_PER_Tm;
	public static final double LUMENS_PER_WATT = 683.002;

	private double selectVariable(GalaxySector.ElementHolder elem, Variable variable) {
		return switch (variable) {
			case TEMPERATURE -> elem.temperatureK;
			case MASS -> elem.massYg * Units.Msol_PER_Yg;
			case AGE -> elem.systemAgeMyr;
			case RADIANT_FLUX -> elem.luminosityLsol;
			case LUMINOUS_FLUX -> {
				final var brightnessMult = StellarCelestialNode.BLACK_BODY_COLOR_TABLE
						.lookupBrightnessMultiplier(elem.temperatureK);
				// i mean, were really converting `W/m^2` to `lm/m^2` here, but it should have
				// the same effect. :p
				yield LUMENS_PER_WATT * brightnessMult * (Units.W_PER_Lsol * Units.Yu_PER_u) * elem.luminosityLsol;
			}
			case IRRADIANCE -> {
				final var distance = elem.systemPosTm.distanceTo(this.center) * Units.pc_PER_Tm;
				final var irradiance = IRRADIANCE_FACTOR * elem.luminosityLsol / (4 * Math.PI * distance * distance);
				yield irradiance;
			}
			case ILLUMINANCE -> {
				final var distance = elem.systemPosTm.distanceTo(this.center) * Units.pc_PER_Tm;
				final var irradiance = IRRADIANCE_FACTOR * elem.luminosityLsol / (4 * Math.PI * distance * distance);

				final var brightnessMult = StellarCelestialNode.BLACK_BODY_COLOR_TABLE
						.lookupBrightnessMultiplier(elem.temperatureK);
				// i mean, were really converting `W/m^2` to `lm/m^2` here, but it should have
				// the same effect. :p
				yield LUMENS_PER_WATT * brightnessMult * irradiance;
			}
			case DISTANCE -> elem.systemPosTm.distanceTo(this.center) * Units.pc_PER_Tm;
		};
	}

	static abstract class Widget {
		public Rect rect;

		public void render() {
		}
	}

	static final class ButtonWidget extends Widget {
		private Consumer<MousePress> action;
	}

	private final Vector<Widget> widgets = new Vector<>();

	// public interface

	private void createPlotsFromSurvey() {
		this.xyPlot = new ScatterPlot(
				this.xVariable.label, this.xVariable.units,
				this.yVariable.label, this.yVariable.units);
		this.xHistogram = new Histogram(this.xVariable.label, 1024, this.xMapping);
		this.yHistogram = new Histogram(this.yVariable.label, 1024, this.yMapping);
		this.levelHistogramsX = new Histogram[GalaxySector.LEVEL_COUNT];
		this.levelHistogramsY = new Histogram[GalaxySector.LEVEL_COUNT];
		for (int i = 0; i < GalaxySector.LEVEL_COUNT; ++i) {
			this.levelHistogramsX[i] = new Histogram(this.xVariable.label, 1024, this.xMapping);
			this.levelHistogramsY[i] = new Histogram(this.yVariable.label, 1024, this.yMapping);
		}

		final var elem = new GalaxySector.ElementHolder();
		ticket.attachedManager.enumerate(ticket, sector -> {
			final var levelHistX = this.levelHistogramsX[sector.level];
			final var levelHistY = this.levelHistogramsY[sector.level];
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
				levelHistX.insert(x);
				levelHistY.insert(y);
			}
		});
	}

	private void createPlotsFromInterpolation() {
		this.xyPlot = new ScatterPlot(
				this.xVariable.label, this.xVariable.units,
				this.yVariable.label, this.yVariable.units);
		this.xHistogram = new Histogram(this.xVariable.label, 1024, this.xMapping);
		this.yHistogram = new Histogram(this.yVariable.label, 1024, this.yMapping);
		this.levelHistogramsX = this.levelHistogramsY = new Histogram[0];

		final var elem = new GalaxySector.ElementHolder();

		var massInputs = new double[1];
		var ageInputs = new double[4096];
		var metallicityInputs = new double[1];

		final AxisMapping MASS_INTERVAL = new AxisMapping.Log(Math.E, Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 100);
		final AxisMapping AGE_INTERVAL = new AxisMapping.Linear(0, 13000);
		final AxisMapping METALLICITY_INTERVAL = new AxisMapping.Linear(Galaxy.METALLICITY_RANGE);

		// for (int i = 0; i < massInputs.length; ++i)
		// massInputs[i] = MASS_INTERVAL.unmap(i / (massInputs.length - 1d));
		massInputs = new double[] { 1 };
		for (int i = 0; i < ageInputs.length; ++i)
			ageInputs[i] = AGE_INTERVAL.unmap(i / (ageInputs.length - 1d));
		// for (int i = 0; i < metallicityInputs.length; ++i)
		// metallicityInputs[i] = METALLICITY_INTERVAL.unmap(i /
		// (metallicityInputs.length - 1d));
		metallicityInputs = new double[] { 1e-4 };

		final var starProps = new StellarProperties();
		Vec3.set(elem.systemPosTm, Vec3.ZERO);
		for (int iMass = 0; iMass < massInputs.length; ++iMass) {
			for (int iMetallicity = 0; iMetallicity < metallicityInputs.length; ++iMetallicity) {
				for (int iAge = 0; iAge < ageInputs.length; ++iAge) {
					// final var info = new BasicSystemInfo();
					elem.systemAgeMyr = ageInputs[iAge];
					elem.massYg = massInputs[iMass];
					elem.metallicity = metallicityInputs[iMetallicity];

					starProps.load(elem.massYg, elem.systemAgeMyr, elem.metallicity);
					elem.luminosityLsol = starProps.luminosityLsol;
					elem.temperatureK = starProps.temperatureK;

					final var x = selectVariable(elem, this.xVariable);
					final var y = selectVariable(elem, this.yVariable);
					this.xyPlot.insert(x, y);
					this.xHistogram.insert(x);
					this.yHistogram.insert(y);
				}
			}
		}

	}

	private void createPlotsFromStellarGrid() {
		this.xyPlot = new ScatterPlot(
				this.xVariable.label, this.xVariable.units,
				this.yVariable.label, this.yVariable.units);
		this.xHistogram = new Histogram(this.xVariable.label, 1024, this.xMapping);
		this.yHistogram = new Histogram(this.yVariable.label, 1024, this.yMapping);
		this.levelHistogramsX = this.levelHistogramsY = new Histogram[0];

		final var elem = new GalaxySector.ElementHolder();
		try {

			var massInputs = new double[] { 1, 1.1, 1.2, 1.3, 1.4, 1.5 };
			var metallicityInputs = new double[] { 1e-4 };

			massInputs = new double[StellarProperties.GRID.initialMasses.length];
			for (int i = 0; i < massInputs.length - 2; ++i) {
				massInputs[i] = StellarProperties.GRID.initialMasses[i + 1];
			}

			// final AxisMapping MASS_INTERVAL = new AxisMapping.Log(Math.E, 0.11, 99.99);
			// massInputs = new double[50];
			// for (int i = 0; i < massInputs.length; ++i) {
			// final var t = i / (massInputs.length - 1d);
			// massInputs[i] = MASS_INTERVAL.unmap(t);
			// // massInputs[i] = Mth.lerp(t, 0.11, 100.0);
			// }

			outer: for (int iMass = 0; iMass < massInputs.length; ++iMass) {
				final double massInput = massInputs[iMass];
				for (int iMetallicity = 0; iMetallicity < metallicityInputs.length; ++iMetallicity) {
					final double metallicityInput = metallicityInputs[iMetallicity];

					final var corners = StellarProperties.GRID.findSurroundingTracks(metallicityInput, massInput);
					if (corners == null)
						continue;
					final var trackNN = StellarProperties.GRID.tracks[corners.metallicityIndex][corners.massIndex];
					final var trackNP = StellarProperties.GRID.tracks[corners.metallicityIndex][corners.massIndex + 1];
					final var trackPN = StellarProperties.GRID.tracks[corners.metallicityIndex + 1][corners.massIndex];
					final var trackPP = StellarProperties.GRID.tracks[corners.metallicityIndex + 1][corners.massIndex
							+ 1];
					final StellarProperties.Track[] tracks = {
							StellarProperties.GRID.tracks[corners.metallicityIndex][corners.massIndex],
							StellarProperties.GRID.tracks[corners.metallicityIndex][corners.massIndex + 1],
							StellarProperties.GRID.tracks[corners.metallicityIndex + 1][corners.massIndex],
							StellarProperties.GRID.tracks[corners.metallicityIndex + 1][corners.massIndex + 1],
					};

					if (trackNN.length == trackNP.length &&
							trackNN.length == trackPN.length &&
							trackNN.length == trackPP.length)
						continue;

					Mod.LOGGER.info("mismatch {} {} {} {}",
							trackNN.length,
							trackNP.length,
							trackPN.length,
							trackPP.length);
					Mod.LOGGER.info("ages {} {} {} {}",
							trackNN.age[trackNN.length - 1],
							trackNP.age[trackNP.length - 1],
							trackPN.age[trackPN.length - 1],
							trackPP.age[trackPP.length - 1]);

					Vec3.set(elem.systemPosTm, Vec3.ZERO);
					int minIndex = Integer.MAX_VALUE;
					for (final var track : tracks) {
						minIndex = Math.min(track.length, minIndex);
					}

					for (int i = 0; i < minIndex; ++i) {
						final var ageMN = Mth.lerp(corners.metallicityDistance, trackNN.age[i], trackPN.age[i]);
						final var ageMP = Mth.lerp(corners.metallicityDistance, trackNP.age[i], trackPP.age[i]);
						elem.systemAgeMyr = Mth.lerp(corners.massDistance, ageMN, ageMP) / 1e6;
						final var massMN = Mth.lerp(corners.metallicityDistance, trackNN.mass[i], trackPN.mass[i]);
						final var massMP = Mth.lerp(corners.metallicityDistance, trackNP.mass[i], trackPP.mass[i]);
						elem.massYg = Mth.lerp(corners.massDistance, massMN, massMP) * Units.Yg_PER_Msol;
						final var luminosityMN = Mth.lerp(corners.metallicityDistance, trackNN.luminosity[i],
								trackPN.luminosity[i]);
						final var luminosityMP = Mth.lerp(corners.metallicityDistance, trackNP.luminosity[i],
								trackPP.luminosity[i]);
						elem.luminosityLsol = Mth.lerp(corners.massDistance, luminosityMN, luminosityMP);
						final var temperatureMN = Mth.lerp(corners.metallicityDistance, trackNN.temperature[i],
								trackPN.temperature[i]);
						final var temperatureMP = Mth.lerp(corners.metallicityDistance, trackNP.temperature[i],
								trackPP.temperature[i]);
						elem.temperatureK = Mth.lerp(corners.massDistance, temperatureMN, temperatureMP);

						elem.metallicity = metallicityInput;

						// {
						// final var x = selectVariable(elem, this.xVariable);
						// final var y = selectVariable(elem, this.yVariable);
						// this.xyPlot.insert(x, y);
						// this.xHistogram.insert(x);
						// this.yHistogram.insert(y);
						// }

						// for (final var track : tracks) {
						// elem.systemAgeMyr = track.age[i] / 1e6;
						// elem.massYg = Units.Yg_PER_Msol * track.mass[i];
						// elem.metallicity = metallicityInput;
						// elem.luminosityLsol = track.luminosity[i];
						// elem.temperatureK = track.temperature[i];
						// final var x = selectVariable(elem, this.xVariable);
						// final var y = selectVariable(elem, this.yVariable);
						// this.xyPlot.insert(x, y);
						// this.xHistogram.insert(x);
						// this.yHistogram.insert(y);
						// }

					}

					// int maxIndexIndex = 0;
					// for (int i = 0; i < tracks.length; ++i) {
					// if (tracks[i].length > tracks[maxIndexIndex].length)
					// maxIndexIndex = i;
					// }

					// for (int i = 0; i < tracks[maxIndexIndex].length; ++i) {
					// final var track = tracks[maxIndexIndex];
					// elem.systemAgeMyr = track.age[i] / 1e6;
					// elem.massYg = Units.Yg_PER_Msol * track.mass[i];
					// elem.metallicity = metallicityInput;
					// elem.luminosityLsol = track.luminosity[i];
					// elem.temperatureK = track.temperature[i];
					// final var x = selectVariable(elem, this.xVariable);
					// final var y = selectVariable(elem, this.yVariable);
					// this.xyPlot.insert(x, y);
					// this.xHistogram.insert(x);
					// this.yHistogram.insert(y);
					// }

					for (final var track : tracks) {
						for (int i = 0; i < track.length; ++i) {
							elem.systemAgeMyr = track.age[i] / 1e6;
							elem.massYg = Units.Yg_PER_Msol * track.mass[i];
							elem.metallicity = metallicityInput;
							elem.luminosityLsol = track.luminosity[i];
							elem.temperatureK = track.temperature[i];
							final var x = selectVariable(elem, this.xVariable);
							final var y = selectVariable(elem, this.yVariable);
							this.xyPlot.insert(x, y);
							this.xHistogram.insert(x);
							this.yHistogram.insert(y);
						}
					}

					// break outer;
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	private void createScatterPlot() {
		createPlotsFromSurvey();
		// createPlotsFromInterpolation();
		// createPlotsFromStellarGrid();
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
		void accept(double pos, double value, ColorRgba color, boolean isMajor);
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
					consumer.accept(logMapping.remap(tMinor), tMinor, MINOR_MARKER_COLOR, false);
				}
				final var color = Math.abs(tl - 1.0) < 1e-24
						? AXIS_MARKER_COLOR
						: MAJOR_MARKER_COLOR;
				consumer.accept(logMapping.remap(tl), tl, color, true);
			}
		} else if (mapping instanceof AxisMapping.Linear linearMapping) {
			final var inc = Math.pow(10,
					Mth.floor(Math.log(linearMapping.domain.max - linearMapping.domain.min) / Math.log(10)));
			final var lmin = inc * Math.floor(linearMapping.domain.min / inc);
			final var lmax = inc * Math.ceil(linearMapping.domain.max / inc);
			for (double t = lmin; t < lmax; t += inc) {
				for (int j = 1; j < 8; ++j) {
					final var tMinor = Mth.lerp(j / 8.0, t, t + inc);
					consumer.accept(linearMapping.remap(tMinor), tMinor, MINOR_MARKER_COLOR, false);
				}
				final var color = Math.abs(t) < 1e-24
						? AXIS_MARKER_COLOR
						: MAJOR_MARKER_COLOR;
				consumer.accept(linearMapping.remap(t), t, color, true);
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
			TransformStack tfm, double t, Rect bounds, ColorRgba color) {
		final double x1, y1, x2, y2;
		x1 = x2 = Mth.lerp(t, bounds.min().x, bounds.max().x);
		y1 = bounds.min().y;
		y2 = bounds.max().y;

		RenderHelper.addLine(builder, tfm, new Vec3(x1, y1, 0), new Vec3(x2, y2, 0), color);
	}

	private void renderHistogram(RenderContext ctx, TransformStack tfm, AxisMapping mapping, Histogram plot,
			Rect bounds, boolean flip) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				IndexPattern.QUADS, BufferLayout.POSITION_COLOR_TEX);

		int max = 0;
		final var maxIndices = new VectorInt();
		if (plot.total() > 0)
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
			lx = Mth.lerp(l, bounds.min().x, bounds.max().x);
			hx = Mth.lerp(h, bounds.min().x, bounds.max().x);
			if (!flip) {
				ly = bounds.min().y;
				hy = Mth.lerp(percent, bounds.min().y, bounds.max().y);
			} else {
				ly = Mth.lerp(percent, bounds.max().y, bounds.min().y);
				hy = bounds.max().y;
			}

			builder.vertex(tfm, hx, ly, 0).color(HISTOGRAM_BIN_COLOR).uv0(1, 0).endVertex();
			builder.vertex(tfm, lx, ly, 0).color(HISTOGRAM_BIN_COLOR).uv0(0, 0).endVertex();
			builder.vertex(tfm, lx, hy, 0).color(HISTOGRAM_BIN_COLOR).uv0(0, 1).endVertex();
			builder.vertex(tfm, hx, hy, 0).color(HISTOGRAM_BIN_COLOR).uv0(1, 1).endVertex();
		}

		builder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				IndexPattern.VANILLA_LINES,
				BufferLayout.POSITION_COLOR_NORMAL);

		if (plot.hasMean()) {
			for (int i = 0; i < maxIndices.size(); ++i) {
				final var t = maxIndices.get(i) / (plot.size() - 1.0);
				renderHistogramMarker(lineBuilder, tfm, t, bounds, HISTOGRAM_MAX_COLOR);
			}
			renderHistogramMarker(lineBuilder, tfm, mapping.remap(plot.getMean()), bounds, HISTOGRAM_MEAN_COLOR);
		}

		final double lx = bounds.min().x, ly = bounds.min().y;
		final double hx = bounds.max().x, hy = bounds.max().y;
		RenderHelper.addLine(builder, tfm, new Vec3(lx, ly, 0), new Vec3(hx, ly, 0), MINOR_MARKER_COLOR);
		RenderHelper.addLine(builder, tfm, new Vec3(lx, hy, 0), new Vec3(hx, hy, 0), MINOR_MARKER_COLOR);
		RenderHelper.addLine(builder, tfm, new Vec3(lx, ly, 0), new Vec3(lx, hy, 0), MINOR_MARKER_COLOR);
		RenderHelper.addLine(builder, tfm, new Vec3(hx, ly, 0), new Vec3(hx, hy, 0), MINOR_MARKER_COLOR);

		RenderSystem.lineWidth(1f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		final var sink = new TextBuilder();
		// final double x, y;
		// y = bounds.min().x;
		// x = bounds.min().y;

		tfm.push();
		tfm.prependTranslation(new Vec3(bounds.min().x, bounds.max().y, 0));
		tfm.prependScale(0.2f);
		sink.cursorY -= this.client.font.lineHeight * 0.005;

		if (plot.hasMean()) {
			sink.emit(tfm.current(), FormattedText.of(String.format("mean=%g", plot.getMean())));
			sink.cursorAppendUnder();
		}
		sink.emit(tfm.current(), FormattedText.of(String.format("n=%d", plot.total())));
		sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);
		tfm.pop();
	}

	private void renderScatterPlot(RenderContext ctx, TransformStack tfm, ScatterPlot plot, Rect bounds,
			boolean flipX, boolean flipY) {
		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				IndexPattern.VANILLA_LINES,
				BufferLayout.POSITION_COLOR_NORMAL);
		final var sink = new TextBuilder();

		// guides
		renderGuides(this.xMapping, (pos, value, color, isMajor) -> {
			if (pos <= 0 || pos >= 1)
				return;
			final var x = Mth.lerp(pos, bounds.min().x, bounds.max().x);
			final var l = new Vec3(x, bounds.min().y, 0);
			final var h = new Vec3(x, bounds.max().y, 0);
			RenderHelper.addLine(lineBuilder, tfm, l, h, color);
			if (isMajor) {
				tfm.push();
				sink.reset();
				sink.scale = 0.33;
				sink.textOrigin = TextBuilder.TextOrigin.BOTTOM;
				sink.cursorX = x;
				sink.cursorY = bounds.min().y;
				sink.emit(tfm.current(), String.format("%.3g %s", value, this.xVariable.units));
				tfm.pop();
			}
		});
		renderGuides(this.yMapping, (pos, value, color, isMajor) -> {
			if (pos <= 0 || pos >= 1)
				return;
			final var y = Mth.lerp(pos, bounds.min().y, bounds.max().y);
			final var l = new Vec3(bounds.min().x, y, 0);
			final var h = new Vec3(bounds.max().x, y, 0);
			RenderHelper.addLine(lineBuilder, tfm, l, h, color);
			if (isMajor) {
				tfm.push();
				sink.reset();
				sink.scale = 0.33;
				sink.textOrigin = TextBuilder.TextOrigin.LEFT;
				sink.cursorX = bounds.min().x;
				sink.cursorY = y;
				sink.emit(tfm.current(), String.format("%.3g %s", value, this.yVariable.units));
				tfm.pop();
			}
		});

		// bounds
		RenderHelper.addLine(lineBuilder, tfm, new Vec3(bounds.min().x, bounds.min().y, 0),
				new Vec3(bounds.min().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, tfm, new Vec3(bounds.max().x, bounds.min().y, 0),
				new Vec3(bounds.max().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, tfm, new Vec3(bounds.min().x, bounds.min().y, 0),
				new Vec3(bounds.max().x, bounds.min().y, 0), MAJOR_MARKER_COLOR);
		RenderHelper.addLine(lineBuilder, tfm, new Vec3(bounds.min().x, bounds.max().y, 0),
				new Vec3(bounds.max().x, bounds.max().y, 0), MAJOR_MARKER_COLOR);

		RenderSystem.lineWidth(1f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		// data
		tfm.push();
		final var pointShader = UltravioletShaders.SHADER_UI_POINTS.get();
		pointShader.setupDefaultShaderUniforms();
		tfm.prependTranslation(bounds.min().xy0());
		tfm.prependScale(bounds.size().xy1());
		pointShader.setUniformf("uModelMatrix", tfm.current());
		this.pointsMesh.draw(pointShader, HawkDrawStates.DRAW_STATE_DIRECT_ADDITIVE_BLENDING);
		tfm.pop();

		// labels
		sink.reset();
		sink.textOrigin = TextBuilder.TextOrigin.TOP;
		sink.cursorX = bounds.min().x + bounds.size().x / 2;
		sink.cursorY = bounds.min().y;
		sink.emit(tfm.current(), this.xVariable.label);

		// tfm.appendScale(0.005f);
		// tfm.appendTranslation(new Vec3(x, y, 0));

		// sink.emit(tfm.current(), FormattedText.of(String.format("n=%d",
		// plot.total())));
		sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);

	}

	@Override
	public void renderScreenPostLayers(RenderContext ctx) {
		if (this.xyPlot == null)
			createScatterPlot();
		if (this.pointsMesh == null)
			createMesh(Rect.UNIPOLAR);
		setupCamera(ctx);

		final var snapshot = RenderMatricesSnapshot.capture();
		this.camera.applyProjection();
		this.camera.applyView();

		final var tfm = new TransformStack();

		tfm.push();
		final var animTime = (System.currentTimeMillis() % 1000) / 1000.0;
		// tfm.appendRotation(Quat.axisAngle(Vec3.ZP, 0.05 * Math.sin(2 * Math.PI *
		// animTime)));
		renderScatterPlot(ctx, tfm, this.xyPlot, Rect.BIPOLAR, false, false);
		tfm.push();
		tfm.appendTranslation(new Vec3(0, -1.3, 0));
		renderHistogram(ctx, tfm, this.xMapping, this.xHistogram, new Rect(-1, 0, 1, 0.25), true);
		tfm.pop();
		tfm.push();
		tfm.appendTranslation(new Vec3(0, -1.3, 0));
		tfm.appendRotation(Quat.axisAngle(Vec3.ZP, Math.PI / 2));
		renderHistogram(ctx, tfm, this.yMapping, this.yHistogram, new Rect(-1, 0, 1, 0.25), true);
		tfm.pop();
		tfm.pop();

		final var sink = new TextBuilder();
		final var textTfm = new TransformStack();
		// textTfm.appendScale(0.005f);
		// textTfm.appendTranslation(new Vec3(1.02, 0.98, 0));
		for (int i = 0; i < this.levelHistogramsX.length; ++i) {
			final var hist = this.levelHistogramsX[i];
			tfm.push();
			tfm.appendTranslation(new Vec3(2, -0.1 * i, 0));
			sink.cursorX = 0;
			sink.cursorY = 0;
			sink.scale = 0.5;
			sink.emit(tfm.current(),
					FormattedText.of(String.format("X level(%d)", i)));
			// tfm.appendRotation(Quat.axisAngle(Vec3.ZP, Math.PI / 2));
			renderHistogram(ctx, tfm, this.xMapping, hist, new Rect(0, 0, 0.5, 0.1), true);
			tfm.pop();
		}
		for (int i = 0; i < this.levelHistogramsY.length; ++i) {
			final var hist = this.levelHistogramsY[i];
			tfm.push();
			tfm.appendTranslation(new Vec3(2.5, -0.1 * i, 0));
			sink.cursorX = 0;
			sink.cursorY = 0;
			sink.scale = 0.5;
			sink.emit(tfm.current(),
					FormattedText.of(String.format("Y level(%d)", i)));
			// tfm.appendRotation(Quat.axisAngle(Vec3.ZP, Math.PI / 2));
			renderHistogram(ctx, tfm, this.yMapping, hist, new Rect(0, 0, 0.5, 0.1), true);
			tfm.pop();
		}
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
