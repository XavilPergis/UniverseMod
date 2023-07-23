package net.xavil.ultraviolet.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.ultraviolet.client.StarRenderManager;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.NewSystemMapScreen;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorPos;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
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
	private final SectorTicket<SectorTicketInfo.Multi> cameraTicket;
	private final StarRenderManager starRenderer;
	private final Vec3 originOffset;

	public ScreenLayerStars(HawkScreen3d attachedScreen, Galaxy galaxy, SectorTicketInfo.Multi ticketInfo,
			Vec3 originOffset) {
		super(attachedScreen, new CameraConfig(1e2, false, 1e8, false));
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.originOffset = originOffset;
		this.cameraTicket = galaxy.sectorManager.createSectorTicket(this.disposer, ticketInfo);
		this.starRenderer = new StarRenderManager(galaxy, this.originOffset);
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
		this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
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
		cameraTicket.info.centerPos = viewCenter;
		cameraTicket.info.multiplicitaveFactor = 2.0;

		this.starRenderer.draw(camera);
	}

}
