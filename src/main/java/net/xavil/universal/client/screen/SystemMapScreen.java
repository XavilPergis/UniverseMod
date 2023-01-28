package net.xavil.universal.client.screen;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;

public class SystemMapScreen extends UniversalScreen {

	private final Minecraft client = Minecraft.getInstance();
	private final StarSystem system;

	// private abstract class Selectable {
	// public double x, y;

	// protected Selectable(double x, double y) {}
	// }

	// private final List<Selectable> selectables = new ArrayList<>();

	protected SystemMapScreen(@Nullable Screen previousScreen, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);
		this.system = system;
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

		// private double renderStarNode(StarNode node, boolean horizontal, int depth,
		// int xOff, int yOff) {
		// var height = 0;
		// var str = "";
		// if (starNode.starClass() != null) {
		// str += "[Class " + starNode.starClass().name + "] ";
		// }
		// str += node.toString();
		// drawString(poseStack, this.client.font, str, xOff + 7, yOff + height,
		// 0xffffffff);
		// h += this.client.font.lineHeight;
		// return height;

		// }

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
		return (System.currentTimeMillis() % 1000000) / 1000f;
		// return (double) this.client.level.getGameTime() + partialTick;
	}

	private void renderNode(StarSystemNode node, float partialTick, Vec3 centerPos) {
		// FIXME: elliptical orbits n stuff

		BufferBuilder builder = Tesselator.getInstance().getBuilder();

		var G = 0.01;
		var k = 0.02;

		float r = 1, g = 0, b = 1, a = 0.5f;
		float d = 1;

		if (node instanceof StarNode starNode) {
			r = 0;
			g = 1;
			b = 0;

			if (starNode.type == StarNode.Type.WHITE_DWARF) {
				r = 1;
			}
			if (starNode.type == StarNode.Type.NEUTRON_STAR) {
				r = 1;
				g = 0;
			}
		}

		var time = getTime(partialTick);
		if (node instanceof BinaryNode binaryNode) {
			var apoapsisDir = new Vec3(1, 0, 0).zRot((float) binaryNode.orbitalPlane.longitueOfAscendingNodeRad());
			var rightDir = new Vec3(0, 1, 0).zRot((float) binaryNode.orbitalPlane.longitueOfAscendingNodeRad());

			var dsfsd = binaryNode.getSemiMajorAxisA() + binaryNode.getSemiMajorAxisB();
			var period = 2 * Math.PI
					* Math.sqrt(dsfsd * dsfsd * dsfsd / (G * (binaryNode.getA().massYg + binaryNode.getB().massYg)));
			var angle = 2 * Math.PI * time / period;

			// T = 2 * pi * sqrt(a^3 / (G * (M1 + M2)))

			var aCenter = centerPos.add(apoapsisDir.scale(k * binaryNode.getFocalDistanceA()))
					.add(apoapsisDir.scale(Math.cos(angle) * k * binaryNode.getSemiMajorAxisA()))
					.add(rightDir.scale(Math.sin(angle) * k * binaryNode.getSemiMinorAxisA()));

			var bCenter = centerPos.add(apoapsisDir.scale(-k * binaryNode.getFocalDistanceB()))
					.add(apoapsisDir.scale(-Math.cos(angle) * k * binaryNode.getSemiMajorAxisB()))
					.add(rightDir.scale(-Math.sin(angle) * k * binaryNode.getSemiMinorAxisB()));

			renderNode(binaryNode.getA(), partialTick, aCenter);
			renderNode(binaryNode.getB(), partialTick, bCenter);

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			var pathSegments = 64;
			for (var i = 0; i < pathSegments; ++i) {
				var percentL = i / (double) pathSegments;
				var percentH = (i + 1) / (double) pathSegments;
				// var angle2 = 2 * Math.PI * time / period;
				var angleL = 2 * Math.PI * percentL;
				var angleH = 2 * Math.PI * percentH;

				// T = 2 * pi * sqrt(a^3 / (G * (M1 + M2)))

				var aCenterL = centerPos.add(apoapsisDir.scale(k * binaryNode.getFocalDistanceA()))
						.add(apoapsisDir.scale(Math.cos(angleL) * k * binaryNode.getSemiMajorAxisA()))
						.add(rightDir.scale(Math.sin(angleL) * k * binaryNode.getSemiMinorAxisA()));
				var aCenterH = centerPos.add(apoapsisDir.scale(k * binaryNode.getFocalDistanceA()))
						.add(apoapsisDir.scale(Math.cos(angleH) * k * binaryNode.getSemiMajorAxisA()))
						.add(rightDir.scale(Math.sin(angleH) * k * binaryNode.getSemiMinorAxisA()));

				var bCenterL = centerPos.add(apoapsisDir.scale(-k * binaryNode.getFocalDistanceB()))
						.add(apoapsisDir.scale(-Math.cos(angleL) * k * binaryNode.getSemiMajorAxisB()))
						.add(rightDir.scale(-Math.sin(angleL) * k * binaryNode.getSemiMinorAxisB()));
				var bCenterH = centerPos.add(apoapsisDir.scale(-k * binaryNode.getFocalDistanceB()))
						.add(apoapsisDir.scale(-Math.cos(angleH) * k * binaryNode.getSemiMajorAxisB()))
						.add(rightDir.scale(-Math.sin(angleH) * k * binaryNode.getSemiMinorAxisB()));

				var aN = aCenterL.subtract(aCenterH).normalize();
				var bN = bCenterL.subtract(bCenterH).normalize();
				builder.vertex(aCenterL.x, aCenterL.y, aCenterL.z).color(1, 0, 0, 0.2f)
						.normal((float) aN.x, (float) aN.y, (float) aN.z).endVertex();
				builder.vertex(aCenterH.x, aCenterH.y, aCenterH.z).color(1, 0, 0, 0.2f)
						.normal((float) aN.x, (float) aN.y, (float) aN.z).endVertex();
				builder.vertex(bCenterL.x, bCenterL.y, bCenterL.z).color(0, 1, 0, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();
				builder.vertex(bCenterH.x, bCenterH.y, bCenterH.z).color(0, 1, 0, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();

			}
			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		}

		for (var childOrbit : node.childOrbits()) {
			var childNode = childOrbit.node();
			var radius = childOrbit.orbitalShape().semimajorAxisTm();

			// the squares of the orbital periods of the planets are directly proportional
			// to the cubes of the semi-major axes of their orbits

			// T^2 ∝ a^3
			// a^3 / T^2 = G * (M + m) / (4 * pi^2)
			// 1 / T^2 = G * (M + m) / (4 * pi^2 * a^3)
			// T^2 = 4 * pi^2 * a^3 / (G * (M + m))

			// T = sqrt(4 * pi^2 * a^3 / (G * (M + m)))

			// theta = 2 * pi * t/T

			var period = Math
					.sqrt(4 * Math.PI * Math.PI * radius * radius * radius / (G * (node.massYg + childNode.massYg)));
			var angle = 2 * Math.PI * time / period;
			if (!childOrbit.isPrograde()) angle *= -1;
			var center = centerPos.add(k * radius * Math.cos(angle), k * radius * Math.sin(angle), 0);

			renderNode(childNode, partialTick, center);

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			var pathSegments = 64;
			for (var i = 0; i < pathSegments; ++i) {
				var percentL = i / (double) pathSegments;
				var percentH = (i + 1) / (double) pathSegments;
				// var angle2 = 2 * Math.PI * time / period;
				var angleL = 2 * Math.PI * percentL;
				var angleH = 2 * Math.PI * percentH;

				if (!childOrbit.isPrograde()) {
					angleL *= -1;
					angleH *= -1;
				}

				// T = 2 * pi * sqrt(a^3 / (G * (M1 + M2)))

				var centerL = centerPos.add(k * radius * Math.cos(angleL), k * radius * Math.sin(angleL), 0);
				var centerH = centerPos.add(k * radius * Math.cos(angleH), k * radius * Math.sin(angleH), 0);

				var bN = centerL.subtract(centerH).normalize();
				builder.vertex(centerL.x, centerL.y, centerL.z).color(0, 1, 0, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();
				builder.vertex(centerH.x, centerH.y, centerH.z).color(0, 1, 0, 0.2f)
						.normal((float) bN.x, (float) bN.y, (float) bN.z).endVertex();

			}
			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		}

		if (!(node instanceof BinaryNode)) {
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			var qll = centerPos.subtract(0, d+1, 0).subtract(d+1, 0, 0);
			var qlh = centerPos.subtract(0, d+1, 0).add(d, 0, 0);
			var qhl = centerPos.add(0, d, 0).subtract(d+1, 0, 0);
			var qhh = centerPos.add(0, d, 0).add(d, 0, 0);
			builder.vertex(qhl.x, qhl.y, 0).color(r, g, b, a)/* .uv(1, 0) */.endVertex();
			builder.vertex(qll.x, qll.y, 0).color(r, g, b, a)/* .uv(0, 0) */.endVertex();
			builder.vertex(qlh.x, qlh.y, 0).color(r, g, b, a)/* .uv(0, 1) */.endVertex();
			builder.vertex(qhh.x, qhh.y, 0).color(r, g, b, a)/* .uv(1, 1) */.endVertex();
			builder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.depthMask(false);
			BufferUploader.end(builder);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		}
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		RenderSystem.depthMask(false);
		renderBackground(poseStack);
		RenderSystem.depthMask(true);

		var partialTick = this.client.getFrameTime();

		if (system != null) {
			poseStack.pushPose();
			renderNode(system.rootNode, partialTick, Vec3.ZERO.add(this.width / 2, this.height / 2, 0));
			new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
			poseStack.popPose();
		}

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
