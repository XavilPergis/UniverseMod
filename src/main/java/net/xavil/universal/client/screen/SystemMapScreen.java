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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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

	private boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false, isRightPressed = false;
	private OrbitCamera camera = new OrbitCamera(TM_PER_UNIT);
	private int followingId = -1;
	private final Map<Integer, Vec3> positions = new HashMap<>();

	protected SystemMapScreen(@Nullable Screen previousScreen, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);
		this.system = system;

		followingId = 0;

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
			this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 0.5));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * 1.2, 4000));
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		if (keyCode == GLFW.GLFW_KEY_Q) {
			if (this.followingId == -1) {
				this.followingId = 0;
			} else {
				this.followingId += 1;
				if (this.system.rootNode.lookup(this.followingId) == null) {
					this.followingId = 0;
				}
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

	// TODO: figure out how we actually wanna handle time
	private double getTime(float partialTick) {
		return (System.currentTimeMillis() % 1000000) / 100000f;
		// return (double) this.client.level.getGameTime() + partialTick;
	}

	public static final double G = 0.00000001;
	public static final double k = 1;

	private double getUnaryAngle(StarSystemNode parent, StarSystemNode.UnaryOrbit orbit, double time) {
		var a = orbit.orbitalShape.semimajorAxisTm();
		// T = 2 * pi * sqrt(a^3 / (G * M))
		var period = 2 * Math.PI * Math.sqrt(a * a * a / (G * parent.massYg));
		return (orbit.isPrograde ? 1 : -1) * 2 * Math.PI * time / period + orbit.orbitalPlane.argumentOfPeriapsisRad();
	}

	private double getBinaryAngle(BinaryNode node, double time) {
		var a = node.getSemiMajorAxisA() + node.getSemiMajorAxisB();
		// T = 2 * pi * sqrt(a^3 / (G * (M1 + M2)))
		var period = 2 * Math.PI * Math.sqrt(a * a * a / (G * (node.getA().massYg + node.getB().massYg)));
		return 2 * Math.PI * time / period + node.orbitalPlane.argumentOfPeriapsisRad();
	}

	private Vec3 getUnaryOffset(OrbitalPlane referencePlane, StarSystemNode.UnaryOrbit orbit, double angle) {
		var plane = referencePlane.transform(orbit.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		// FIXME: elliptical orbits
		return Vec3.ZERO
				.add(apoapsisDir.scale(Math.cos(angle) * k * orbit.orbitalShape.semimajorAxisTm()))
				.add(rightDir.scale(Math.sin(angle) * k * orbit.orbitalShape.semiminorAxisTm()));
	}

	private Vec3 getBinaryOffsetA(OrbitalPlane referencePlane, BinaryNode node, double angle) {
		var plane = referencePlane.transform(node.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		return apoapsisDir.scale(k * node.getFocalDistanceA())
				.add(apoapsisDir.scale(Math.cos(angle) * k * node.getSemiMajorAxisA()))
				.add(rightDir.scale(Math.sin(angle) * k * node.getSemiMinorAxisA()));
	}

	private Vec3 getBinaryOffsetB(OrbitalPlane referencePlane, BinaryNode node, double angle) {
		var plane = referencePlane.transform(node.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		return apoapsisDir.scale(-k * node.getFocalDistanceB())
				.add(apoapsisDir.scale(Math.cos(angle) * -k * node.getSemiMajorAxisB()))
				.add(rightDir.scale(Math.sin(angle) * -k * node.getSemiMinorAxisB()));
	}

	private void positionNode(StarSystemNode node, OrbitalPlane referencePlane, float partialTick, Vec3 centerPos) {
		var time = getTime(partialTick);
		if (node instanceof BinaryNode binaryNode) {
			var angle = getBinaryAngle(binaryNode, time);
			var aCenter = centerPos.add(getBinaryOffsetA(referencePlane, binaryNode, angle));
			var bCenter = centerPos.add(getBinaryOffsetB(referencePlane, binaryNode, angle));
			positionNode(binaryNode.getA(), referencePlane, partialTick, aCenter);
			positionNode(binaryNode.getB(), referencePlane, partialTick, bCenter);
		}

		for (var childOrbit : node.childOrbits()) {
			var angle = getUnaryAngle(node, childOrbit, time);
			var center = centerPos.add(getUnaryOffset(referencePlane, childOrbit, angle));
			positionNode(childOrbit.node, referencePlane, partialTick, center);
		}

		this.positions.put(node.getId(), centerPos);
	}

	private void renderNode(StarSystemNode node, OrbitalPlane referencePlane, float partialTick, Vec3 centerPos) {
		// FIXME: elliptical orbits n stuff

		BufferBuilder builder = Tesselator.getInstance().getBuilder();

		var color = Color.rgb(1, 1, 1);
		if (node instanceof StarNode starNode) {
			color = starNode.getColor();
		}

		var time = getTime(partialTick);
		if (node instanceof BinaryNode binaryNode) {
			var angle = getBinaryAngle(binaryNode, time);

			var aCenter = centerPos.add(getBinaryOffsetA(referencePlane, binaryNode, angle));
			var bCenter = centerPos.add(getBinaryOffsetB(referencePlane, binaryNode, angle));
			renderNode(binaryNode.getA(), referencePlane, partialTick, aCenter);
			renderNode(binaryNode.getB(), referencePlane, partialTick, bCenter);

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			var pathSegments = 64;
			for (var i = 0; i < pathSegments; ++i) {
				var angleL = 2 * Math.PI * (i / (double) pathSegments);
				var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);

				var aCenterL = centerPos.add(getBinaryOffsetA(referencePlane, binaryNode, angleL));
				var aCenterH = centerPos.add(getBinaryOffsetA(referencePlane, binaryNode, angleH));
				var bCenterL = centerPos.add(getBinaryOffsetB(referencePlane, binaryNode, angleL));
				var bCenterH = centerPos.add(getBinaryOffsetB(referencePlane, binaryNode, angleH));

				var aN = aCenterL.subtract(aCenterH).normalize();
				var bN = bCenterL.subtract(bCenterH).normalize();
				builder.vertex(aCenterL.x, aCenterL.y, aCenterL.z).color(0.5f, 0.4f, 0.1f, 0.2f)
						.normal((float) aN.x, (float) aN.y, (float) aN.z).endVertex();
				builder.vertex(aCenterH.x, aCenterH.y, aCenterH.z).color(0.5f, 0.4f, 0.1f, 0.2f)
						.normal((float) aN.x, (float) aN.y, (float) aN.z).endVertex();
				builder.vertex(bCenterL.x, bCenterL.y, bCenterL.z).color(0.5f, 0.4f, 0.1f, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();
				builder.vertex(bCenterH.x, bCenterH.y, bCenterH.z).color(0.5f, 0.4f, 0.1f, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();

			}
			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
		}

		for (var childOrbit : node.childOrbits()) {
			var childNode = childOrbit.node;

			var angle = getUnaryAngle(node, childOrbit, time);
			var center = centerPos.add(getUnaryOffset(referencePlane, childOrbit, angle));

			renderNode(childNode, referencePlane, partialTick, center);

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			var pathSegments = 64;
			for (var i = 0; i < pathSegments; ++i) {
				var angleL = 2 * Math.PI * (i / (double) pathSegments);
				var angleH = 2 * Math.PI * ((i + 1) / (double) pathSegments);
				var centerL = centerPos.add(getUnaryOffset(referencePlane, childOrbit, angleL));
				var centerH = centerPos.add(getUnaryOffset(referencePlane, childOrbit, angleH));

				var bN = centerL.subtract(centerH).normalize();
				builder.vertex(centerL.x, centerL.y, centerL.z).color(0.2f, 0.2f, 0.2f, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();
				builder.vertex(centerH.x, centerH.y, centerH.z).color(0.2f, 0.2f, 0.2f, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();

			}
			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
		}

		var focusPos = this.camera.focus.get(partialTick);
		var toCenter = centerPos.subtract(focusPos);

		int maxLineSegments = 32;
		double lineSegmentLength = 0.25;
		var maxLength = 2 * maxLineSegments * lineSegmentLength;

		if (toCenter.x * toCenter.x + toCenter.z * toCenter.z < maxLength * maxLength) {
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
						.color(0.2f, 0.2f, 0.2f, 0.2f * sa).normal(0, 1, 0).endVertex();
				builder.vertex(centerPos.x, end, centerPos.z)
						.color(0.2f, 0.2f, 0.2f, 0.2f * ea).normal(0, 1, 0).endVertex();

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

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		var camPos = this.camera.getPos(partialTick);
		var distanceFromCamera = camPos.distanceTo(centerPos);

		final double starMinSize = 0.02, starBaseSize = 0.05, starRadiusFactor = 0.5;
		final double otherMinSize = 0.01, otherBaseSize = 0;
		final double brightBillboardSizeFactor = 0.5;

		if (node instanceof StarNode starNode) {
			var d = Math.max(starMinSize * distanceFromCamera,
					Math.max(starBaseSize, starRadiusFactor * starNode.radiusRsol));
			RenderHelper.renderBillboard(builder, this.camera, centerPos, d, 0, partialTick, color);
			RenderHelper.renderBillboard(builder, this.camera, centerPos, brightBillboardSizeFactor * d, 0,
					partialTick, Color.rgb(1, 1, 1));
		} else if (node instanceof PlanetNode planetNode) {
			var d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
			RenderHelper.renderBillboard(builder, this.camera, centerPos, d, 0, partialTick, color);
			RenderHelper.renderBillboard(builder, this.camera, centerPos, brightBillboardSizeFactor * d, 0,
					partialTick, Color.rgb(1, 1, 1));
		} else if (!(node instanceof BinaryNode)) {
			var d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
			RenderHelper.renderBillboard(builder, this.camera, centerPos, d, 0, partialTick, color);
			RenderHelper.renderBillboard(builder, this.camera, centerPos, brightBillboardSizeFactor * d, 0,
					partialTick, Color.rgb(1, 1, 1));
		}

		builder.end();
		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);
	}

	private double getGridScale(float partialTick) {
		var currentThreshold = Units.TM_PER_AU;
		var scaleFactor = 10;
		var scale = Units.TM_PER_AU;
		for (var i = 0; i < 10; ++i) {
			currentThreshold *= scaleFactor;
			if (this.camera.scale.get(partialTick) > currentThreshold)
				scale = currentThreshold;
		}
		return scale;
	}

	private void renderGrid(VertexConsumer builder, float partialTick) {
		var n = 25;
		var focusPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
		RenderHelper.renderGrid(builder, focusPos, getGridScale(partialTick) * n, 10, n);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		RenderSystem.depthMask(false);
		// fillGradient(poseStack, 0, 0, this.width, this.height, 0xcf000000,
		// 0xcf000000);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		var partialTick = this.client.getFrameTime();

		if (system != null) {
			positionNode(system.rootNode, OrbitalPlane.ZERO, partialTick, Vec3.ZERO);

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

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			renderGrid(builder, partialTick);
			builder.end();

			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.lineWidth(1);
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);

			renderNode(system.rootNode, OrbitalPlane.ZERO, partialTick, Vec3.ZERO);

			if (followingId != -1) {
				var camPos = this.camera.getPos(partialTick);
				var nodePos = this.positions.get(this.followingId);
				var distanceFromCamera = camPos.distanceTo(nodePos);

				if (nodePos != null) {
					this.camera.focus.set(nodePos.scale(TM_PER_UNIT));
					RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
					builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
					RenderHelper.renderBillboard(builder, this.camera,
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

			poseStack.pushPose();
			new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
			poseStack.popPose();

		}

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
