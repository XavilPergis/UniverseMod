package net.xavil.universal.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.xavil.universal.client.ClientDebugFeatures;
import net.xavil.universal.client.PlanetRenderingContext;
import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.camera.OrbitCamera.Cached;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universal.client.flexible.FlexibleVertexConsumer;
import net.xavil.universal.client.screen.BlackboardKeys;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.collections.Blackboard;
import net.xavil.util.math.Color;
import net.xavil.util.math.Ellipse;
import net.xavil.util.math.Ray;
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec3;

public class ScreenLayerSystem extends Universal3dScreen.Layer3d {
	public final Galaxy galaxy;
	private final SystemTicket ticket;

	public boolean showGuides = true;

	private boolean isPlanetRendererSetup = false;
	private PlanetRenderingContext renderContext = new PlanetRenderingContext();

	public ScreenLayerSystem(Universal3dScreen attachedScreen, Galaxy galaxy, GalaxySectorId systemId) {
		super(attachedScreen, new CameraConfig(0.01, 1e6, 1e12, Units.Tm_PER_au));
		this.galaxy = galaxy;
		this.ticket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId);
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
				if (!ray.intersectsSphere(elemPos, 0.1 * distance))
					continue;
				if (distance < closestDistance) {
					closestDistance = distance;
					closestId = node.getId();
				}
			}
			return closestId;
		}).unwrapOr(-1);
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
				final var pos = followingNode.position.div(camera.renderScale);
				camera.focus.set(pos);
			}
		});
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		final var cullingCamera = getCullingCamera();
		this.galaxy.getSystem(this.ticket.id).ifSome(system -> {
			final var universe = this.galaxy.parentUniverse;
			system.rootNode.updatePositions(universe.getCelestialTime(partialTick));

			if (!this.isPlanetRendererSetup)
				setupPlanetRenderer(system);

			final var nodes = system.rootNode.selfAndChildren();
			this.renderContext.setSystemOrigin(system.pos.mul(camera.renderScale));
			this.renderContext.begin(universe.getCelestialTime(partialTick));
			for (final var node : nodes.iterable()) {
				this.renderContext.render(builder, camera, node, false);
				if (this.showGuides)
					showOrbitGuides(builder, camera, cullingCamera, node);
			}
			this.renderContext.end();

			final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);
			final var selectedNode = system.rootNode.lookup(selectedId);
			if (selectedNode != null) {
				final var pos = selectedNode.position.div(camera.renderScale);
				RenderHelper.renderBillboard(builder, camera, new PoseStack(), pos,
						0.02 * camera.pos.distanceTo(pos), Color.WHITE, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION,
						GameRenderer.getPositionColorTexShader());
			}
		});
	}

	private static void addEllipseArc(FlexibleVertexConsumer builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, Ellipse ellipse, Color color, double endpointAngleL,
			double endpointAngleH, int maxDepth, boolean fadeOut) {

		final var camPos = cullingCamera.pos.mul(cullingCamera.renderScale);
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
						endpointL.div(cullingCamera.renderScale),
						endpointH.div(cullingCamera.renderScale),
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
					camera.toCameraSpace(endpointL.div(cullingCamera.renderScale)),
					camera.toCameraSpace(endpointH.div(cullingCamera.renderScale)),
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

	private Color getPathColor(Blackboard.Key<Color> key, boolean selected) {
		return selected ? getBlackboardOrDefault(key) : getBlackboardOrDefault(BlackboardKeys.SELECTED_PATH_COLOR);
	}

	private void showOrbitGuides(FlexibleBufferBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNode node) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			showBinaryGuides(builder, camera, cullingCamera, binaryNode);
		} else {
			final var info = node.getOrbitInfo();
			if (info != null)
				showUnaryGuides(builder, camera, cullingCamera, info);
		}
	}

	private void showBinaryGuides(FlexibleBufferBuilder builder, OrbitCamera.Cached camera,
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

		builder.end();
		RenderSystem.lineWidth(2f);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		builder.draw(GameRenderer.getRendertypeLinesShader());
	}

	private void showUnaryGuides(FlexibleBufferBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNodeChild<?> orbiter) {
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);

		final var ellipse = orbiter.getEllipse();
		final var isSelected = selectedId != orbiter.node.getId();
		final var color = getPathColor(BlackboardKeys.UNARY_PATH_COLOR, isSelected);
		addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);

		builder.end();
		RenderSystem.lineWidth(2f);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		builder.draw(GameRenderer.getRendertypeLinesShader());
	}

}