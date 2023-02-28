package net.xavil.universal.client.screen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.PlanetRenderingContext;
import net.xavil.universal.common.Ellipse;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.networking.c2s.ServerboundTeleportToPlanetPacket;

public class SystemMapScreen extends UniversalScreen {

	private final Minecraft client = Minecraft.getInstance();
	private final StarSystem system;

	public static final double TM_PER_UNIT = Units.TM_PER_AU;

	public static final Color BINARY_PATH_COLOR = new Color(0.5f, 0.4f, 0.1f, 0.5f);
	public static final Color UNARY_PATH_COLOR = new Color(0.2f, 0.2f, 0.2f, 0.5f);

	private boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false, isRightPressed = false;
	private OrbitCamera camera = new OrbitCamera(TM_PER_UNIT);
	private int followingId = -1;
	private int selectedId = -1;
	private boolean showGuides = true;
	private final SystemId systemId;
	private final Map<Integer, Vec3> positions = new HashMap<>();

	private Set<String> activeDebugFeatures = new HashSet<>();

	public SystemMapScreen(@Nullable Screen previousScreen, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);
		this.systemId = systemId;
		this.system = system;

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
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.TM_PER_LY;

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
			this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 0.001));
			// this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 0.5));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * 1.2, 4000));
			return true;
		}

		return false;
	}

	private int getClosestNode(OrbitCamera.Cached camera) {
		int closest = -1;
		var focusPos = camera.focus.div(TM_PER_UNIT);
		for (var entry : this.positions.entrySet()) {
			if (closest == -1) {
				closest = entry.getKey();
				continue;
			}
			var node = this.system.rootNode.lookup(entry.getKey());
			if (!(node instanceof BinaryNode)) {
				var currentDist = entry.getValue().distanceTo(focusPos);
				var closestDist = this.positions.get(closest).distanceTo(focusPos);
				if (currentDist < closestDist)
					closest = entry.getKey();
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
			this.selectedId = getClosestNode(this.camera.cached(partialTick));
			this.followingId = selectedId;
			Mod.LOGGER.warn("this.selectedId = " + this.selectedId);
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
		} else if (keyCode == GLFW.GLFW_KEY_P && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			if (this.activeDebugFeatures.contains("orbit_path_subdivisions")) {
				this.activeDebugFeatures.remove("orbit_path_subdivisions");
			} else {
				this.activeDebugFeatures.add("orbit_path_subdivisions");
			}
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
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.TM_PER_LY;

		var offset = Vec3.from(right, 0, forward).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
	}

	static class NodeRenderer {
		private final Minecraft client = Minecraft.getInstance();
		private final PoseStack poseStack;

		public NodeRenderer(PoseStack poseStack) {
			this.poseStack = poseStack;
		}

		private static int computeMaxDepth(StarSystemNode node, int depth) {
			var maxDepth = depth;
			if (node instanceof BinaryNode binaryNode) {
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.getA(), depth + 1));
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.getB(), depth + 1));
			}
			return maxDepth;
		}

		private void renderNodeMain(StarSystemNode node) {
			var maxDepth = computeMaxDepth(node, 0);
			renderNode(node, 0, 5 * maxDepth, 0);
		}

		record SegmentInfo(int height, int segmentStart, int segmentEnd) {
			int center() {
				return (this.segmentStart + this.segmentEnd) / 2;
			}
		}

		private SegmentInfo renderNode(StarSystemNode node, int depth, int xOff, int yOff) {
			if (node instanceof BinaryNode binaryNode) {
				var aInfo = renderNode(binaryNode.getA(), depth + 1, xOff, yOff);
				var bInfo = renderNode(binaryNode.getB(), depth + 1, xOff, yOff + aInfo.height);

				var lineStartY = aInfo.center();
				var lineEndY = aInfo.height + bInfo.center();

				var vertialX = 5 * depth;
				fill(poseStack, vertialX, yOff + lineStartY, vertialX + 1, yOff + lineEndY + 1, 0x77ffffff);

				var aEndX = binaryNode.getA() instanceof BinaryNode ? vertialX : xOff;
				var bEndX = binaryNode.getB() instanceof BinaryNode ? vertialX : xOff;

				fill(poseStack, vertialX + 1, yOff + lineStartY, aEndX + 5, yOff + lineStartY + 1, 0x77ffffff);
				fill(poseStack, vertialX + 1, yOff + lineEndY, bEndX + 5, yOff + lineEndY + 1, 0x77ffffff);

				return new SegmentInfo(aInfo.height + bInfo.height, lineStartY, lineEndY);
			} else {
				if (node instanceof StarNode starNode) {
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

	private void addEllipseArc(VertexConsumer builder, Ellipse ellipse, Vec3 cameraPos, Color color,
			double endpointAngleL, double endpointAngleH, int maxDepth) {

		var endpointL = ellipse.pointFromAngle(endpointAngleL);
		var endpointH = ellipse.pointFromAngle(endpointAngleH);
		var midpointSegment = endpointL.div(2).add(endpointH.div(2));

		// var midpointAngle = (endpointAngleL + endpointAngleL) / 2;
		// var midpointIdeal = ellipse.pointFromAngle(midpointAngle);
		// var totalMidpointError = midpointIdeal.distanceTo(midpointSegment);

		var divisionRadius = 10 * endpointL.distanceTo(endpointH);

		if (this.activeDebugFeatures.contains("orbit_path_subdivisions")) {
			color = ORBIT_PATH_DEBUG_COLORS[maxDepth % ORBIT_PATH_DEBUG_COLORS.length];
		}

		if (maxDepth > 0 && cameraPos.distanceTo(midpointSegment) < divisionRadius) {
			var subdivisionSegments = 2;
			for (var i = 0; i < subdivisionSegments; ++i) {
				var percentL = i / (double) subdivisionSegments;
				var percentH = (i + 1) / (double) subdivisionSegments;
				var angleL = Mth.lerp(percentL, endpointAngleL, endpointAngleH);
				var angleH = Mth.lerp(percentH, endpointAngleL, endpointAngleH);
				addEllipseArc(builder, ellipse, cameraPos, color, angleL, angleH, maxDepth - 1);
			}
		} else {
			RenderHelper.addLine(builder, endpointL, endpointH, color);
		}

	}

	private void addEllipse(VertexConsumer builder, Ellipse ellipse, Vec3 cameraPos, Color color) {
		var basePathSegments = 32;
		var maxDepth = 5;
		for (var i = 0; i < basePathSegments; ++i) {
			var angleL = 2 * Math.PI * (i / (double) basePathSegments);
			var angleH = 2 * Math.PI * ((i + 1) / (double) basePathSegments);
			addEllipseArc(builder, ellipse, cameraPos, color, angleL, angleH, maxDepth);
		}
	}

	private void renderNode(OrbitCamera.Cached camera, StarSystemNode node, OrbitalPlane referencePlane,
			PlanetRenderingContext ctx, double time, float partialTick, Vec3 centerPos) {
		// FIXME: elliptical orbits n stuff
		// FIXME: every node's reference plane is the root reference plane currently,
		// because im unsure how to transform child planes from being parent-relative to
		// root-relative.

		BufferBuilder builder = Tesselator.getInstance().getBuilder();

		var cameraPos = this.camera.getPos(partialTick);
		var focusPos = camera.focus.div(TM_PER_UNIT);
		var toCenter = centerPos.sub(focusPos);

		if (node instanceof BinaryNode binaryNode) {
			var angle = StarSystemNode.getBinaryAngle(binaryNode, time);

			var aCenter = centerPos.add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angle));
			var bCenter = centerPos.add(StarSystemNode.getBinaryOffsetB(referencePlane, binaryNode, angle));
			var newPlane = binaryNode.orbitalPlane.withReferencePlane(referencePlane);
			renderNode(camera, binaryNode.getA(), newPlane, ctx, time, partialTick, aCenter);
			renderNode(camera, binaryNode.getB(), newPlane, ctx, time, partialTick, bCenter);

			if (this.showGuides) {
				RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
				builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
				var pathSegments = 64;
				if (!(binaryNode.getA() instanceof BinaryNode)) {
					var plane = binaryNode.orbitalPlane.withReferencePlane(referencePlane);
					var upDir = plane.rotationFromReference().transform(Vec3.XP);
					var rightDir = plane.rotationFromReference().transform(Vec3.ZP);

					// return upDir.mul(node.getFocalDistanceA())
					// .add(upDir.mul(Math.cos(angle) * node.getSemiMajorAxisA()))
					// .add(rightDir.mul(Math.sin(angle) * node.getSemiMinorAxisA()));

					// addEllipse(builder, ellipse, cameraPos, BINARY_PATH_COLOR.withA(0.5));
					// for (var i = 0; i < pathSegments; ++i) {
					// var angleL = 2 * Math.PI * (i / (double) pathSegments);
					// var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
					// var aCenterL = centerPos
					// .add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angleL));
					// var aCenterH = centerPos
					// .add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angleH));
					// RenderHelper.addLine(builder, aCenterL, aCenterH,
					// BINARY_PATH_COLOR.withA(0.5));
					// }
				}
				if (!(binaryNode.getB() instanceof BinaryNode)) {
					for (var i = 0; i < pathSegments; ++i) {
						var angleL = 2 * Math.PI * (i / (double) pathSegments);
						var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
						var bCenterL = centerPos
								.add(StarSystemNode.getBinaryOffsetB(referencePlane, binaryNode, angleL));
						var bCenterH = centerPos
								.add(StarSystemNode.getBinaryOffsetB(referencePlane, binaryNode, angleH));
						RenderHelper.addLine(builder, bCenterL, bCenterH, BINARY_PATH_COLOR.withA(0.5));
					}
				}

				// var d = 0.01 * this.camera.getPos(partialTick).distanceTo(centerPos);
				// RenderHelper.addLine(builder, centerPos.subtract(d, 0, d), centerPos.add(d,
				// 0, d),
				// BINARY_PATH_COLOR.withA(0.2));
				// RenderHelper.addLine(builder, centerPos.subtract(-d, 0, d), centerPos.add(-d,
				// 0, d),
				// BINARY_PATH_COLOR.withA(0.2));
				RenderHelper.addLine(builder, aCenter, bCenter, BINARY_PATH_COLOR.withA(0.2));

				builder.end();
				RenderSystem.enableBlend();
				RenderSystem.disableTexture();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.depthMask(false);
				BufferUploader.end(builder);
			}
		}

		for (var childOrbit : node.childOrbits()) {
			var childNode = childOrbit.node;

			var ellipse = StarSystemNode.getUnaryEllipse(referencePlane, centerPos, childOrbit);

			var angle = StarSystemNode.getUnaryAngle(node, childOrbit, time);
			// var center = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane,
			// childOrbit, angle));
			var center = ellipse.pointFromAngle(angle);
			var newPlane = childOrbit.orbitalPlane.withReferencePlane(referencePlane);
			renderNode(camera, childNode, newPlane, ctx, time, partialTick, center);

			if (this.showGuides) {
				RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
				builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

				// var ellipse = StarSystemNode.getUnaryEllipse(referencePlane, centerPos,
				// childOrbit);
				addEllipse(builder, ellipse, cameraPos, BINARY_PATH_COLOR.withA(0.5));

				// var pathSegments = 64;
				// for (var i = 0; i < pathSegments; ++i) {
				// var angleL = 2 * Math.PI * (i / (double) pathSegments);
				// var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
				// var centerL = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane,
				// childOrbit, angleL));
				// var centerH = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane,
				// childOrbit, angleH));
				// RenderHelper.addLine(builder, centerL, centerH, UNARY_PATH_COLOR.withA(1));
				// }
				builder.end();
				RenderSystem.enableBlend();
				RenderSystem.disableTexture();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.depthMask(false);
				BufferUploader.end(builder);
			}
		}

		var projectedFocus = Vec3.from(centerPos.x, focusPos.y, centerPos.z);
		var d = 0.05 * this.camera.getPos(partialTick).distanceTo(projectedFocus);
		int maxLineSegments = 32;
		double lineSegmentLength = d * 0.25;
		var k = 4;
		var maxLength = 2 * maxLineSegments * lineSegmentLength;

		if (this.showGuides && toCenter.x * toCenter.x + toCenter.z * toCenter.z < k * maxLength * k * maxLength) {
			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

			double currentOffset = 0, prevOffset = 0;
			for (var i = 0; i < maxLineSegments; ++i) {
				var start = focusPos.y + prevOffset;
				var end = focusPos.y + (currentOffset + prevOffset) / 2;

				if (focusPos.y < centerPos.y) {
					if (end > centerPos.y)
						end = centerPos.y;
				} else {
					if (end < centerPos.y)
						end = centerPos.y;
				}

				var startD = Vec3.from(centerPos.x, start, centerPos.z).distanceTo(focusPos);
				var endD = Vec3.from(centerPos.x, end, centerPos.z).distanceTo(focusPos);

				float sa = 1 - (float) Math.pow(1 - Math.max(0, 1 - (startD / maxLength)), 3);
				float ea = 1 - (float) Math.pow(1 - Math.max(0, 1 - (endD / maxLength)), 3);
				builder.vertex(centerPos.x, start, centerPos.z)
						.color(0.5f, 0.5f, 0.5f, 0.5f * sa).normal(0, 1, 0).endVertex();
				builder.vertex(centerPos.x, end, centerPos.z)
						.color(0.5f, 0.5f, 0.5f, 0.5f * ea).normal(0, 1, 0).endVertex();

				prevOffset = currentOffset;
				if (focusPos.y < centerPos.y) {
					currentOffset += 2 * lineSegmentLength;
					if (prevOffset > centerPos.y - focusPos.y) {
						break;
					}
				} else {
					currentOffset -= 2 * lineSegmentLength;
					if (prevOffset < centerPos.y - focusPos.y) {
						break;
					}
				}

			}

			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
		}

		if (node instanceof PlanetNode planetNode) {
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			ctx.renderPlanet(planetNode, new PoseStack(), centerPos, 1e-12, Color.WHITE);
			// RenderHelper.renderPlanet(builder, planetNode,
			// this.camera.getPos(partialTick), 1e-12, new PoseStack(),
			// centerPos, Color.WHITE);
		} else {
			RenderHelper.renderStarBillboard(builder, camera, node, centerPos, TM_PER_UNIT, partialTick);
		}
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		RenderSystem.depthMask(false);
		// fillGradient(poseStack, 0, 0, this.width, this.height, 0xcf000000,
		// 0xcf000000);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		final var partialTick = this.client.getFrameTime();
		final var camera = this.camera.cached(partialTick);

		if (system == null)
			return;

		// TODO: figure out how we actually wanna handle time
		double time = (System.currentTimeMillis() % 1000000) / 1000f;

		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> this.positions.put(node.getId(), pos));

		if (followingId != -1) {
			var nodePos = this.positions.get(this.followingId);
			if (nodePos != null) {
				this.camera.focus.set(nodePos.mul(TM_PER_UNIT));
			}
		}

		var prevMatrices = camera.setupRenderMatrices(partialTick);

		final var builder = Tesselator.getInstance().getBuilder();

		RenderHelper.renderGrid(builder, camera, TM_PER_UNIT, 1e-3 * Units.TM_PER_AU,
				10, 100, partialTick);

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		var k = this.camera.scale.get(partialTick);
		RenderHelper.addBillboard(builder,
				new PoseStack(),
				camera.focus.div(TM_PER_UNIT),
				Vec3.from(0.02 * k, 0, 0),
				Vec3.from(0, 0, 0.02 * k), Vec3.ZERO, 0, 0.5f, 0.5f, 1);
		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
				.setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		BufferUploader.end(builder);

		var ctx = new PlanetRenderingContext(builder);
		system.rootNode.visit(node -> {
			if (node instanceof StarNode starNode) {
				final var pos = positions.get(starNode.getId());
				var light = new PlanetRenderingContext.PointLight(pos, starNode.getColor(), starNode.luminosityLsol);
				ctx.pointLights.add(light);
			}
		});

		renderNode(camera, system.rootNode, OrbitalPlane.ZERO, ctx, time, partialTick, Vec3.ZERO);

		var closestId = getClosestNode(camera);
		var closestPos = this.positions.get(closestId);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);

		RenderHelper.renderLine(builder, camera.focus.div(TM_PER_UNIT), closestPos,
				partialTick, new Color(1, 1, 1, 1));

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		for (var entry : this.positions.entrySet()) {
			var camPos = this.camera.getPos(partialTick);
			var nodePos = entry.getValue();
			var distanceFromCamera = camPos.distanceTo(nodePos);

			if (this.selectedId == entry.getKey()) {
				RenderHelper.addBillboard(builder, new PoseStack(), camera.up, camera.right, nodePos,
						0.01 * distanceFromCamera, 0, new Color(1, 0.5, 0.5, 0.2));
			} else {
				RenderHelper.addBillboard(builder, new PoseStack(), camera.up, camera.right, nodePos,
						0.01 * distanceFromCamera, 0, new Color(0.5, 0.5, 0.5, 0.2));
			}

		}
		builder.end();
		this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
				.setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		BufferUploader.end(builder);

		prevMatrices.restore();

		if (this.selectedId != -1) {
			int h = 0;
			var node = this.system.rootNode.lookup(this.selectedId);
			poseStack.pushPose();
			poseStack.translate(20, 20, 0);
			this.client.font.draw(poseStack, String.format("§9§l§nNode %s§r", "" + node.getId()), 0, h, 0xff777777);
			h += this.client.font.lineHeight + 1;
			poseStack.translate(4, 0, 0);

			if (node instanceof StarNode starNode) {
				this.client.font.draw(poseStack,
						String.format("§9Mass§r: %.4e Yg (%.2f M☉)", node.massYg, node.massYg / Units.YG_PER_MSOL), 0,
						h, 0xff777777);
				h += this.client.font.lineHeight;
				this.client.font.draw(poseStack, String.format("§9Luminosity§r: %.6f L☉", starNode.luminosityLsol), 0,
						h, 0xff777777);
				h += this.client.font.lineHeight;
				this.client.font.draw(poseStack, String.format("§9Radius§r: %.2f R☉", starNode.radiusRsol), 0, h,
						0xff777777);
				h += this.client.font.lineHeight;
				this.client.font.draw(poseStack, String.format("§9Temperature§r: %.0f K", starNode.temperatureK), 0, h,
						0xff777777);
				h += this.client.font.lineHeight;
				var starClass = starNode.starClass();
				if (starClass != null) {
					var s = "§9Spectral Class§r: " + starClass.name;
					this.client.font.draw(poseStack, s, 0, h, 0xff777777);
					h += this.client.font.lineHeight;
				}
				var s = "§9Type§r: " + starNode.type.name();
				this.client.font.draw(poseStack, s, 0, h, 0xff777777);
				h += this.client.font.lineHeight;
			} else if (node instanceof PlanetNode planetNode) {
				this.client.font.draw(poseStack, String.format("§9Mass§r: %.2f Yg", node.massYg), 0, h, 0xff777777);
				h += this.client.font.lineHeight;
				this.client.font.draw(poseStack, "§9Type§r: " + planetNode.type.name(), 0, h, 0xff777777);
				h += this.client.font.lineHeight;
			} else {
				this.client.font.draw(poseStack, String.format("§9Mass§r: %.2f Yg", node.massYg), 0, h, 0xff777777);
				h += this.client.font.lineHeight;
			}

			// drawString(poseStack, this.client.font, "scale: " + scale, 0, 0, 0xffffffff);
			poseStack.popPose();
		}

		// poseStack.pushPose();
		// new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
		// poseStack.popPose();

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
