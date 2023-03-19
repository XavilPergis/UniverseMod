package net.xavil.universal.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.universal.client.ClientDebugFeatures;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.PlanetRenderingContext;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universal.networking.c2s.ServerboundTeleportToPlanetPacket;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Ellipse;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.Vec3;

public class SystemMapScreen extends UniversalScreen {

	private final Minecraft client = Minecraft.getInstance();
	private final StarSystem system;

	public static final double TM_PER_UNIT = Units.Tm_PER_au;

	public static final Color BINARY_PATH_COLOR = new Color(0.1f, 0.4f, 0.5f, 0.5f);
	public static final Color UNARY_PATH_COLOR = new Color(0.5f, 0.4f, 0.1f, 0.5f);

	private boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false, isRightPressed = false;
	private OrbitCamera camera = new OrbitCamera(1e12, TM_PER_UNIT);
	private int followingId = -1;
	private int selectedId = -1;
	private boolean showGuides = true;
	private final SystemId systemId;

	private static class ClientNodeInfo {
		public VertexBuffer vertexBuffer;
	}

	private final Map<Integer, ClientNodeInfo> clientInfo = new HashMap<>();

	public SystemMapScreen(@Nullable Screen previousScreen, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);
		this.systemId = systemId;
		this.system = system;

		// TODO: find a better default that plonking the user directly into the middle
		// of nowhere when loading binary systems.

		this.camera.pitch.set(Math.PI / 8);
		this.camera.yaw.set(Math.PI / 8);
		this.camera.scale.set(4.0);
		this.camera.scale.setTarget(8.0);
	}

	public SystemMapScreen(@Nullable Screen previousScreen, SystemNodeId id, StarSystem system) {
		this(previousScreen, id.system(), system);
		this.selectedId = this.followingId = id.nodeId();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy))
			return true;

		final var partialTick = this.client.getFrameTime();
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.Tm_PER_ly;

		if (button == 2) {
			var realDy = this.camera.pitch.get(partialTick) < 0 ? -dy : dy;
			var offset = Vec3.from(dx, 0, realDy).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
			followingId = -1;
		} else if (button == 1) {
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(0, dragScale * dy, 0));
			followingId = -1;
		} else if (button == 0) {
			this.camera.yaw.setTarget(this.camera.yaw.getTarget() + dx * 0.005);
			var desiredPitch = this.camera.pitch.getTarget() + dy * 0.005;
			var actualPitch = Mth.clamp(desiredPitch, -Math.PI / 2, Math.PI / 2);
			this.camera.pitch.setTarget(actualPitch);
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDelta))
			return true;

		if (scrollDelta > 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 1e-6));
			// this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 0.5));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * 1.2, 4e3));
			return true;
		}

		return false;
	}

	private CelestialNode getClosestNode(OrbitCamera.Cached camera) {
		CelestialNode closest = null;
		var focusPos = camera.focus.div(TM_PER_UNIT);

		final var nodes = new ArrayList<CelestialNode>();
		this.system.rootNode.visit(nodes::add);

		for (var node : nodes) {
			if (closest == null) {
				closest = node;
				continue;
			}
			if (!(node instanceof BinaryCelestialNode)) {
				var currentDist = node.position.distanceTo(focusPos);
				var closestDist = closest.position.distanceTo(focusPos);
				if (currentDist < closestDist)
					closest = node;
			}
		}

		return closest;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		final var partialTick = this.client.getFrameTime();

		if (keyCode == GLFW.GLFW_KEY_F) {
			this.selectedId = getClosestNode(this.camera.cached(partialTick)).getId();
			this.followingId = selectedId;
		} else if (keyCode == GLFW.GLFW_KEY_R  && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			Minecraft.getInstance().reloadResourcePacks();
		} else if (keyCode == GLFW.GLFW_KEY_R) {
			this.followingId = selectedId;
		} else if (keyCode == GLFW.GLFW_KEY_Q) {
			if (this.followingId == -1) {
				this.followingId = 0;
			} else {
				this.followingId += 1;
				if (this.system.rootNode.lookup(this.followingId) == null) {
					this.followingId = 0;
				}
			}
		} else if (keyCode == GLFW.GLFW_KEY_G) {
			this.showGuides = !this.showGuides;
		} else if (keyCode == GLFW.GLFW_KEY_T) {
			if (this.selectedId != -1) {
				var packet = new ServerboundTeleportToPlanetPacket();
				packet.planetId = new SystemNodeId(this.systemId, this.selectedId);
				this.client.player.connection.send(packet);
			}
		} else if (keyCode == GLFW.GLFW_KEY_J && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			final var universe = MinecraftClientAccessor.getUniverse(this.client);
			final var id = universe.getStartingSystemGenerator().getStartingSystemId();
			// final var system = universe.getSystem(id.system());
			var screen = new GalaxyMapScreen(client.screen, id.system());
			// this.client.setScreen(new SystemMapScreen(previousScreen, id, system));
			this.client.setScreen(screen);
		} else if (keyCode == GLFW.GLFW_KEY_P && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			ClientDebugFeatures.SHOW_ORBIT_PATH_SUBDIVISIONS.toggle();
		} else if (keyCode == GLFW.GLFW_KEY_L && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			ClientDebugFeatures.SHOW_ALL_ORBIT_PATH_LEVELS.toggle();
		}

		// TODO: key mappings
		if (keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = true;
			return true;
		}

		return false;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (super.keyReleased(keyCode, scanCode, modifiers))
			return true;

		// TODO: key mappings
		if (keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = false;
			return true;
		}

		return false;
	}

	@Override
	public void tick() {
		super.tick();
		this.camera.tick();

		double forward = 0, right = 0;
		double speed = 25;
		forward += this.isForwardPressed ? speed : 0;
		forward += this.isBackwardPressed ? -speed : 0;
		right += this.isLeftPressed ? speed : 0;
		right += this.isRightPressed ? -speed : 0;

		if (forward != 0 || right != 0)
			followingId = -1;

		// TODO: consolidate with the logic in mouseDragged()?
		final var partialTick = this.client.getFrameTime();
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.Tm_PER_ly;

		var offset = Vec3.from(right, 0, forward).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
	}

	static class NodeRenderer {
		private final Minecraft client = Minecraft.getInstance();
		private final PoseStack poseStack;

		public NodeRenderer(PoseStack poseStack) {
			this.poseStack = poseStack;
		}

		private static int computeMaxDepth(CelestialNode node, int depth) {
			var maxDepth = depth;
			if (node instanceof BinaryCelestialNode binaryNode) {
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.getA(), depth + 1));
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.getB(), depth + 1));
			}
			return maxDepth;
		}

		private void renderNodeMain(CelestialNode node) {
			var maxDepth = computeMaxDepth(node, 0);
			renderNode(node, 0, 5 * maxDepth, 0);
		}

		record SegmentInfo(int height, int segmentStart, int segmentEnd) {
			int center() {
				return (this.segmentStart + this.segmentEnd) / 2;
			}
		}

		private SegmentInfo renderNode(CelestialNode node, int depth, int xOff, int yOff) {
			if (node instanceof BinaryCelestialNode binaryNode) {
				var aInfo = renderNode(binaryNode.getA(), depth + 1, xOff, yOff);
				var bInfo = renderNode(binaryNode.getB(), depth + 1, xOff, yOff + aInfo.height);

				var lineStartY = aInfo.center();
				var lineEndY = aInfo.height + bInfo.center();

				var vertialX = 5 * depth;
				fill(poseStack, vertialX, yOff + lineStartY, vertialX + 1, yOff + lineEndY + 1, 0x77ffffff);

				var aEndX = binaryNode.getA() instanceof BinaryCelestialNode ? vertialX : xOff;
				var bEndX = binaryNode.getB() instanceof BinaryCelestialNode ? vertialX : xOff;

				fill(poseStack, vertialX + 1, yOff + lineStartY, aEndX + 5, yOff + lineStartY + 1, 0x77ffffff);
				fill(poseStack, vertialX + 1, yOff + lineEndY, bEndX + 5, yOff + lineEndY + 1, 0x77ffffff);

				return new SegmentInfo(aInfo.height + bInfo.height, lineStartY, lineEndY);
			} else {
				if (node instanceof StellarCelestialNode starNode) {
					var h = 0;
					var str = "";
					if (starNode.starClass() != null) {
						str += "[Class " + starNode.starClass().name + "] ";
					}
					str += node.toString();
					drawString(poseStack, this.client.font, str, xOff + 7, yOff + h, 0xffffffff);
					h += this.client.font.lineHeight;
					return new SegmentInfo(h, 0, h);
				}
			}

			return new SegmentInfo(0, 0, 0);
		}
	}

	private static final Color[] ORBIT_PATH_DEBUG_COLORS = { Color.RED, Color.GREEN, Color.BLUE, Color.CYAN,
			Color.MAGENTA, Color.YELLOW, };

	private static void addEllipseArc(VertexConsumer builder, OrbitCamera.Cached camera, Ellipse ellipse, Color color,
			double endpointAngleL, double endpointAngleH, int maxDepth, boolean fadeOut) {

		var subdivisionSegments = 2;

		var endpointL = ellipse.pointFromTrueAnomaly(endpointAngleL);
		var endpointH = ellipse.pointFromTrueAnomaly(endpointAngleH);
		var midpointSegment = endpointL.div(2).add(endpointH.div(2));

		var segmentLength = endpointL.distanceTo(endpointH);

		var maxDistance = 10 * camera.scale;
		var divisionFactor = 30;

		// var midpointAngle = (endpointAngleL + endpointAngleL) / 2;
		// var midpointIdeal = ellipse.pointFromTrueAnomaly(midpointAngle);
		// var totalMidpointError = midpointIdeal.distanceTo(midpointSegment);

		if (ClientDebugFeatures.SHOW_ORBIT_PATH_SUBDIVISIONS.isEnabled()) {
			color = ORBIT_PATH_DEBUG_COLORS[maxDepth % ORBIT_PATH_DEBUG_COLORS.length];
		}

		var isSegmentVisible = !fadeOut || midpointSegment.distanceTo(camera.pos) < segmentLength / 2 + maxDistance;
		var insideDivisionRadius = camera.pos.distanceTo(midpointSegment) < divisionFactor * segmentLength;
		// var insideDivisionRadius = camera.pos.distanceTo(midpointSegment) < divisionFactor * totalMidpointError;
		if (maxDepth > 0 && isSegmentVisible && insideDivisionRadius) {
			for (var i = 0; i < subdivisionSegments; ++i) {
				var percentL = i / (double) subdivisionSegments;
				var percentH = (i + 1) / (double) subdivisionSegments;
				var angleL = Mth.lerp(percentL, endpointAngleL, endpointAngleH);
				var angleH = Mth.lerp(percentH, endpointAngleL, endpointAngleH);
				addEllipseArc(builder, camera, ellipse, color, angleL, angleH, maxDepth - 1, fadeOut);
			}

			if (ClientDebugFeatures.SHOW_ALL_ORBIT_PATH_LEVELS.isEnabled()) {
				RenderHelper.addLine(builder, camera, endpointL, endpointH, color.withA(0.1));
			}
		} else {
			double alphaL = color.a(), alphaH = color.a();
			if (fadeOut) {
				var distL = camera.pos.distanceTo(endpointL);
				var distH = camera.pos.distanceTo(endpointH);
				alphaL *= Math.max(0, 1 - Mth.inverseLerp(distL, 0, maxDistance));
				alphaH *= Math.max(0, 1 - Mth.inverseLerp(distH, 0, maxDistance));
			}
			if (alphaH == 0 && alphaL == 0)
				return;
			RenderHelper.addLine(builder, camera.toCameraSpace(endpointL), camera.toCameraSpace(endpointH),
					color.withA(alphaL), color.withA(alphaH));
		}

	}

	private static void addEllipse(VertexConsumer builder, OrbitCamera.Cached camera, Ellipse ellipse, Color color,
			boolean fadeOut) {
		var basePathSegments = 32;
		var maxDepth = 20;
		for (var i = 0; i < basePathSegments; ++i) {
			var angleL = 2 * Math.PI * (i / (double) basePathSegments);
			var angleH = 2 * Math.PI * ((i + 1) / (double) basePathSegments);
			addEllipseArc(builder, camera, ellipse, color, angleL, angleH, maxDepth, fadeOut);
		}
	}

	private void showBinaryGuides(BufferBuilder builder, OrbitCamera.Cached camera, OrbitalPlane referencePlane,
			float partialTick, BinaryCelestialNode node) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		if (!(node.getA() instanceof BinaryCelestialNode)) {
			var ellipse = node.getEllipseA(node.referencePlane);
			var isSelected = this.selectedId != node.getA().getId();
			var color = isSelected ? BINARY_PATH_COLOR.withA(0.5) : new Color(0.2, 1.0, 0.2, 1.0);
			addEllipse(builder, camera, ellipse, color, isSelected);
		}
		if (!(node.getB() instanceof BinaryCelestialNode)) {
			var ellipse = node.getEllipseB(node.referencePlane);
			var isSelected = this.selectedId != node.getB().getId();
			var color = isSelected ? BINARY_PATH_COLOR.withA(0.5) : new Color(0.2, 1.0, 0.2, 1.0);
			addEllipse(builder, camera, ellipse, color, isSelected);
		}

		builder.end();
		RenderSystem.lineWidth(2f);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		BufferUploader.end(builder);
	}

	private void showUnaryGuides(BufferBuilder builder, OrbitCamera.Cached camera, OrbitalPlane referencePlane,
			float partialTick, CelestialNodeChild<?> orbiter) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		var ellipse = orbiter.getEllipse(orbiter.parentNode.referencePlane);
		var isSelected = this.selectedId != orbiter.node.getId();
		var color = isSelected ? UNARY_PATH_COLOR.withA(0.5) : new Color(0.2, 1.0, 0.2, 1.0);
		addEllipse(builder, camera, ellipse, color, isSelected);

		builder.end();
		RenderSystem.lineWidth(2f);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		BufferUploader.end(builder);
	}

	private void renderNode(OrbitCamera.Cached camera, CelestialNode node, PlanetRenderingContext ctx,
			float partialTick) {

		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		final var flexibleBuilder = BufferRenderer.immediateBuilder();

		if (node instanceof PlanetaryCelestialNode planetNode) {
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			ctx.renderPlanet(flexibleBuilder, camera, planetNode, new PoseStack(), Color.WHITE, false);
		} else {
			ctx.render(flexibleBuilder, camera, node, new PoseStack(), Color.WHITE, false);
		}

		if (node instanceof BinaryCelestialNode binaryNode && this.showGuides) {
			showBinaryGuides(builder, camera, binaryNode.referencePlane, partialTick, binaryNode);
		}

		for (var childOrbit : node.childOrbits()) {
			if (!this.showGuides)
				continue;
			showUnaryGuides(builder, camera, childOrbit.node.referencePlane, partialTick, childOrbit);
		}

	}

	private void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = Tesselator.getInstance().getBuilder();

		// system.rootNode.visit(node -> {
		// if (!this.clientInfo.containsKey(node.getId())) {
		// var info = new ClientNodeInfo();
		// info.vertexBuffer = new VertexBuffer();
		// this.clientInfo.put(node.getId(), info);
		// }
		// });

		RenderHelper.renderGrid(builder, camera, TM_PER_UNIT, Units.Tm_PER_au, 10,
				40, partialTick);

		final var universe = MinecraftClientAccessor.getUniverse(this.client);

		// FIXME: `Minecraft` has a `pausePartialTick` field that we should use instead
		// of 0 here.
		double time = universe.getCelestialTime(this.client.isPaused() ? 0 : partialTick);

		var ctx = new PlanetRenderingContext(time);
		system.rootNode.visit(node -> {
			if (node instanceof StellarCelestialNode starNode) {
				var light = PlanetRenderingContext.PointLight.fromStar(starNode);
				ctx.pointLights.add(light);
			}
		});

		system.rootNode.visit(node -> {
			renderNode(camera, node, ctx, partialTick);
		});

		// RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		// builder.begin(VertexFormat.Mode.QUADS,
		// DefaultVertexFormat.POSITION_COLOR_TEX);
		// var k = this.camera.scale.get(partialTick);
		// RenderHelper.addBillboard(builder,
		// new PoseStack(),
		// camera.focus.div(TM_PER_UNIT),
		// Vec3.from(0.02 * k, 0, 0),
		// Vec3.from(0, 0, 0.02 * k), Vec3.ZERO, 0, 0.5f, 0.5f, 1);
		// builder.end();

		// this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
		// .setFilter(true, false);
		// RenderSystem.setShaderTexture(0,
		// RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		// RenderSystem.enableBlend();
		// RenderSystem.defaultBlendFunc();
		// RenderSystem.disableCull();
		// RenderSystem.disableDepthTest();
		// BufferUploader.end(builder);

		var closestPos = getClosestNode(camera).position;
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);


		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		RenderHelper.addLine(builder, camera, camera.focus.div(TM_PER_UNIT), closestPos, new Color(1, 1, 1, 1));

		builder.end();

		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);


		// RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		// builder.begin(VertexFormat.Mode.QUADS,
		// DefaultVertexFormat.POSITION_COLOR_TEX);
		// this.system.rootNode.visit(node -> {
		// var camPos = this.camera.getPos(partialTick);
		// var nodePos = node.position;
		// var distanceFromCamera = camPos.distanceTo(nodePos);

		// if (this.selectedId == node.getId()) {
		// RenderHelper.addBillboard(builder, new PoseStack(), camera.up, camera.right,
		// nodePos,
		// 0.01 * distanceFromCamera, 0, new Color(1, 0.5, 0.5, 0.2));
		// } else {
		// RenderHelper.addBillboard(builder, new PoseStack(), camera.up, camera.right,
		// nodePos,
		// 0.01 * distanceFromCamera, 0, new Color(0.5, 0.5, 0.5, 0.2));
		// }
		// });
		// builder.end();
		// this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
		// .setFilter(true, false);
		// RenderSystem.setShaderTexture(0,
		// RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		// RenderSystem.enableBlend();
		// RenderSystem.defaultBlendFunc();
		// RenderSystem.disableCull();
		// RenderSystem.disableDepthTest();
		// BufferUploader.end(builder);
	}

	private void addNodeInfo(CelestialNode node, BiConsumer<String, String> consumer) {

		if (node instanceof BinaryCelestialNode)
			return;

		// consumer.accept("Mass", String.format("%.2f Yg", node.massYg));
		consumer.accept("Obliquity", String.format("%.2f rad", node.obliquityAngle));
		consumer.accept("Rotational Period", String.format("%.2f s", node.rotationalPeriod));

		// TODO: inclination n stuff

		if (node instanceof StellarCelestialNode starNode) {
			consumer.accept("Mass", String.format("%.4e Yg (%.2f M☉)", node.massYg, node.massYg / Units.Yg_PER_Msol));
			consumer.accept("Luminosity", String.format("%.6f L☉", starNode.luminosityLsol));
			consumer.accept("Radius", String.format("%.2f R☉", starNode.radiusRsol));
			consumer.accept("Temperature", String.format("%.0f K", starNode.temperatureK));
			var starClass = starNode.starClass();
			if (starClass != null)
				consumer.accept("Spectral Class", starClass.name);
			consumer.accept("Type", starNode.type.name());
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			if (planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
				consumer.accept("Mass",
						String.format("%.2f Yg (%.2f M♃)", node.massYg, node.massYg / Units.Yg_PER_Mjupiter));
			} else {
				consumer.accept("Mass",
						String.format("%.2f Yg (%.2f Mⴲ)", node.massYg, node.massYg / Units.Yg_PER_Mearth));
			}
			consumer.accept("Type", planetNode.type.name());
			consumer.accept("Temperature", String.format("%.0f K", planetNode.temperatureK));
			if (planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
				consumer.accept("Radius", String.format("%.2f R♃",
						planetNode.radiusRearth * (Units.m_PER_Rearth / Units.m_PER_Rjupiter)));
			} else {
				consumer.accept("Radius", String.format("%.2f Rⴲ", planetNode.radiusRearth));
			}
		}

	}

	private void render2d(PoseStack poseStack, float partialTick) {

		if (this.selectedId != -1) {
			var node = this.system.rootNode.lookup(this.selectedId);
			poseStack.pushPose();
			poseStack.translate(20, 20, 0);
			this.client.font.draw(poseStack, String.format("§9§l§nNode %s§r", "" + node.getId()), 0, 0, 0xff777777);
			poseStack.translate(4, 0, 0);

			var obj = new Object() {
				int currentHeight = client.font.lineHeight;
			};
			addNodeInfo(node, (property, value) -> {
				this.client.font.draw(poseStack, "§9" + property + "§r: " + value, 0, obj.currentHeight, 0xff777777);
				obj.currentHeight += this.client.font.lineHeight + 1;
			});

			// drawString(poseStack, this.client.font, "scale: " + scale, 0, 0, 0xffffffff);
			poseStack.popPose();
		}

		// RenderHelper.renderStarBillboard(null, null, poseStack, null);

		// poseStack.pushPose();
		// new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
		// poseStack.popPose();

	}

	@Override
	public boolean shouldRenderWorld() {
		return false;
	}

	@Override
	public void onClose() {
		super.onClose();
		this.clientInfo.values().forEach(info -> {
			if (info.vertexBuffer != null)
				info.vertexBuffer.close();
		});
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		RenderSystem.depthMask(false);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		final var partialTick = this.client.getFrameTime();

		if (system == null)
			return;

		final var universe = MinecraftClientAccessor.getUniverse(this.client);

		// FIXME: `Minecraft` has a `pausePartialTick` field that we should use instead
		// of 0 here.
		double time = universe.getCelestialTime(this.client.isPaused() ? 0 : partialTick);

		system.rootNode.updatePositions(time);

		if (followingId != -1) {
			var nodePos = this.system.rootNode.lookup(this.followingId).position;
			if (nodePos != null) {
				this.camera.focus.set(nodePos.mul(TM_PER_UNIT));
			}
		}

		final var camera = this.camera.cached(partialTick);

		final var prevMatrices = camera.setupRenderMatrices();
		render3d(camera, partialTick);
		prevMatrices.restore();
		render2d(poseStack, partialTick);
		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
