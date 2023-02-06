package net.xavil.universal.client.screen;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;

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
	private final Map<Integer, Vec3> positions = new HashMap<>();

	protected SystemMapScreen(@Nullable Screen previousScreen, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);
		this.system = system;

		this.camera.pitch.set(Math.PI / 8);
		this.camera.yaw.set(Math.PI / 8);
		this.camera.scale.set(4.0);
		this.camera.scale.setTarget(8.0);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy))
			return true;

		final var partialTick = this.client.getFrameTime();
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.TM_PER_LY;

		if (button == 2) {
			var realDy = this.camera.pitch.get(partialTick) < 0 ? -dy : dy;
			var offset = new Vec3(dx, 0, realDy).yRot(-this.camera.yaw.get(partialTick).floatValue()).scale(dragScale);
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

	private int getClosestNode(float partialTick) {
		int closest = -1;
		var focusPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
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
			this.selectedId = getClosestNode(partialTick);
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

		var offset = new Vec3(right, 0, forward).yRot(-this.camera.yaw.get(partialTick).floatValue()).scale(dragScale);
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

	private void renderNode(StarSystemNode node, OrbitalPlane referencePlane, double time, float partialTick,
			Vec3 centerPos) {
		// FIXME: elliptical orbits n stuff
		// FIXME: every node's reference plane is the root reference plane currently,
		// because im unsure how to transform child planes from being parent-relative to
		// root-relative.

		BufferBuilder builder = Tesselator.getInstance().getBuilder();

		var focusPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
		var toCenter = centerPos.subtract(focusPos);

		if (node instanceof BinaryNode binaryNode) {
			var angle = StarSystemNode.getBinaryAngle(binaryNode, time);

			var aCenter = centerPos.add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angle));
			var bCenter = centerPos.add(StarSystemNode.getBinaryOffsetB(referencePlane, binaryNode, angle));
			renderNode(binaryNode.getA(), referencePlane, time, partialTick, aCenter);
			renderNode(binaryNode.getB(), referencePlane, time, partialTick, bCenter);

			if (this.showGuides) {
				RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
				builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
				var pathSegments = 64;
				if (!(binaryNode.getA() instanceof BinaryNode)) {
					for (var i = 0; i < pathSegments; ++i) {
						var angleL = 2 * Math.PI * (i / (double) pathSegments);
						var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
						var aCenterL = centerPos
								.add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angleL));
						var aCenterH = centerPos
								.add(StarSystemNode.getBinaryOffsetA(referencePlane, binaryNode, angleH));
						RenderHelper.addLine(builder, aCenterL, aCenterH, BINARY_PATH_COLOR.withA(0.5));
					}
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

			var angle = StarSystemNode.getUnaryAngle(node, childOrbit, time);
			var center = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane, childOrbit, angle));

			renderNode(childNode, referencePlane, time, partialTick, center);

			if (this.showGuides) {
				RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
				builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
				var pathSegments = 64;
				for (var i = 0; i < pathSegments; ++i) {
					var angleL = 2 * Math.PI * (i / (double) pathSegments);
					var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
					var centerL = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane, childOrbit, angleL));
					var centerH = centerPos.add(StarSystemNode.getUnaryOffset(referencePlane, childOrbit, angleH));
					RenderHelper.addLine(builder, centerL, centerH, UNARY_PATH_COLOR.withA(0.5));
				}
				builder.end();
				RenderSystem.enableBlend();
				RenderSystem.disableTexture();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.depthMask(false);
				BufferUploader.end(builder);
			}
		}

		var projectedFocus = new Vec3(centerPos.x, focusPos.y, centerPos.z);
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

				var startD = new Vec3(centerPos.x, start, centerPos.z).distanceTo(focusPos);
				var endD = new Vec3(centerPos.x, end, centerPos.z).distanceTo(focusPos);

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
			RenderHelper.renderPlanet(builder, planetNode, 0.0001, new PoseStack(), centerPos, Color.WHITE);
		} else {
			RenderHelper.renderStarBillboard(builder, this.camera, node, centerPos, TM_PER_UNIT, partialTick);
		}
	}

	private double getGridScale(float partialTick) {
		return RenderHelper.getGridScale(this.camera, Units.TM_PER_AU, 10, partialTick);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		RenderSystem.depthMask(false);
		// fillGradient(poseStack, 0, 0, this.width, this.height, 0xcf000000,
		// 0xcf000000);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		var partialTick = this.client.getFrameTime();

		if (system == null)
			return;

		// TODO: figure out how we actually wanna handle time
		double time = (System.currentTimeMillis() % 1000000) / 1000f;
		// // return (double) this.client.level.getGameTime() + partialTick;

		StarSystemNode.positionNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO,
				(node, pos) -> this.positions.put(node.getId(), pos));

		if (followingId != -1) {
			var nodePos = this.positions.get(this.followingId);
			if (nodePos != null) {
				this.camera.focus.set(nodePos.scale(TM_PER_UNIT));
			}
		}

		var prevMatrices = this.camera.setupRenderMatrices(partialTick);

		final var builder = Tesselator.getInstance().getBuilder();

		var scale = getGridScale(tickDelta);
		poseStack.pushPose();
		poseStack.translate(0, this.camera.focus.get(partialTick).y, 0);
		poseStack.scale((float) scale * 0.025f, (float) scale * 0.025f, (float) scale * 0.025f);
		poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		this.client.font.draw(poseStack, "" + String.format("%.2f", scale / Units.TM_PER_AU) + " au", 0, 0,
				0x20777777);
		// drawString(poseStack, this.client.font, "scale: " + scale, 0, 0, 0xffffffff);
		poseStack.popPose();

		RenderHelper.renderGrid(builder, this.camera, TM_PER_UNIT, Units.TM_PER_AU,
		10, 100, partialTick);

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		var k = this.camera.scale.get(partialTick);
		RenderHelper.addBillboard(builder, this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT),
				new Vec3(0.02 * k, 0, 0),
				new Vec3(0, 0, 0.02 * k), Vec3.ZERO, 0, 0.5f, 0.5f, 1);
		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
				.setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		BufferUploader.end(builder);

		// for (var entry : this.positions.entrySet()) {
		// poseStack.pushPose();
		// poseStack.translate(entry.getValue().x, entry.getValue().y,
		// entry.getValue().z);
		// poseStack.scale((float) scale * 0.025f, (float) scale * 0.025f, (float) scale
		// * 0.025f);
		// poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
		// RenderSystem.depthMask(false);
		// RenderSystem.disableCull();
		// RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
		// GlStateManager.DestFactor.ONE);
		// this.client.font.draw(poseStack, "" + entry.getKey(), 0, 0,
		// 0x80777777);
		// // drawString(poseStack, this.client.font, "scale: " + scale, 0, 0,
		// 0xffffffff);
		// poseStack.popPose();
		// }

		renderNode(system.rootNode, OrbitalPlane.ZERO, time, partialTick, Vec3.ZERO);

		var closestId = getClosestNode(partialTick);
		var closestPos = this.positions.get(closestId);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);

		RenderHelper.renderLine(builder, this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT), closestPos,
				partialTick, new Color(1, 1, 1, 1));

		if (selectedId != -1) {
			var camPos = this.camera.getPos(partialTick);
			var nodePos = this.positions.get(this.selectedId);
			var distanceFromCamera = camPos.distanceTo(nodePos);

			if (nodePos != null) {
				RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				RenderHelper.addBillboard(builder, this.camera,
						nodePos, 0.05 * distanceFromCamera, 0, partialTick,
						new Color(1, 1, 1, 0.2f));
				builder.end();

				this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION)
						.setFilter(true, false);
				RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.disableDepthTest();
				BufferUploader.end(builder);
			}
		}

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
