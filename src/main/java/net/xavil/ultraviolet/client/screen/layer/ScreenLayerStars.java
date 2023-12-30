package net.xavil.ultraviolet.client.screen.layer;

import java.util.Random;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.StarRenderManager;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.NewSystemMapScreen;
import net.xavil.ultraviolet.client.screen.SystemMapScreen;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.BaseGalaxyGenerationLayer;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorPos;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerStars extends HawkScreen3d.Layer3d {
	private final HawkScreen3d screen;
	public final Galaxy galaxy;
	private final StarRenderManager starRenderer;
	private final Vec3 originOffset;

	public ScreenLayerStars(HawkScreen3d attachedScreen, Galaxy galaxy, Vec3 originOffset) {
		super(attachedScreen, new CameraConfig(1e2, false, 1e9, false));
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.originOffset = originOffset;
		// this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy,
		// 		new SectorTicketInfo.Multi(Vec3.ZERO, GalaxySector.BASE_SIZE_Tm, SectorTicketInfo.Multi.SCALES_EXP)));
		// this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy,
		// 		new SectorTicketInfo.Multi(Vec3.ZERO, GalaxySector.BASE_SIZE_Tm, SectorTicketInfo.Multi.SCALES_EXP_ADJUSTED)));
		this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy,
				new SectorTicketInfo.Multi(Vec3.ZERO, GalaxySector.BASE_SIZE_Tm, new double[] { 1, 4, 8, 16, 32, 64, 128, 256 })));
		this.starRenderer.setOriginOffset(this.originOffset);
	}

	private Vec3 getStarViewCenterPos(OrbitCamera.Cached camera) {
		if (ClientConfig.get(ConfigKey.SECTOR_TICKET_AROUND_FOCUS))
			return camera.focus;
		return camera.pos.sub(this.originOffset).mul(camera.metersPerUnit / 1e12);
	}

	@Override
	public boolean handleClick(Vec2 mousePos, int button) {
		if (super.handleClick(mousePos, button))
			return true;

		if (button == 0) {
			final var ray = this.lastCamera.rayForPicking(this.client.getWindow(), mousePos);
			final var selected = pickElement(this.lastCamera, ray);
			insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM, selected.unwrapOrNull());
			return true;
		}

		return false;
	}

	@Override
	public boolean handleKeypress(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_K) {
			this.starRenderer.setMode(StarRenderManager.Mode.REALISTIC);
		}
		if (keypress.keyCode == GLFW.GLFW_KEY_L) {
			this.starRenderer.setMode(StarRenderManager.Mode.MAP);
		}
		if (keypress.keyCode == GLFW.GLFW_KEY_R) {
			final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
			if (selected != null) {
				final var disposer = new Disposable.Multi();
				final var ticket = this.galaxy.sectorManager.createSystemTicket(disposer, selected);
				this.galaxy.sectorManager.forceLoad(ticket);
				this.galaxy.sectorManager.getSystem(selected).ifSome(system -> {
					final var id = new SystemId(this.galaxy.galaxyId, selected);
					if (keypress.hasModifiers(GLFW.GLFW_MOD_SHIFT)) {
						final var screen = new NewSystemMapScreen(this.screen, this.galaxy, id, system);
						this.client.setScreen(screen);
					} else {
						final var screen = new SystemMapScreen(this.screen, this.galaxy, id, system);
						screen.camera.pitch.set(this.screen.camera.pitch.target);
						screen.camera.yaw.set(this.screen.camera.yaw.target);
						this.client.setScreen(screen);
					}
				});
				disposer.close();
				return true;
			}
		} else if (keypress.keyCode == GLFW.GLFW_KEY_H) {
			try (final var disposer = Disposable.scope()) {
				final var center = this.starRenderer.getSectorTicket().info.centerPos;
				final var ticket = this.galaxy.sectorManager.createSectorTicket(disposer, new SectorTicketInfo.Multi(
						center, 3.0 * GalaxySector.BASE_SIZE_Tm, SectorTicketInfo.Multi.SCALES_UNIFORM));
				// final var ticket = this.starRenderer.getSectorTicket();
				ticket.attachedManager.forceLoad(ticket);

				final var survey = new StarSurvey();
				survey.init(ticket);
				StarSurvey.printSurvey(survey, msg -> Mod.LOGGER.info("{}", msg));
			}

			return true;
		}

		return false;
	}

	public static final class StarSurvey {
		// these are just based on mass, not on actual color
		public int starCountL = 0;
		public int starCountM = 0;
		public int starCountK = 0;
		public int starCountG = 0;
		public int starCountF = 0;
		public int starCountA = 0;
		public int starCountB = 0;
		public int starCountO = 0;
		public double totalMassL = 0, totalTemperatureL = 0, totalLuminosityL = 0, totalAgeL = 0;
		public double totalMassM = 0, totalTemperatureM = 0, totalLuminosityM = 0, totalAgeM = 0;
		public double totalMassK = 0, totalTemperatureK = 0, totalLuminosityK = 0, totalAgeK = 0;
		public double totalMassG = 0, totalTemperatureG = 0, totalLuminosityG = 0, totalAgeG = 0;
		public double totalMassF = 0, totalTemperatureF = 0, totalLuminosityF = 0, totalAgeF = 0;
		public double totalMassA = 0, totalTemperatureA = 0, totalLuminosityA = 0, totalAgeA = 0;
		public double totalMassB = 0, totalTemperatureB = 0, totalLuminosityB = 0, totalAgeB = 0;
		public double totalMassO = 0, totalTemperatureO = 0, totalLuminosityO = 0, totalAgeO = 0;
		public int starsPerLevel[] = new int[GalaxySector.ROOT_LEVEL + 1];
		public int sectorsPerLevel[] = new int[GalaxySector.ROOT_LEVEL + 1];

		public final Histogram idealMassDistribution = new Histogram("Mass (M☉)", new Interval(0, 16), 32);
		public final Histogram massDistribution = new Histogram("Mass (M☉)", new Interval(0, 16), 32);
		public final Histogram temperatureDistribution = new Histogram("Temperature (K)", new Interval(0, 50000), 32);
		public final Histogram luminosityDistribution = new Histogram("Luminosity (L☉)", new Interval(0, 21), 32);
		public final Histogram ageDistribution = new Histogram("Age (Mya)", new Interval(0, 10000), 32);

		public StarSurvey() {
			final var rng = Rng.wrap(new Random());
			for (int i = 0; i < 1000000; ++i) {
				// final var level = rng.uniformInt(0, GalaxySector.ROOT_LEVEL + 1);
				// final var mass = BaseGalaxyGenerationLayer.generateStarMassForLevel(rng,
				// level);
				final var mass = BaseGalaxyGenerationLayer.generateStarMass(rng);
				this.idealMassDistribution.insert(mass / Units.Yg_PER_Msol);
			}
		}

		public void init(SectorTicket<SectorTicketInfo.Multi> ticket) {
			final var elem = new GalaxySector.ElementHolder();
			ticket.attachedManager.enumerate(ticket, sector -> {
				// if (sector.level != 3)
				// 	return;
				final var radius = ticket.info.radiusForLevel(sector.level);
				this.starsPerLevel[sector.level] += sector.elements.size();
				this.sectorsPerLevel[sector.level] += 1;
				for (int i = 0; i < sector.elements.size(); ++i) {
					sector.elements.load(elem, i);
					if (elem.systemPosTm.distanceTo(ticket.info.centerPos) <= radius)
						insert(sector, elem);
				}
			});
		}

		public void reset() {
			for (int i = 0; i < this.starsPerLevel.length; ++i)
				this.starsPerLevel[i] = 0;
			for (int i = 0; i < this.sectorsPerLevel.length; ++i)
				this.sectorsPerLevel[i] = 0;
			this.starCountL = this.starCountM = this.starCountK = this.starCountG = this.starCountF = this.starCountA = this.starCountB = this.starCountO = 0;
			this.totalMassL = this.totalTemperatureL = this.totalLuminosityL = this.totalAgeL = 0;
			this.totalMassM = this.totalTemperatureM = this.totalLuminosityM = this.totalAgeM = 0;
			this.totalMassK = this.totalTemperatureK = this.totalLuminosityK = this.totalAgeK = 0;
			this.totalMassG = this.totalTemperatureG = this.totalLuminosityG = this.totalAgeG = 0;
			this.totalMassF = this.totalTemperatureF = this.totalLuminosityF = this.totalAgeF = 0;
			this.totalMassA = this.totalTemperatureA = this.totalLuminosityA = this.totalAgeA = 0;
			this.totalMassB = this.totalTemperatureB = this.totalLuminosityB = this.totalAgeB = 0;
			this.totalMassO = this.totalTemperatureO = this.totalLuminosityO = this.totalAgeO = 0;
			this.idealMassDistribution.reset();
			this.massDistribution.reset();
			this.temperatureDistribution.reset();
			this.luminosityDistribution.reset();
			this.ageDistribution.reset();
		}

		public void insert(GalaxySector sector, GalaxySector.ElementHolder elem) {
			final var massMsol = Units.Msol_PER_Yg * elem.massYg;
			if (massMsol >= 16) {
				this.starCountO += 1;
				this.totalMassO += massMsol;
				this.totalTemperatureO += elem.temperatureK;
				this.totalLuminosityO += elem.luminosityLsol;
				this.totalAgeO += elem.systemAgeMyr;
			} else if (massMsol >= 2.1) {
				this.starCountB += 1;
				this.totalMassB += massMsol;
				this.totalTemperatureB += elem.temperatureK;
				this.totalLuminosityB += elem.luminosityLsol;
				this.totalAgeB += elem.systemAgeMyr;
			} else if (massMsol >= 1.4) {
				this.starCountA += 1;
				this.totalMassA += massMsol;
				this.totalTemperatureA += elem.temperatureK;
				this.totalLuminosityA += elem.luminosityLsol;
				this.totalAgeA += elem.systemAgeMyr;
			} else if (massMsol >= 1.04) {
				this.starCountF += 1;
				this.totalMassF += massMsol;
				this.totalTemperatureF += elem.temperatureK;
				this.totalLuminosityF += elem.luminosityLsol;
				this.totalAgeF += elem.systemAgeMyr;
			} else if (massMsol >= 0.8) {
				this.starCountG += 1;
				this.totalMassG += massMsol;
				this.totalTemperatureG += elem.temperatureK;
				this.totalLuminosityG += elem.luminosityLsol;
				this.totalAgeG += elem.systemAgeMyr;
			} else if (massMsol >= 0.45) {
				this.starCountK += 1;
				this.totalMassK += massMsol;
				this.totalTemperatureK += elem.temperatureK;
				this.totalLuminosityK += elem.luminosityLsol;
				this.totalAgeK += elem.systemAgeMyr;
			} else if (massMsol >= 0.08) {
				this.starCountM += 1;
				this.totalMassM += massMsol;
				this.totalTemperatureM += elem.temperatureK;
				this.totalLuminosityM += elem.luminosityLsol;
				this.totalAgeM += elem.systemAgeMyr;
			} else {
				this.starCountL += 1;
				this.totalMassL += massMsol;
				this.totalTemperatureL += elem.temperatureK;
				this.totalLuminosityL += elem.luminosityLsol;
				this.totalAgeL += elem.systemAgeMyr;
			}
			this.massDistribution.insert(massMsol);
			this.temperatureDistribution.insert(elem.temperatureK);
			this.luminosityDistribution.insert(elem.luminosityLsol);
			this.ageDistribution.insert(elem.systemAgeMyr);
		}

		public static void printSurvey(StarSurvey survey, Consumer<String> printer) {
			final var starCountTotal = survey.starCountO + survey.starCountB + survey.starCountA + survey.starCountF
					+ survey.starCountG + survey.starCountK + survey.starCountM + survey.starCountL;
			printer.accept(String.format("Total Star Count: %d", starCountTotal));
			for (int i = 0; i <= GalaxySector.ROOT_LEVEL; ++i) {
				printer.accept(String.format("%d stars across %d sectors for level %d (%f stars/sector)",
						survey.starsPerLevel[i],
						survey.sectorsPerLevel[i], i, survey.starsPerLevel[i] / (double) survey.sectorsPerLevel[i]));
			}
			printer.accept("Star Class Averages:");
			if (survey.starCountO > 0)
				printer.accept(String.format("O: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountO,
						100.0 * survey.starCountO / (double) starCountTotal,
						survey.totalMassO / survey.starCountO,
						survey.totalTemperatureO / survey.starCountO,
						survey.totalLuminosityO / survey.starCountO,
						survey.totalAgeO / survey.starCountO));
			if (survey.starCountB > 0)
				printer.accept(String.format("B: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountB,
						100.0 * survey.starCountB / (double) starCountTotal,
						survey.totalMassB / survey.starCountB,
						survey.totalTemperatureB / survey.starCountB,
						survey.totalLuminosityB / survey.starCountB,
						survey.totalAgeB / survey.starCountB));
			if (survey.starCountA > 0)
				printer.accept(String.format("A: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountA,
						100.0 * survey.starCountA / (double) starCountTotal,
						survey.totalMassA / survey.starCountA,
						survey.totalTemperatureA / survey.starCountA,
						survey.totalLuminosityA / survey.starCountA,
						survey.totalAgeA / survey.starCountA));
			if (survey.starCountF > 0)
				printer.accept(String.format("F: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountF,
						100.0 * survey.starCountF / (double) starCountTotal,
						survey.totalMassF / survey.starCountF,
						survey.totalTemperatureF / survey.starCountF,
						survey.totalLuminosityF / survey.starCountF,
						survey.totalAgeF / survey.starCountF));
			if (survey.starCountG > 0)
				printer.accept(String.format("G: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountG,
						100.0 * survey.starCountG / (double) starCountTotal,
						survey.totalMassG / survey.starCountG,
						survey.totalTemperatureG / survey.starCountG,
						survey.totalLuminosityG / survey.starCountG,
						survey.totalAgeG / survey.starCountG));
			if (survey.starCountK > 0)
				printer.accept(String.format("K: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountK,
						100.0 * survey.starCountK / (double) starCountTotal,
						survey.totalMassK / survey.starCountK,
						survey.totalTemperatureK / survey.starCountK,
						survey.totalLuminosityK / survey.starCountK,
						survey.totalAgeK / survey.starCountK));
			if (survey.starCountM > 0)
				printer.accept(String.format("M: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountM,
						100.0 * survey.starCountM / (double) starCountTotal,
						survey.totalMassM / survey.starCountM,
						survey.totalTemperatureM / survey.starCountM,
						survey.totalLuminosityM / survey.starCountM,
						survey.totalAgeM / survey.starCountM));
			if (survey.starCountL > 0)
				printer.accept(String.format("L: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountL,
						100.0 * survey.starCountL / (double) starCountTotal,
						survey.totalMassL / survey.starCountL,
						survey.totalTemperatureL / survey.starCountL,
						survey.totalLuminosityL / survey.starCountL,
						survey.totalAgeL / survey.starCountL));

			printer.accept("Ideal Mass Distribution");
			survey.idealMassDistribution.display(printer, 100);
			printer.accept("Mass Distribution");
			survey.massDistribution.display(printer, 100);
			printer.accept("Temperature Distribution");
			survey.temperatureDistribution.display(printer, 100);
			printer.accept("Luminosity Distribution");
			survey.luminosityDistribution.display(printer, 100);
			printer.accept("Age Distribution");
			survey.ageDistribution.display(printer, 100);
		}
	}

	public static final class Histogram {
		private final Interval domain;
		private final String inputLabel;
		private final int[] bins;

		private final Vector<Double> outliersLo = new Vector<>(), outliersHi = new Vector<>();
		private int outliersLoCount, outliersHiCount;

		private int total = 0;

		public Histogram(String inputLabel, Interval domain, int binCount) {
			this.inputLabel = inputLabel;
			this.bins = new int[binCount];
			this.domain = domain;
		}

		public void reset() {
			this.total = 0;
			for (int i = 0; i < this.bins.length; ++i)
				this.bins[i] = 0;
			this.outliersLo.clear();
			this.outliersHi.clear();
			this.outliersLoCount = this.outliersHiCount = 0;
		}

		public void insert(double value) {
			this.total += 1;
			final var t = Mth.inverseLerp(value, this.domain.lower, this.domain.higher);
			if (t < 0) {
				if (this.outliersLoCount < 20)
					this.outliersLo.push(value);
				this.outliersLoCount += 1;
			} else if (t >= 1) {
				if (this.outliersHiCount < 20)
					this.outliersHi.push(value);
				this.outliersHiCount += 1;
			} else {
				final var bin = Mth.floor(t * this.bins.length);
				this.bins[bin] += 1;
			}
		}

		private static final char[] BAR_CHARS = { ' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉', '█' };

		private void makeBar(StringBuilder sb, double barPercent, int barLength) {
			double w = barPercent * barLength, ws = 1.0;
			for (int j = 0; j < barLength; ++j) {
				double p = Mth.clamp(w, 0, 1);
				w -= ws;
				sb.append(BAR_CHARS[Mth.floor(p * (BAR_CHARS.length - 1))]);
			}
		}

		private void makeBar(StringBuilder sb, String label, char ch, int barLength) {
			if (label != null && !label.isEmpty()) {
				for (int j = 0; j < barLength; ++j) {
					if (j < 5)
						sb.append(ch);
					else if (j < 6)
						sb.append(' ');
					else if (j < 6 + label.length())
						sb.append(label.charAt(j - 6));
					else if (j < 7 + label.length())
						sb.append(' ');
					else
						sb.append(ch);
				}
			} else {
				for (int j = 0; j < barLength; ++j) {
					sb.append(ch);
				}
			}
		}

		public void display(Consumer<String> printer, int barLength) {
			final var sb = new StringBuilder();
			sb.setLength(0);
			sb.append("+");
			makeBar(sb, this.inputLabel, '=', barLength);
			sb.append("+");
			printer.accept(sb.toString());
			for (int i = 0; i < this.bins.length; ++i) {
				final var binPercent = this.bins[i] / (double) this.total;
				final var binLo = Mth.lerp(i / (double) this.bins.length, this.domain.lower, this.domain.higher);
				final var binHi = Mth.lerp((i + 1) / (double) this.bins.length, this.domain.lower, this.domain.higher);
				sb.setLength(0);
				makeBar(sb, binPercent, barLength);
				printer.accept(
						String.format("|%s| %d : %f%% : %f-%f", sb, this.bins[i], 100 * binPercent, binLo, binHi));
			}
			sb.setLength(0);
			sb.append("+");
			makeBar(sb, this.inputLabel, '-', barLength);
			sb.append("+");
			printer.accept(sb.toString());
			printer.accept(String.format("Outliers (Lo): %d, %s", this.outliersLoCount, this.outliersLo));
			printer.accept(String.format("Outliers (Hi): %d, %s", this.outliersHiCount, this.outliersHi));
			printer.accept(String.format("Total: %d", this.total));
			sb.setLength(0);
			makeBar(sb, null, '=', barLength + 2);
			printer.accept(sb.toString());
		}
	}

	private Maybe<GalaxySectorId> pickElement(OrbitCamera.Cached camera, Ray ray) {
		// @formatter:off
		final var closest = new Object() {
			double    distance    = Double.POSITIVE_INFINITY;
			int       sectorIndex = -1;
			SectorPos sectorPos   = null;
		};
		// @formatter:on
		final var viewCenter = getStarViewCenterPos(camera);
		final var ticket = this.starRenderer.getSectorTicket();
		this.galaxy.sectorManager.enumerate(ticket, sector -> {
			final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			if (!ray.intersectAABB(min, max))
				return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());

			final var elem = new GalaxySector.ElementHolder();
			final var pos = elem.systemPosTm;
			final var proj = new Vec3.Mutable(0, 0, 0);
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);

				if (pos.distanceTo(viewCenter) > levelSize)
					continue;

				final var distance = ray.origin().distanceTo(pos);
				if (!ray.intersectsSphere(pos, 0.02 * distance))
					continue;

				Vec3.sub(pos, pos, ray.origin());
				Vec3.projectOnto(proj, pos, ray.dir());
				final var projDist = pos.distanceTo(proj);
				if (projDist < closest.distance) {
					closest.distance = projDist;
					closest.sectorPos = sector.pos();
					closest.sectorIndex = i;
				}
			}

		});

		if (closest.sectorIndex == -1)
			return Maybe.none();
		return Maybe.some(GalaxySectorId.from(closest.sectorPos, closest.sectorIndex));
	}

	@Override
	public void render3d(Cached camera, RenderContext ctx) {
		final var cullingCamera = getCullingCamera();
		final var viewCenter = getStarViewCenterPos(cullingCamera);
		final var ticket = this.starRenderer.getSectorTicket();

		this.starRenderer.draw(camera, viewCenter);

		// TODO: render things that are currently jumping between systems

		if (ClientConfig.get(ConfigKey.SHOW_SECTOR_BOUNDARIES)) {
			final var builder = BufferRenderer.IMMEDIATE_BUILDER
					.beginGeneric(PrimitiveType.LINES, BufferLayout.POSITION_COLOR_NORMAL);
			ticket.attachedManager.enumerate(ticket, sector -> {
				if (!this.galaxy.sectorManager.isComplete(sector.pos()))
					;
				// if (!this.galaxy.sectorManager.isComplete(sector.pos()))
				// return;
				// if (sector.elements.size() == 0)
				// return;

				final Vec3 s = sector.pos().minBound(), e = sector.pos().maxBound();
				final var color = ClientConfig.getDebugColor(sector.pos().level()).withA(0.2f);

				final var nnn = new Vec3(s.x, s.y, s.z);
				final var nnp = new Vec3(s.x, s.y, e.z);
				final var npn = new Vec3(s.x, e.y, s.z);
				final var npp = new Vec3(s.x, e.y, e.z);
				final var pnn = new Vec3(e.x, s.y, s.z);
				final var pnp = new Vec3(e.x, s.y, e.z);
				final var ppn = new Vec3(e.x, e.y, s.z);
				final var ppp = new Vec3(e.x, e.y, e.z);

				// X
				RenderHelper.addLine(builder, camera, nnn, pnn, color);
				RenderHelper.addLine(builder, camera, nnp, pnp, color);
				RenderHelper.addLine(builder, camera, npn, ppn, color);
				RenderHelper.addLine(builder, camera, npp, ppp, color);
				// Y
				RenderHelper.addLine(builder, camera, nnn, npn, color);
				RenderHelper.addLine(builder, camera, nnp, npp, color);
				RenderHelper.addLine(builder, camera, pnn, ppn, color);
				RenderHelper.addLine(builder, camera, pnp, ppp, color);
				// Z
				RenderHelper.addLine(builder, camera, npn, npp, color);
				RenderHelper.addLine(builder, camera, nnn, nnp, color);
				RenderHelper.addLine(builder, camera, ppn, ppp, color);
				RenderHelper.addLine(builder, camera, pnn, pnp, color);
			});
			// ticket.info.enumerateAffectedSectors(pos -> {
			// });
			builder.end().draw(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_ADDITIVE_BLENDING);
		}
	}

}
