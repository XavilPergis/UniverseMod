package net.xavil.ultraviolet.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Mth;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import java.util.Comparator;

import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.ultraviolet.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.collections.Blackboard;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerSystem extends HawkScreen3d.Layer3d {
	public final Galaxy galaxy;
	private final SystemTicket ticket;
	// private CelestialNode systemRoot;

	public boolean showGuides = true;

	private PlanetRenderingContext renderContext = this.disposer.attach(new PlanetRenderingContext());

	public ScreenLayerSystem(HawkScreen3d attachedScreen, Galaxy galaxy, GalaxySectorId systemId) {
		super(attachedScreen, new CameraConfig(1e-1, true, 1e7, true));
		// super(attachedScreen, new CameraConfig(1e-4, false, 1e5, false));
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
	public boolean handleKeypress(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_R) {
			final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);
			if (selectedId != -1) {
				final var packet = new ServerboundTeleportToLocationPacket();
				final var systemId = new SystemId(this.galaxy.galaxyId, this.ticket.id);
				final var id = new SystemNodeId(systemId, selectedId);
				packet.location = new WorldType.SystemNode(id);
				this.client.player.connection.send(packet);
			}
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_RIGHT) {
			final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
			if (system != null) {
				final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOrNull();
				if (selectedId == null) {
					insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, 0);
					insertBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE, 0);
				} else {
					for (int i = selectedId + 1;; ++i) {
						final var node = system.rootNode.lookup(i);
						if (node == null) {
							i = 0;
						}
						if (!(node instanceof BinaryCelestialNode)) {
							insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, i);
							insertBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE, i);
							break;
						}
					}
				}
			}
		} else if (keypress.keyCode == GLFW.GLFW_KEY_LEFT) {
			final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
			if (system != null) {
				final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOrNull();
				if (selectedId == null) {
					insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, 0);
					insertBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE, 0);
				} else {
					for (int i = selectedId - 1;; --i) {
						final var node = system.rootNode.lookup(i);
						if (node == null) {
							i = system.rootNode.getId();
						}
						if (!(node instanceof BinaryCelestialNode)) {
							insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE, i);
							insertBlackboard(BlackboardKeys.FOLLOWING_STAR_SYSTEM_NODE, i);
							break;
						}
					}
				}
			}
		} else if (keypress.keyCode == GLFW.GLFW_KEY_G) {
			this.showGuides = !this.showGuides;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_J) {
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
		double closestDistance = Double.POSITIVE_INFINITY;
		int closestId = -1;
		for (final var node : rootNode.iterable()) {
			final var elemPos = node.position.xyz().mul(1e12 / camera.metersPerUnit);
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
				final var pos = followingNode.position.xyz().mul(1e12 / camera.metersPerUnit);
				camera.focus.set(pos);
			}
		});
	}

	@Override
	public void render3d(Cached camera, RenderContext ctx) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var cullingCamera = getCullingCamera();
		final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
		if (system == null)
			return;

		final var universe = this.galaxy.parentUniverse;
		final var time = universe.getCelestialTime(this.client.isPaused() ? 0 : ctx.partialTick);

		this.renderContext.begin(time);
		this.renderContext.setupLights(system, camera);

		final var allNodes = system.rootNode.iter().filterCast(UnaryCelestialNode.class).collectTo(Vector::new);
		allNodes.sort(Comparator.comparingDouble(node -> node.position.distanceTo(this.camera.posTm)));
		allNodes.reverse();

		final var modelTfm = new TransformStack();

		for (final var node : allNodes.iterable()) {
			final var radiusTm = ClientConfig.get(ConfigKey.PLANET_EXAGGERATION_FACTOR) * Units.Tu_PER_ku * node.radius;
			final var radiusUnits = radiusTm * (1e12 / camera.metersPerUnit);
			final var nodePosUnits = node.position.mul(1e12 / camera.metersPerUnit);

			// final var distanceRatio = radiusTm / camera.posTm.distanceTo(pos);
			// if (distanceRatio < 0.0001)
			// return;
			// final var offset = camera.posTm.mul(1e12 / camera.metersPerUnit);

			modelTfm.push();
			modelTfm.appendRotation(Quat.axisAngle(Vec3.YP, node.rotationalRate * time));
			modelTfm.appendRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));
			// modelTfm.appendScale(radiusUnits);
			modelTfm.appendTransform(Mat4.scale(radiusUnits));
			// modelTfm.appendTranslation(nodePosUnits.mul(camera.metersPerUnit / 1e12));
			// modelTfm.appendRotation(camera.orientation.inverse());
			modelTfm.appendTranslation(camera.toCameraSpace(nodePosUnits));

			if (node instanceof StellarCelestialNode starNode
					&& starNode.type == StellarCelestialNode.Type.BLACK_HOLE) {
				final var shader = UltravioletShaders.SHADER_GRAVITATIONAL_LENSING.get();

				ctx.currentTexture.colorTexture.setWrapMode(GlTexture.WrapMode.MIRRORED_REPEAT);

				final var color = new Vec3.Mutable();
				StellarCelestialNode.blackBodyColorFromTable(color, 1200);

				shader.setUniformf("uAccretionDiscColor", color);
				// shader.setUniform("uAccretionDiscColor", 1.0, 0.2, 0.0);
				// shader.setUniform("uAccretionDiscColor", 0.5, 0.0, 1.0);
				// shader.setUniformf("uAccretionDiscColor", 0.0, 0.0, 0.0);
				shader.setUniformf("uAccretionDiscNormal", 0.5, 1.0, 0.2);
				shader.setUniformf("uAccretionDiscDensityFalloff", 3.0);
				shader.setUniformf("uAccretionDiscInnerPercent", 0.2);
				shader.setUniformf("uAccretionDiscInnerFalloff", 100.0);
				shader.setUniformf("uAccretionDiscDensityFalloffRadial", 4.0);
				shader.setUniformf("uAccretionDiscDensityFalloffVerticalInner", 100.0);
				shader.setUniformf("uAccretionDiscDensityFalloffVerticalOuter", 100.0);
				shader.setUniformf("uAccretionDiscBrightness", 250.0);
				shader.setUniformf("uEffectLimitFactor", 30.0);
				shader.setUniformf("uGravitationalConstant", 100.0);
				shader.setUniformf("uPosition", this.camera.toCameraSpace(node.position.xyz()));
				shader.setUniformf("uMass", node.massYg);

				// input textures
				shader.setUniformSampler("uColorTexture", ctx.currentTexture.colorTexture);
				shader.setUniformSampler("uDepthTexture", ctx.currentTexture.depthTexture);

				// camera uniforms
				final var frustumCorners = this.camera.captureFrustumCornersView();
				shader.setUniformf("uCameraFrustumNearNN", frustumCorners.nnp());
				shader.setUniformf("uCameraFrustumNearNP", frustumCorners.npp());
				shader.setUniformf("uCameraFrustumNearPN", frustumCorners.pnp());
				shader.setUniformf("uCameraFrustumNearPP", frustumCorners.ppp());
				shader.setUniformf("uCameraFrustumFarNN", frustumCorners.nnn());
				shader.setUniformf("uCameraFrustumFarNP", frustumCorners.npn());
				shader.setUniformf("uCameraFrustumFarPN", frustumCorners.pnn());
				shader.setUniformf("uCameraFrustumFarPP", frustumCorners.ppn());
				shader.setUniformf("uMetersPerUnit", this.camera.metersPerUnit);
				shader.setUniformf("uCameraNear", this.camera.nearPlane);
				shader.setUniformf("uCameraFar", this.camera.farPlane);

				final var newTexture = RenderTexture.HDR_COLOR_DEPTH.acquireTemporary();
				newTexture.framebuffer.bind();
				newTexture.framebuffer.clear();
				BufferRenderer.drawFullscreen(shader);

				ctx.replaceCurrentTexture(newTexture);
			}

			this.renderContext.render(builder, camera, node, modelTfm, false);

			if (node instanceof PlanetaryCelestialNode planetNode && planetNode.hasAtmosphere()
					&& !planetNode.hasAtmosphere()) {
				// i dont wanna deal with running the atmosphere shader multiple times right now
				final var brightestStars = system.rootNode.iter()
						.filterCast(StellarCelestialNode.class).collectTo(Vector::new);
				brightestStars.sort(Comparator.comparingDouble(node2 -> node2.luminosityLsol));
				brightestStars.reverse();
				brightestStars.truncate(4);

				final var shader = UltravioletShaders.SHADER_ATMOSPHERE.get();
				shader.setUniformf("uPosition", this.camera.toCameraSpace(node.position.xyz()));
				for (int i = 0; i < brightestStars.size(); ++i) {
					final var star = brightestStars.get(i);
					shader.setUniformf("uStarPos" + i, this.camera.toCameraSpace(star.position.xyz()));
					shader.setUniformf("uStarColor" + i, star.getColor().withA((float) star.luminosityLsol));
				}

				// input textures
				shader.setUniformSampler("uColorTexture", ctx.currentTexture.colorTexture);
				shader.setUniformSampler("uDepthTexture", ctx.currentTexture.depthTexture);

				// camera uniforms
				final var frustumCorners = this.camera.captureFrustumCornersView();
				shader.setUniformf("uCameraFrustumNearNN", frustumCorners.nnp());
				shader.setUniformf("uCameraFrustumNearNP", frustumCorners.npp());
				shader.setUniformf("uCameraFrustumNearPN", frustumCorners.pnp());
				shader.setUniformf("uCameraFrustumNearPP", frustumCorners.ppp());
				shader.setUniformf("uCameraFrustumFarNN", frustumCorners.nnn());
				shader.setUniformf("uCameraFrustumFarNP", frustumCorners.npn());
				shader.setUniformf("uCameraFrustumFarPN", frustumCorners.pnn());
				shader.setUniformf("uCameraFrustumFarPP", frustumCorners.ppn());
				shader.setUniformf("uMetersPerUnit", this.camera.metersPerUnit);
				shader.setUniformf("uCameraNear", this.camera.nearPlane);
				shader.setUniformf("uCameraFar", this.camera.farPlane);

				final var newTexture = RenderTexture.HDR_COLOR_DEPTH.acquireTemporary();
				newTexture.framebuffer.bind();
				newTexture.framebuffer.clear();
				BufferRenderer.drawFullscreen(shader);

				ctx.replaceCurrentTexture(newTexture);
			}
			modelTfm.pop();
		}

		for (final var node : system.rootNode.iterable()) {
			if (this.showGuides)
				showOrbitGuides(builder, camera, cullingCamera, node, time);
		}

		this.renderContext.end();

		// final var selectedId =
		// getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);
		// final var selectedNode = system.rootNode.lookup(selectedId);
		// if (selectedNode != null) {
		// final var pos = selectedNode.position.mul(1e12 / camera.metersPerUnit);
		// RenderHelper.renderUiBillboard(builder, camera, new TransformStack(), pos,
		// 0.02 * camera.pos.distanceTo(pos), Color.WHITE,
		// RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		// }
	}

	private static void addEllipseArc(FlexibleVertexConsumer builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, Ellipse ellipse, ColorRgba color, double endpointAngleL,
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

		if (ClientConfig.get(ConfigKey.SHOW_LINE_LODS)) {
			color = ClientConfig.getDebugColor(maxDepth);
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

			if (ClientConfig.get(ConfigKey.SHOW_ALL_LINE_LODS)) {
				RenderHelper.addLine(builder, camera,
						endpointL.mul(1e12 / cullingCamera.metersPerUnit),
						endpointH.mul(1e12 / cullingCamera.metersPerUnit),
						color.withA(0.1f));
			}
		} else {
			float alphaL = color.a, alphaH = color.a;
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
			OrbitCamera.Cached cullingCamera, Ellipse ellipse, ColorRgba color, boolean fadeOut) {
		var basePathSegments = 32;
		var maxDepth = 4;
		for (var i = 0; i < basePathSegments; ++i) {
			var angleL = 2 * Math.PI * (i / (double) basePathSegments);
			var angleH = 2 * Math.PI * ((i + 1) / (double) basePathSegments);
			addEllipseArc(builder, camera, cullingCamera, ellipse, color, angleL, angleH, maxDepth, fadeOut);
		}
	}

	private ColorRgba getPathColor(Blackboard.Key<String, ColorRgba> key, boolean selected) {
		return selected ? getBlackboardOrDefault(key) : getBlackboardOrDefault(BlackboardKeys.SELECTED_PATH_COLOR);
	}

	private void showOrbitGuides(VertexBuilder builder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNode node, double celestialTime) {
		if (node instanceof BinaryCelestialNode binaryNode) {
			showBinaryGuides(builder, camera, cullingCamera, binaryNode, celestialTime);
		}
		final var info = node.getOrbitInfo();
		if (info != null)
			showUnaryGuides(builder, camera, cullingCamera, info, celestialTime);
	}

	private void showBinaryGuides(VertexBuilder vertexBuilder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, BinaryCelestialNode node, double celestialTime) {
		final var builder = vertexBuilder.beginGeneric(PrimitiveType.LINES, BufferLayout.POSITION_COLOR_NORMAL);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);

		/* if (!(node.getA() instanceof BinaryCelestialNode)) */ {
			final var ellipse = node.getEllipseA(node.referencePlane, celestialTime);
			final var isSelected = selectedId != node.getInner().getId();
			final var color = getPathColor(BlackboardKeys.BINARY_PATH_COLOR, isSelected);
			addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);
		}
		/* if (!(node.getB() instanceof BinaryCelestialNode)) */ {
			final var ellipse = node.getEllipseB(node.referencePlane, celestialTime);
			final var isSelected = selectedId != node.getOuter().getId();
			final var color = getPathColor(BlackboardKeys.BINARY_PATH_COLOR, isSelected);
			addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);
		}

		RenderSystem.lineWidth(2);
		builder.end().draw(SHADER_VANILLA_RENDERTYPE_LINES.get(), DRAW_STATE_LINES);
	}

	private void showUnaryGuides(VertexBuilder vertexBuilder, OrbitCamera.Cached camera,
			OrbitCamera.Cached cullingCamera, CelestialNodeChild<?> orbiter, double celestialTime) {
		final var builder = vertexBuilder.beginGeneric(PrimitiveType.LINES, BufferLayout.POSITION_COLOR_NORMAL);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM_NODE).unwrapOr(-1);

		final var ellipse = orbiter.getEllipse(celestialTime);
		final var isSelected = selectedId != orbiter.node.getId();
		final var color = getPathColor(BlackboardKeys.UNARY_PATH_COLOR, isSelected);
		addEllipse(builder, camera, cullingCamera, ellipse, color, isSelected);

		RenderSystem.lineWidth(2);
		builder.end().draw(SHADER_VANILLA_RENDERTYPE_LINES.get(), DRAW_STATE_LINES);
	}

}
