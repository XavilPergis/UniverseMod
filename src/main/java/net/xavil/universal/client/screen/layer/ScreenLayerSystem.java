package net.xavil.universal.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.xavil.universal.client.ClientDebugFeatures;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.BillboardBatcher;
import net.xavil.universal.client.screen.BlackboardKeys;
import net.xavil.universal.client.screen.NewGalaxyMapScreen;
import net.xavil.universal.client.screen.NewSystemMapScreen;
import net.xavil.universal.client.screen.OrbitCamera;
import net.xavil.universal.client.screen.OrbitCamera.Cached;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.math.Color;
import net.xavil.util.math.Ray;
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec3;

public class ScreenLayerSystem extends Universal3dScreen.Layer3d {
	private final NewSystemMapScreen screen;
	public final Galaxy galaxy;
	private final SystemTicket ticket;
	private Vec3 selectedPos;

	public ScreenLayerSystem(NewSystemMapScreen attachedScreen, Galaxy galaxy, GalaxySectorId systemId) {
		super(attachedScreen);
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.ticket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId);
	}

	@Override
	public boolean handleClick(Vec2 mousePos, int button) {
		if (super.handleClick(mousePos, button))
			return true;

		if (button == 0) {
			this.screen.getLastCamera().ifSome(camera -> {
				final var ray = camera.rayForPicking(this.client.getWindow(), mousePos);
				final var selected = pickNode(camera, ray);
				insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, selected);
				// this.selectedPos = selected
				// 		.flatMap(this.galaxy.sectorManager::getInitial)
				// 		.map(elem -> elem.pos())
				// 		.unwrapOrNull();
			});
			return true;
		}

		return false;
	}

	@Override
	public boolean handleKeypress(int keyCode, int scanCode, int modifiers) {
		if (super.handleKeypress(keyCode, scanCode, modifiers))
			return true;

		if (((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) && ((modifiers & GLFW.GLFW_MOD_ALT) != 0)) {
		}

		if (keyCode == GLFW.GLFW_KEY_R) {
			// final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
			// if (selected != null) {
			// 	final var disposer = new Disposable.Multi();
			// 	final var ticket = this.galaxy.sectorManager.createSystemTicket(disposer, selected);
			// 	this.galaxy.sectorManager.forceLoad(ticket);
			// 	this.galaxy.sectorManager.getSystem(selected).ifSome(system -> {
			// 		final var id = new SystemId(this.galaxy.galaxyId, selected);
			// 		final var screen = new NewSystemMapScreen(this.screen, this.galaxy, id, system);
			// 		screen.camera.pitch.set(this.screen.camera.pitch.getTarget());
			// 		screen.camera.yaw.set(this.screen.camera.yaw.getTarget());
			// 		this.client.setScreen(screen);
			// 	});
			// 	disposer.dispose();
			// }
			return true;
		}

		return false;
	}

	private int pickNode(OrbitCamera.Cached camera, Ray ray) {
		return this.galaxy.getSystem(this.ticket.id).map(system -> {
			final var nodes = system.rootNode.selfAndChildren();
			double closestDistance = Double.POSITIVE_INFINITY;
			int closestId = -1;
			for (final var node : nodes.iterable()) {
				final var elemPos = node.position.div(camera.renderScale);
				final var distance = ray.origin().distanceTo(elemPos);
				if (!ray.intersectsSphere(elemPos, 0.05 * distance))
					continue;
				if (distance < closestDistance) {
					closestDistance = distance;
					closestId = node.getId();
				}
			}
			return closestId;
		}).unwrapOr(-1);
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		// final var builder = BufferRenderer.immediateBuilder();
		// final var batcher = new BillboardBatcher(builder, 10000);

		// final var cullingCamera = this.screen.getCullingCamera(camera);

		// final var viewCenter = getStarViewCenterPos(cullingCamera);
		// batcher.begin(camera);
		// this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
		// 	// if (sector.pos().level() != 0) return;
		// 	final var min = sector.pos().minBound().div(cullingCamera.renderScale);
		// 	final var max = sector.pos().maxBound().div(cullingCamera.renderScale);
		// 	if (!cullingCamera.isAabbInFrustum(min, max))
		// 		return;
		// 	final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
		// 	// final var levelSize = GalaxySector.sizeForLevel(0);
		// 	final var camPos = cullingCamera.pos.mul(cullingCamera.renderScale);
		// 	sector.initialElements.forEach(elem -> {
		// 		if (elem.pos().distanceTo(viewCenter) > levelSize)
		// 			return;
		// 		final var toStar = elem.pos().sub(camPos);
		// 		if (toStar.dot(cullingCamera.forward) <= 0)
		// 			return;
		// 		batcher.add(elem.info().primaryStar, elem.pos());
		// 	});
		// });
		// batcher.end();

		// if (this.selectedPos != null) {
		// 	final var pos = this.selectedPos.div(camera.renderScale);
		// 	RenderHelper.renderBillboard(builder, camera, new PoseStack(), pos,
		// 			0.02 * camera.pos.distanceTo(pos), Color.WHITE, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION,
		// 			GameRenderer.getPositionColorTexShader());
		// }
	}

	
}
