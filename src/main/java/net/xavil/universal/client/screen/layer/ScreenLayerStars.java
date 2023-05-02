package net.xavil.universal.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.renderer.GameRenderer;
import net.xavil.universal.client.ClientDebugFeatures;
import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.camera.OrbitCamera.Cached;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.BillboardBatcher;
import net.xavil.universal.client.screen.BlackboardKeys;
import net.xavil.universal.client.screen.NewGalaxyMapScreen;
import net.xavil.universal.client.screen.NewSystemMapScreen;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.universal.common.universe.galaxy.SectorTicket;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.universal.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.math.Color;
import net.xavil.util.math.Ray;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec3;

public class ScreenLayerStars extends Universal3dScreen.Layer3d {
	private final NewGalaxyMapScreen screen;
	public final Galaxy galaxy;
	private final SectorTicket<SectorTicketInfo.Multi> cameraTicket;
	private Vec3 selectedPos;

	public ScreenLayerStars(NewGalaxyMapScreen attachedScreen, Galaxy galaxy, SectorTicketInfo.Multi ticketInfo) {
		super(attachedScreen, new CameraConfig(0.01, 1e6));
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.cameraTicket = galaxy.sectorManager.createSectorTicket(this.disposer, ticketInfo);
	}

	private Vec3 getStarViewCenterPos(OrbitCamera.Cached camera) {
		if (ClientDebugFeatures.SECTOR_TICKET_AROUND_FOCUS.isEnabled())
			return camera.focus;
		// return camera.pos.mul(camera.metersPerUnit /
		// 1e12).add(camera.forward.mul(GalaxySector.BASE_SIZE_Tm));
		return camera.pos.mul(camera.metersPerUnit / 1e12);
	}

	@Override
	public boolean handleClick(Vec2 mousePos, int button) {
		if (super.handleClick(mousePos, button))
			return true;

		if (button == 0) {
			final var ray = this.lastCamera.rayForPicking(this.client.getWindow(), mousePos);
			final var selected = pickElement(this.lastCamera, ray);
			insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM, selected.unwrapOrNull());
			this.selectedPos = selected
					.flatMap(this.galaxy.sectorManager::getInitial)
					.map(elem -> elem.pos())
					.unwrapOrNull();
			return true;
		}

		return false;
	}

	@Override
	public boolean handleKeypress(int keyCode, int scanCode, int modifiers) {
		if (super.handleKeypress(keyCode, scanCode, modifiers))
			return true;

		if (((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) && ((modifiers & GLFW.GLFW_MOD_ALT) != 0)) {
			if (keyCode == GLFW.GLFW_KEY_B) {
				ClientDebugFeatures.SHOW_SECTOR_BOUNDARIES.toggle();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_H) {
				ClientDebugFeatures.SECTOR_TICKET_AROUND_FOCUS.toggle();
				return true;
			}
		}

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
				disposer.dispose();
			}
			return true;
		}

		return false;
	}

	private Option<GalaxySectorId> pickElement(OrbitCamera.Cached camera, Ray ray) {
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
			return Option.none();
		return Option.some(GalaxySectorId.from(closest.sectorPos, closest.sectorIndex));
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		final var batcher = new BillboardBatcher(builder, 10000);

		final var cullingCamera = getCullingCamera();
		final var viewCenter = getStarViewCenterPos(cullingCamera);
		cameraTicket.info.centerPos = viewCenter;
		cameraTicket.info.multiplicitaveFactor = 2.0;

		batcher.begin(camera);
		this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
			// if (sector.pos().level() != 0) return;
			final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			if (!cullingCamera.isAabbInFrustum(min, max))
				return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			// final var levelSize = GalaxySector.sizeForLevel(0);
			final var camPos = cullingCamera.pos.mul(camera.metersPerUnit / 1e12);
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(viewCenter) > levelSize)
					return;
				final var toStar = elem.pos().sub(camPos);
				if (toStar.dot(cullingCamera.forward) <= 0)
					return;
				batcher.add(elem.info().primaryStar, elem.pos());
			});
		});
		batcher.end();

		if (this.selectedPos != null) {
			final var pos = this.selectedPos.mul(1e12 / camera.metersPerUnit);
			RenderHelper.renderBillboard(builder, camera, new TransformStack(), pos,
					0.02 * camera.pos.distanceTo(pos), Color.WHITE, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION,
					GameRenderer.getPositionColorTexShader());
		}
	}

}
