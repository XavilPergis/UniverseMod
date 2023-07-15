package net.xavil.ultraviolet.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.util.Mth;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.ultraviolet.client.ClientDebugFeatures;
import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.ultraviolet.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.collections.Blackboard;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerSystem extends HawkScreen3d.Layer3d {
	public final Galaxy galaxy;
	private final SystemTicket ticket;
	// private CelestialNode systemRoot;

	public boolean showGuides = true;

	private boolean isPlanetRendererSetup = false;
	private PlanetRenderingContext renderContext = new PlanetRenderingContext();

	public ScreenLayerSystem(HawkScreen3d attachedScreen, Galaxy galaxy, GalaxySectorId systemId) {
		super(attachedScreen, new CameraConfig(0.01, 1e6));
		this.galaxy = galaxy;
		this.ticket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId);
		// this.systemRoot = this.ticket.forceLoad().unwrapOrNull();
	}

	@Override
	public boolean handleClick(Vec2 mousePos, int button) {
		if (super.handleClick(mousePos, button))
			return true;

		if (button == 0) {
			final var ray = this.lastCamera.rayForPicking(this.client.getWindow(), mousePos);
			final var selected = pickNode(this.lastCamera, ray);
			insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, selected);
			return true;
		}

		return false;
	}

	@Override
	public boolean handleKeypress(int keyCode, int scanCode, int modifiers) {
		if (super.handleKeypress(keyCode, scanCode, modifiers))
			return true;

		if (((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) && ((modifiers & GLFW.GLFW_MOD_ALT) != 0)) {
			if (keyCode == GLFW.GLFW_KEY_P) {
				ClientDebugFeatures.SHOW_ORBIT_PATH_SUBDIVISIONS.toggle();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_L) {
				ClientDebugFeatures.SHOW_ALL_ORBIT_PATH_LEVELS.toggle();
				return true;
			}
		}

		if (keyCode == GLFW.GLFW_KEY_R) {
			final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);
			if (selectedId != -1) {
				final var packet = new ServerboundTeleportToLocationPacket();
				final var systemId = new SystemId(this.galaxy.galaxyId, this.ticket.id);
				final var id = new SystemNodeId(systemId, selectedId);
				packet.location = new Location.World(id);
				this.client.player.connection.send(packet);
			}
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_G) {
			this.showGuides = !this.showGuides;
		} else if (keyCode == GLFW.GLFW_KEY_J) {
			final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOrNull();
			if (selectedId != null) {
				final var stationId = EntityAccessor.getStation(this.client.player);
				if (stationId == -1)
					return true;
				final var systemId = new SystemId(this.galaxy.galaxyId, this.ticket.id);
				final var id = new SystemNodeId(systemId, selectedId);
				final var packet = new ServerboundStationJumpPacket(stationId, id, false);
				this.client.player.connection.send(packet);
			}
			return true;
		}

		return false;
	}

	private int pickNode(OrbitCamera.Cached camera, Ray ray) {
		return this.galaxy.getSystem(this.ticket.id).map(system -> pickNode(camera, ray, system.rootNode)).unwrapOr(-1);
	}

	private int pickNode(OrbitCamera.Cached camera, Ray ray, CelestialNode rootNode) {
		final var nodes = rootNode.selfAndChildren();
		double closestDistance = Double.POSITIVE_INFINITY;
		int closestId = -1;
		for (final var node : nodes.iterable()) {
			final var elemPos = node.position.mul(1e12 / camera.metersPerUnit);
			final var distance = ray.origin().distanceTo(elemPos);
			if (!ray.intersectsSphere(elemPos, 0.1 * distance))
				continue;
			if (distance < closestDistance) {
				closestDistance = distance;
				closestId = node.getId();
			}
		}
		return closestId;
	}

	private void setupPlanetRenderer(StarSystem system) {
		system.rootNode.visit(node -> {
			if (node instanceof StellarCelestialNode starNode) {
				final var light = PlanetRenderingContext.PointLight.fromStar(starNode);
				this.renderContext.pointLights.add(light);
			}
		});
		this.isPlanetRendererSetup = true;
	}

	@Override
	public void onMoved(Vec3 displacement) {
		super.onMoved(displacement);
		removeBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE);
	}

	@Override
	public void setup3d(OrbitCamera camera, float partialTick) {
		super.setup3d(camera, partialTick);
		this.galaxy.getSystem(this.ticket.id).ifSome(system -> {
			final var followingId = getBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE).unwrapOr(-1);
			final var followingNode = system.rootNode.lookup(followingId);
			if (followingNode != null) {
				final var pos = followingNode.position.mul(1e12 / camera.metersPerUnit);
				camera.focus.set(pos);
			}
		});
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var cullingCamera = getCullingCamera();
		this.galaxy.getSystem(this.ticket.id).ifSome(system -> {
			final var universe = this.galaxy.parentUniverse;
			final var time = universe.getCelestialTime(this.client.isPaused() ? 0 : partialTick);
			// system.rootNode.updatePositions(time);

			if (!this.isPlanetRendererSetup)
				setupPlanetRenderer(system);

			system.pos.mul(1e12 / camera.metersPerUnit);
			final var nodes = system.rootNode.selfAndChildren();
			// this.renderContext.setSystemOrigin(system.pos);
			this.renderContext.begin(time);
			for (final var node : nodes.iterable()) {
				this.renderContext.render(builder, camera, node, false);
				if (this.showGuides)
					showOrbitGuides(builder, camera, cullingCamera, node);
			}
			this.renderContext.end();

			final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);
			final var selectedNode = system.rootNode.lookup(selectedId);
			// if (selectedNode != null) {
			// 	final var pos = selectedNode.position.mul(1e12 / camera.metersPerUnit);
			// 	RenderHelper.renderUiBillboard(builder, camera, new TransformStack(), pos,
			// 			0.02 * camera.pos.distanceTo(pos), Color.WHITE, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
			// }
		});
	}

	private static void addEllipseArc(FlexibleVertexConsumer builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, Ellipse ellipse, Color color, double endpointAngleL,
			double endpointAngleH, int maxDepth, boolean fadeOut) {

		final var camPos = cullingCamera.pos.mul(camera.metersPerUnit / 1e12);
		var subdivisionSegments = 2;

		var endpointL = ellipse.pointFromTrueAnomaly(endpointAngleL);
		var endpointH = ellipse.pointFromTrueAnomaly(endpointAngleH);
		var midpointSegment = endpointL.div(2).add(endpointH.div(2));

		var segmentLength = endpointL.distanceTo(endpointH);

		var maxDistance = 10 * cullingCamera.scale;
		var divisionFactor = 30;

		// var midpointAngle = (endpointAngleL + endpointAngleL) / 2;
		// var midpointIdeal = ellipse.pointFromTrueAnomaly(midpointAngle);
		// var totalMidpointError = midpointIdeal.distanceTo(midpointSegment);

		if (ClientDebugFeatures.SHOW_ORBIT_PATH_SUBDIVISIONS.isEnabled()) {
			color = ClientDebugFeatures.getDebugColor(maxDepth);
		}

		var isSegmentVisible = !fadeOut
				|| midpointSegment.distanceTo(camPos) < segmentLength / 2 + maxDistance;
		var insideDivisionRadius = camPos.distanceTo(midpointSegment) < divisionFactor * segmentLength;
		// var insideDivisionRadius = camPos.distanceTo(midpointSegment) <
		// divisionFactor * totalMidpointError;
		if (maxDepth > 0 && isSegmentVisible && insideDivisionRadius) {
			for (var i = 0; i < subdivisionSegments; ++i) {
				var percentL = i / (double) subdivisionSegments;
				var percentH = (i + 1) / (double) subdivisionSegments;
				var angleL = Mth.lerp(percentL, endpointAngleL, endpointAngleH);
				var angleH = Mth.lerp(percentH, endpointAngleL, endpointAngleH);
				addEllipseArc(builder, camera, cullingCamera, ellipse, color, angleL, angleH, maxDepth - 1, fadeOut);
			}

			if (ClientDebugFeatures.SHOW_ALL_ORBIT_PATH_LEVELS.isEnabled()) {
				RenderHelper.addLine(builder, camera,
						endpointL.mul(1e12 / cullingCamera.metersPerUnit),
						endpointH.mul(1e12 / cullingCamera.metersPerUnit),
						color.withA(0.1));
			}
		} else {
			double alphaL = color.a(), alphaH = color.a();
			if (fadeOut) {
				var distL = camPos.distanceTo(endpointL);
				var distH = camPos.distanceTo(endpointH);
				alphaL *= Math.max(0, 1 - Mth.inverseLerp(distL, 0, maxDistance));
				alphaH *= Math.max(0, 1 - Mth.inverseLerp(distH, 0, maxDistance));
			}
			if (alphaH == 0 && alphaL == 0)
				return;
			RenderHelper.addLine(builder,
					camera.toCameraSpace(endpointL.mul(1e12 / cullingCamera.metersPerUnit)),
					camera.toCameraSpace(endpointH.mul(1e12 / cullingCamera.metersPerUnit)),
					color.withA(alphaL), color.withA(alphaH));
		}

	}

	private static void addEllipse(FlexibleVertexConsumer builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, Ellipse ellipse, Color color, boolean fadeOut) {
		var basePathSegments = 32;
		var maxDepth = 20;
		for (var i = 0; i < basePathSegments; ++i) {
			var angleL = 2 * Math.PI * (i / (double) basePathSegments);
			var angleH = 2 * Math.PI * ((i + 1) / (double) basePathSegments);
			addEllipseArc(builder, camera, cullingCamera, ellipse, color, angleL, angleH, maxDepth, fadeOut);
		}
	}

	private Color getPathColor(Blackboard.Key<String, Color> key, boolean selected) {
		return selected ? getBlackboardOrDefault(key) : getBlackboardOrDefault(BlackboardKeys.SELECTED_PATH_COLOR);
	}

	private void showOrbitGuides(VertexBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNode node) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			showBinaryGuides(builder, camera, cullingCamera, binaryNode);
		} else {
			final var info = node.getOrbitInfo();
			if (info != null)
				showUnaryGuides(builder, camera, cullingCamera, info);
		}
	}

	private void showBinaryGuides(VertexBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, BinaryCelestialNode node) {
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);

		if (!(node.getA() instanceof BinaryCelestialNode)) {
			final var ellipse = node.getEllipseA(node.referencePlane);
			final var isSelected = selectedId != node.getA().getId();
			final var color = getPathColor(BlackboardKeys.BINARY_PATH_COLOR, isSelected);
			addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);
		}
		if (!(node.getB() instanceof BinaryCelestialNode)) {
			final var ellipse = node.getEllipseB(node.referencePlane);
			final var isSelected = selectedId != node.getB().getId();
			final var color = getPathColor(BlackboardKeys.BINARY_PATH_COLOR, isSelected);
			addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);
		}

		builder.end().draw(getVanillaShader(SHADER_VANILLA_RENDERTYPE_LINES), DRAW_STATE_LINES);
	}

	private void showUnaryGuides(VertexBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNodeChild<?> orbiter) {
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);

		final var ellipse = orbiter.getEllipse();
		final var isSelected = selectedId != orbiter.node.getId();
		final var color = getPathColor(BlackboardKeys.UNARY_PATH_COLOR, isSelected);
		addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);

		builder.end().draw(getVanillaShader(SHADER_VANILLA_RENDERTYPE_LINES), DRAW_STATE_LINES);
	}

}
