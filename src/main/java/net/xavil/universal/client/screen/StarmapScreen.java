package net.xavil.universal.client.screen;

import java.util.Random;
import java.util.stream.Stream;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.CelestialNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class StarmapScreen extends Screen {

	private final Minecraft client = Minecraft.getInstance();

	public StarmapScreen() {
		super(new TranslatableComponent("narrator.screen.starmap"));

		this.random = new Random();

		this.universe = MinecraftClientAccessor.getUniverse(this.client);

		var galaxyVolume = this.universe.getOrGenerateGalaxyVolume(this.universe.getStartingId().sectorPos());
		var startingGalaxy = galaxyVolume.fullById(this.universe.getStartingId().sectorId());

		this.galaxy = startingGalaxy;
		this.volumePos = Vec3i.ZERO;

		var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
		this.camera.focusPos = volume.offsetById(this.currentSystemId).scale(1 / 1000f);
	}

	private Random random;
	private ClientUniverse universe;
	private Galaxy galaxy;
	private Vec3i volumePos;
	// private StarSystem starSystem;

	static class NodeRenderer {
		private final Minecraft client = Minecraft.getInstance();
		// private final CelestialNode node;
		private final PoseStack poseStack;

		// private int maxDepth = 0;

		public NodeRenderer(PoseStack poseStack) {
			// this.node = node;
			this.poseStack = poseStack;
			// this.maxDepth = computeMaxDepth(node, 0);
		}

		// record RenderPosition(int yOffset, int height) {}

		private static int computeMaxDepth(CelestialNode node, int depth) {
			var maxDepth = 0;
			if (node instanceof CelestialNode.BinaryNode binaryNode) {
				maxDepth = Math.max(maxDepth, depth);
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.a, depth + 1));
				maxDepth = Math.max(maxDepth, computeMaxDepth(binaryNode.b, depth + 1));
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
			if (node instanceof CelestialNode.BinaryNode binaryNode) {
				var aInfo = renderNode(binaryNode.a, depth + 1, xOff, yOff);
				var bInfo = renderNode(binaryNode.b, depth + 1, xOff, yOff + aInfo.height);

				var lineStartY = aInfo.center();
				var lineEndY = aInfo.height + bInfo.center();

				var vertialX = 5 * depth;
				fill(poseStack, vertialX, yOff + lineStartY, vertialX + 1, yOff + lineEndY + 1, 0x77ffffff);

				var aEndX = binaryNode.a instanceof CelestialNode.BinaryNode ? vertialX : xOff;
				var bEndX = binaryNode.b instanceof CelestialNode.BinaryNode ? vertialX : xOff;

				fill(poseStack, vertialX + 1, yOff + lineStartY, aEndX + 5, yOff + lineStartY + 1, 0x77ffffff);
				fill(poseStack, vertialX + 1, yOff + lineEndY, bEndX + 5, yOff + lineEndY + 1, 0x77ffffff);

				return new SegmentInfo(aInfo.height + bInfo.height, lineStartY, lineEndY);
			} else if (node instanceof CelestialNode.StellarBodyNode starNode) {
				var height = 3 * this.client.font.lineHeight;
				var str = "[Class " + starNode.starClass().name + "] " + node.toString();
				drawString(poseStack, this.client.font, str, xOff + 7, yOff + this.client.font.lineHeight,
						0xffffffff);
				return new SegmentInfo(height, 0, height);
			}

			return new SegmentInfo(0, 0, 0);
		}
	}

	// scale of 1 means 1 world unit is 0.1 ly
	// private double scale = 1;

	private StarmapCamera camera = new StarmapCamera();

	private int currentSystemId = 0;

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy))
			return true;

		final var dragScale = this.camera.scale * 10 / Units.TM_PER_LY;

		if (button == 2) {
			var realDy = this.camera.pitch < 0 ? -dy : dy;
			var offset = new Vec3(dx, 0, realDy).yRot((float) -this.camera.yaw).scale(dragScale);
			this.camera.focusPos = this.camera.focusPos.add(offset);
		} else if (button == 1) {
			this.camera.focusPos = this.camera.focusPos.add(0, dragScale * dy, 0);
		} else if (button == 0) {
			this.camera.yaw += dx * 0.005;
			this.camera.pitch += dy * 0.005;
			if (this.camera.pitch > Math.PI / 2)
				this.camera.pitch = Math.PI / 2;
			if (this.camera.pitch < -Math.PI / 2)
				this.camera.pitch = -Math.PI / 2;
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDelta))
			return true;

		if (scrollDelta > 0) {
			this.camera.scale /= 1.5;
			this.camera.scale = Math.max(this.camera.scale, 0.5);
			return true;
		} else if (scrollDelta < 0) {
			this.camera.scale *= 1.5;
			this.camera.scale = Math.min(this.camera.scale, 192.0);
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (super.charTyped(c, i))
			return true;
		if (c == 'r') {
			var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
			this.currentSystemId += 1;
			if (this.currentSystemId >= volume.size()) {
				this.currentSystemId = 0;
			}

			this.camera.focusPos = volume.offsetById(this.currentSystemId).scale(1 / 1000f);
		}
		return false;
	}

	private static class StarmapCamera {
		public Vec3 focusPos = Vec3.ZERO;
		public double yaw = 0;
		public double pitch = 0;
		public double scale = 1;

		// TODO: move camera outwards from focus
		public double cameraDistance = 3;

		public double fovDeg = 90;
		public double nearPlane = 0.05;
		public double farPlane = 1000;

		private final Minecraft client = Minecraft.getInstance();

		public Vec3 getUpVector() {
			return new Vec3(0, 1, 0).xRot((float) -this.pitch).yRot((float) -this.yaw);
		}

		public Vec3 getRightVector() {
			return new Vec3(1, 0, 0).xRot((float) -this.pitch).yRot((float) -this.yaw);
		}

		public Matrix4f getProjectionMatrix() {
			var window = this.client.getWindow();
			// var aspectRatio = window.getWidth() / window.getHeight();
			return Matrix4f.perspective((float) this.fovDeg,
					(float) window.getWidth() / (float) window.getHeight(), (float) this.nearPlane,
					(float) 100000);
		}

		public Matrix4f getViewMatrix() {
			var window = this.client.getWindow();
			// var aspectRatio = window.getWidth() / window.getHeight();
			return Matrix4f.perspective((float) this.fovDeg,
					(float) window.getWidth() / (float) window.getHeight(), (float) this.nearPlane,
					(float) 100000);
		}

		static Quaternion qmul(Quaternion a, Quaternion b) {
			var res = new Quaternion(a);
			res.mul(b);
			return res;
		}

		public Quaternion getOrientation() {
			var xRot = Vector3f.XP.rotation((float) this.pitch);
			var yRot = Vector3f.YP.rotation((float) (this.yaw + Math.PI));

			// _quat *= q_right;
			// _quat = q_up * _quat;

			// var res = ;
			var res = qmul(xRot, yRot);

			return res;
			// return
			// glm::quat q = glm::angleAxis(glm::radians(-_upAngle), glm::vec3(1,0,0));
			// q*= glm::angleAxis(glm::radians(_rightAngle), glm::vec3(0,1,0));
			// return glm::mat4_cast(q);
		}
	}

	public static final double TM_PER_UNIT = 1000;
	public static final double UNITS_PER_SECTOR = Galaxy.TM_PER_SECTOR / TM_PER_UNIT;

	private void renderGrid(MultiBufferSource buffers) {
		renderGrid(buffers.getBuffer(RenderType.lines()));
	}

	private void renderGrid(VertexConsumer builder) {

		double l = 0;
		double h = UNITS_PER_SECTOR;

		var gridLineCount = 32;
		var scale = 1;
		var gridResolution = scale * h / gridLineCount;

		// focusPos.z == 1r -> 1r
		// focusPos.z == 2r -> 2r
		var gridMinX = gridResolution * Math.floor(this.camera.focusPos.x / gridResolution);
		var gridMinZ = gridResolution * Math.floor(this.camera.focusPos.z / gridResolution);

		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridResolution - (gridResolution * gridLineCount / 2);
			var lx = gridMinX + scale * l - (gridResolution * gridLineCount / 2);
			var hx = gridMinX + scale * h - (gridResolution * gridLineCount / 2);
			builder.vertex(lx, this.camera.focusPos.y, z).color(1, 1, 1, 0.2f).normal(1, 0, 0).endVertex();
			builder.vertex(hx, this.camera.focusPos.y, z).color(1, 1, 1, 0.2f).normal(1, 0, 0).endVertex();
		}
		// Z
		for (var i = 1; i < gridLineCount; ++i) {
			var x = gridMinX + i * gridResolution - (gridResolution * gridLineCount / 2);
			var lz = gridMinZ + scale * l - (gridResolution * gridLineCount / 2);
			var hz = gridMinZ + scale * h - (gridResolution * gridLineCount / 2);
			builder.vertex(x, this.camera.focusPos.y, lz).color(1, 1, 1, 0.2f).normal(0, 0, 1).endVertex();
			builder.vertex(x, this.camera.focusPos.y, hz).color(1, 1, 1, 0.2f).normal(0, 0, 1).endVertex();
		}

	}

	private void renderSectorBox(MultiBufferSource buffers, Vec3i sectorPos) {
		renderSectorBox(buffers.getBuffer(RenderType.lines()), sectorPos);
	}

	private void renderSectorBox(VertexConsumer builder, Vec3i sectorPos) {
		double lx = (double) sectorPos.getX() * UNITS_PER_SECTOR;
		double ly = (double) sectorPos.getY() * UNITS_PER_SECTOR;
		double lz = (double) sectorPos.getZ() * UNITS_PER_SECTOR;
		double hx = lx + UNITS_PER_SECTOR;
		double hy = ly + UNITS_PER_SECTOR;
		double hz = lz + UNITS_PER_SECTOR;

		float r = 1, g = 1, b = 1, a = 1;

		// X axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		// Y axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		// Z axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
	}

	private void renderSectorStars(VertexConsumer builder, Vec3i sectorPos, Stream<Vec3> sectorOffsets) {

		var up = camera.getUpVector();
		var right = camera.getRightVector();
		var upHalf = up.scale(0.5).scale(0.5);
		var rightHalf = right.scale(0.5).scale(0.5);

		sectorOffsets.forEach(pos -> {
			var distanceFromFocus = this.camera.focusPos.scale(TM_PER_UNIT).distanceTo(pos);
			var alphaFactor = 1 - Mth.clamp(distanceFromFocus / (0.5f * Galaxy.TM_PER_SECTOR), 0, 1);

			if (alphaFactor == 0)
				return;

			var center = pos.scale(1 / TM_PER_UNIT);

			var qll = center.subtract(upHalf).subtract(rightHalf);
			var qlh = center.subtract(upHalf).add(rightHalf);
			var qhl = center.add(upHalf).subtract(rightHalf);
			var qhh = center.add(upHalf).add(rightHalf);

			float r = 1.0f;
			float g = 0.2f;
			float b = 0.3f;
			float a = (float) alphaFactor;

			builder.vertex(qhl.x, qhl.y, qhl.z).color(r, g, b, a).endVertex();
			builder.vertex(qll.x, qll.y, qll.z).color(r, g, b, a).endVertex();
			builder.vertex(qlh.x, qlh.y, qlh.z).color(r, g, b, a).endVertex();
			builder.vertex(qhh.x, qhh.y, qhh.z).color(r, g, b, a).endVertex();
		});

	}

	private void renderStars(LodVolume<StarSystem.Info, StarSystem> volume) {

		var oldInvViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();

		var rotation = this.camera.getOrientation();

		var modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		modelViewStack.setIdentity();

		modelViewStack.pushPose();

		modelViewStack.mulPose(rotation);
		Matrix3f inverseViewRotationMatrix = modelViewStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}
		modelViewStack.popPose();

		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(this.camera.getProjectionMatrix());

		modelViewStack.mulPose(rotation);
		var forward = camera.getUpVector().cross(camera.getRightVector());
		var aaa = this.camera.focusPos.add(forward.scale(this.camera.scale));
		modelViewStack.translate(-aaa.x, -aaa.y, -aaa.z);

		// rotate then translate rotates everything about the origin, then translates
		// translate then rotate translates everything away from the origin and then
		// rotates everything around it.

		// the last operation on a poseStack is the operation that happens FIRST!
		// so mulPose(); translate(); actually translates and then rotates.

		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.defaultBlendFunc();

		// Grid

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		renderSectorBox(bufferBuilder, volume.position);
		renderGrid(bufferBuilder);

		bufferBuilder.end();
		RenderSystem.applyModelViewMatrix();
		BufferUploader.end(bufferBuilder);

		// Stars

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		renderSectorStars(bufferBuilder, volume.position, volume.streamIds().mapToObj(volume::offsetById));
		renderSectorStars(bufferBuilder, Vec3i.ZERO, Stream.of(this.camera.focusPos.scale(TM_PER_UNIT)));

		bufferBuilder.setQuadSortOrigin((float) aaa.x, (float) aaa.y, (float) aaa.z);
		bufferBuilder.end();

		RenderSystem.applyModelViewMatrix();

		BufferUploader.end(bufferBuilder);

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();
		RenderSystem.setInverseViewRotationMatrix(oldInvViewRotationMatrix);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float f) {
		// current system coordinates

		var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
		var system = volume.fullById(this.currentSystemId);

		RenderSystem.depthMask(false);
		renderBackground(poseStack);
		RenderSystem.depthMask(true);

		renderStars(volume);

		if (system != null) {
			poseStack.pushPose();
			// poseStack.translate(0, 30, 0);
			// poseStack.mulPose(Vector3f.ZN.rotationDegrees(mouseX));
			// poseStack.translate(0, -30, 0);
			drawString(poseStack, this.client.font, this.volumePos.toString(), 0, 0, 0xffffffff);
			// poseStack.translate(this.dragOffsetX, this.dragOffsetY, 0);
			new NodeRenderer(poseStack).renderNodeMain(system.rootNode);
			poseStack.popPose();
		}

		super.render(poseStack, mouseX, mouseY, f);
	}

}
