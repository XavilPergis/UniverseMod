package net.xavil.ultraviolet.client.screen.layer;

import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.StarRenderManager;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.NewSystemMapScreen;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.BaseGalaxyGenerationLayer;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorPos;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerStars extends HawkScreen3d.Layer3d {
	private final HawkScreen3d screen;
	public final Galaxy galaxy;
	private final StarRenderManager starRenderer;
	private final Vec3 originOffset;

	public ScreenLayerStars(HawkScreen3d attachedScreen, Galaxy galaxy, SectorTicketInfo.Multi ticketInfo,
			Vec3 originOffset) {
		super(attachedScreen, new CameraConfig(1e2, false, 1e8, false));
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.originOffset = originOffset;
		this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy, this.originOffset));
	}

	private Vec3 getStarViewCenterPos(OrbitCamera.Cached camera) {
		if (ClientConfig.get(ConfigKey.SECTOR_TICKET_AROUND_FOCUS))
			return camera.focus;
		return camera.pos.sub(this.originOffset).mul(camera.metersPerUnit / 1e12);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.starRenderer != null)
			this.starRenderer.tick();
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
	public boolean handleKeypress(int keyCode, int scanCode, int modifiers) {
		if (super.handleKeypress(keyCode, scanCode, modifiers))
			return true;

		if (keyCode == GLFW.GLFW_KEY_R) {
			final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
			if (selected != null) {
				final var disposer = new Disposable.Multi();
				final var ticket = this.galaxy.sectorManager.createSystemTicket(disposer, selected);
				this.galaxy.sectorManager.forceLoad(ticket);
				this.galaxy.sectorManager.getSystem(selected).ifSome(system -> {
					final var id = new SystemId(this.galaxy.galaxyId, selected);
					final var screen = new NewSystemMapScreen(this.screen, this.galaxy, id, system);
					screen.camera.pitch.set(this.screen.camera.pitch.target);
					screen.camera.yaw.set(this.screen.camera.yaw.target);
					this.client.setScreen(screen);
				});
				disposer.close();
				return true;
			}
		} else if (keyCode == GLFW.GLFW_KEY_H) {
			final var massHistogram = new Histogram(new Interval(0, 16), 32);
			final var temperatureHistogram = new Histogram(new Interval(0, 50000), 32);
			final var luminosityHistogram = new Histogram(new Interval(0, 21), 32);
			final var aaaaaaaaa = new Histogram(new Interval(0, 16), 32);
			final var rng = Rng.wrap(new Random());
			for (int i = 0; i < 1000000; ++i) {
				aaaaaaaaa.insert(BaseGalaxyGenerationLayer.generateStarMass(rng, 0) / Units.Yg_PER_Msol);
			}
			final var ticket = this.starRenderer.getSectorTicket();
			this.galaxy.sectorManager.forceLoad(ticket);
			ticket.attachedManager.enumerate(ticket, sector -> {
				sector.initialElements.iter().enumerate().forEach(elem -> {
					final var star = elem.item.info().primaryStar;
					if (star.type == StellarCelestialNode.Type.MAIN_SEQUENCE || star.type == StellarCelestialNode.Type.GIANT) {
						massHistogram.insert(star.massYg / Units.Yg_PER_Msol);
						temperatureHistogram.insert(star.temperatureK);
						luminosityHistogram.insert(star.luminosityLsol);
					}
				});
			});
			aaaaaaaaa.display();
			massHistogram.display();
			temperatureHistogram.display();
			luminosityHistogram.display();
			return true;
		}

		return false;
	}

	class Histogram {
		private final Interval domain;
		private int total = 0;
		private final int[] bins;
		private final Vector<Double> outliersLo = new Vector<>(), outliersHi = new Vector<>();

		public Histogram(Interval domain, int binCount) {
			this.bins = new int[binCount];
			this.domain = domain;
		}

		public void insert(double value) {
			this.total += 1;
			final var t = Mth.inverseLerp(value, this.domain.lower(), this.domain.higher());
			if (t < 0) {
				this.outliersLo.push(value);
			} else if (t >= 1) {
				this.outliersHi.push(value);
			} else {
				final var bin = Mth.floor(t * this.bins.length);
				this.bins[bin] += 1;
			}
		}

		private static final int PERCENTAGE_BAR_LENGTH = 100;

		public void display() {
			final var sb = new StringBuilder();
			Mod.LOGGER.info("==================================================");
			for (int i = 0; i < this.bins.length; ++i) {
				sb.setLength(0);
				final var binPercent = this.bins[i] / (double) this.total;
				final var binLo = Mth.lerp(i / (double) this.bins.length, this.domain.lower(), this.domain.higher());
				final var binHi = Mth.lerp((i + 1) / (double) this.bins.length, this.domain.lower(),
						this.domain.higher());
				for (int j = 0; j < PERCENTAGE_BAR_LENGTH; ++j) {
					sb.append(j < binPercent * PERCENTAGE_BAR_LENGTH ? '#' : ' ');
				}
				Mod.LOGGER.info("{}",
						String.format("|%s| %d : %f%% : %f-%f", sb, this.bins[i], 100 * binPercent, binLo, binHi));
			}
			Mod.LOGGER.info("- - - - - - - - - - - - - - - - - - - - - - - - - ");
			Mod.LOGGER.info("Outliers (Lo): {}", this.outliersLo);
			Mod.LOGGER.info("Outliers (Hi): {}", this.outliersHi);
			Mod.LOGGER.info("Total: {}", this.total);
			Mod.LOGGER.info("==================================================");
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
			// final var levelSize = GalaxySector.sizeForLevel(0);
			sector.initialElements.iter().enumerate().forEach(elem -> {
				if (elem.item.pos().distanceTo(viewCenter) > levelSize)
					return;

				final var elemPos = elem.item.pos().mul(1e12 / camera.metersPerUnit);
				final var distance = ray.origin().distanceTo(elemPos);

				// final var distance = ray.origin().distanceTo(elemPos);
				if (!ray.intersectsSphere(elemPos, 0.02 * distance))
					return;

				final var elemPosRayRel = elemPos.sub(ray.origin());
				final var proj = elemPosRayRel.projectOnto(ray.dir());
				final var projDist = elemPosRayRel.distanceTo(proj);
				if (projDist < closest.distance) {
					closest.distance = projDist;
					closest.sectorPos = sector.pos();
					closest.sectorIndex = elem.index;
				}
			});
		});

		if (closest.sectorIndex == -1)
			return Maybe.none();
		return Maybe.some(GalaxySectorId.from(closest.sectorPos, closest.sectorIndex));
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		final var cullingCamera = getCullingCamera();
		final var viewCenter = getStarViewCenterPos(cullingCamera);
		final var ticket = this.starRenderer.getSectorTicket();

		this.starRenderer.draw(camera, viewCenter);

		// TODO: render things that are currently jumping between systems

		if (ClientConfig.get(ConfigKey.SHOW_SECTOR_BOUNDARIES)) {
			final var builder = BufferRenderer.IMMEDIATE_BUILDER;
			builder.begin(PrimitiveType.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			ticket.info.enumerateAffectedSectors(pos -> {
				final Vec3 s = pos.minBound(), e = pos.maxBound();
				final var color = ClientConfig.getDebugColor(pos.level()).withA(0.2);

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
			builder.end().draw(HawkShaders.getVanillaShader(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES),
					HawkDrawStates.DRAW_STATE_ADDITIVE_BLENDING);
		}
	}

}
