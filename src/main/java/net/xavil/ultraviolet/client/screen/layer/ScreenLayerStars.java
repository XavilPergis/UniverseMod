package net.xavil.ultraviolet.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
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
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorPos;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
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
				// final var ticket = this.galaxy.sectorManager.createSectorTicket(disposer, new SectorTicketInfo.Multi(
				// 		center, 3.0 * GalaxySector.BASE_SIZE_Tm, SectorTicketInfo.Multi.SCALES_UNIFORM));
				final var ticket = this.starRenderer.getSectorTicket();
				ticket.attachedManager.forceLoad(ticket);

				final var survey = new StarSurvey();
				survey.init(ticket);
				StarSurvey.printSurvey(survey, msg -> Mod.LOGGER.info("{}", msg));
			}

			return true;
		}

		return false;
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
