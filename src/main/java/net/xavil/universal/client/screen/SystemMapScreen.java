package net.xavil.universal.client.screen;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarNode;

public class SystemMapScreen extends Screen {

	private final Minecraft client = Minecraft.getInstance();
	private final StarSystem system;
	private final @Nullable Screen previousScreen;

	// private abstract class Selectable {
	// 	public double x, y;

	// 	protected Selectable(double x, double y) {}
	// }

	// private final List<Selectable> selectables = new ArrayList<>();

	protected SystemMapScreen(@Nullable Screen previousScreen, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"));
		this.system = system;
		this.previousScreen = previousScreen;

		// var maxId = this.system.rootNode.getId();

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
			} else if (node instanceof StarNode starNode) {
				var height = 3 * this.client.font.lineHeight;
				var str = "";
				if (starNode.starClass() != null) {
					str += "[Class " + starNode.starClass().name + "] ";
				}
				str += node.toString();
				drawString(poseStack, this.client.font, str, xOff + 7, yOff + this.client.font.lineHeight,
						0xffffffff);
				return new SegmentInfo(height, 0, height);
			}

			return new SegmentInfo(0, 0, 0);
		}
	}

	@Override
	public void onClose() {
		// NOTE: explicitly not calling super's onClose because we want to set the
		// screen to the previous scrren instead of always setting it to null.
		this.client.setScreen(previousScreen);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.depthMask(false);
		renderBackground(poseStack);
		RenderSystem.depthMask(true);

		if (system != null) {
			poseStack.pushPose();
			// drawString(poseStack, this.client.font, this.volumePos.toString(), 0, 0,
			// 0xffffffff);
			new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
			poseStack.popPose();
		}

		super.render(poseStack, mouseX, mouseY, partialTick);
	}

}
