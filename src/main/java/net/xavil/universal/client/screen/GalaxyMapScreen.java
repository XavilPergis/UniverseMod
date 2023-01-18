package net.xavil.universal.client.screen;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.CelestialNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class GalaxyMapScreen extends Screen {

	private final Minecraft client = Minecraft.getInstance();
	public final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");

	public GalaxyMapScreen() {
		super(new TranslatableComponent("narrator.screen.starmap"));

		this.universe = MinecraftClientAccessor.getUniverse(this.client);

		var galaxyVolume = this.universe.getOrGenerateGalaxyVolume(this.universe.getStartingId().sectorPos());
		var startingGalaxy = galaxyVolume.fullById(this.universe.getStartingId().sectorId());

		this.galaxy = startingGalaxy;
		this.volumePos = Vec3i.ZERO;

		var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
		this.camera.focusPos = this.camera.focusPosOld = this.camera.focusPosTarget = volume
				.offsetById(this.currentSystemId);

		this.camera.pitch = this.camera.pitchOld = this.camera.pitchTarget = Math.PI / 8;
		this.camera.yaw = this.camera.yawOld = this.camera.yawTarget = Math.PI / 8;
		this.camera.scaleTarget = 8;
	}

	private ClientUniverse universe;
	private Galaxy galaxy;
	private Vec3i volumePos;

	private StarmapCamera camera = new StarmapCamera();
	private int currentSystemId = 0;

	private double starRenderRadius = 0.9 * Galaxy.TM_PER_SECTOR;

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy))
			return true;

		final var dragScale = TM_PER_UNIT * this.camera.scaleTarget * 10 / Units.TM_PER_LY;

		if (button == 2) {
			var realDy = this.camera.pitch < 0 ? -dy : dy;
			var offset = new Vec3(dx, 0, realDy).yRot((float) -this.camera.yaw).scale(dragScale);
			this.camera.focusPosTarget = this.camera.focusPosTarget.add(offset);
		} else if (button == 1) {
			this.camera.focusPosTarget = this.camera.focusPosTarget.add(0, dragScale * dy, 0);
		} else if (button == 0) {
			this.camera.yawTarget += dx * 0.005;
			this.camera.pitchTarget += dy * 0.005;
			if (this.camera.pitchTarget > Math.PI / 2)
				this.camera.pitchTarget = Math.PI / 2;
			if (this.camera.pitchTarget < -Math.PI / 2)
				this.camera.pitchTarget = -Math.PI / 2;
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDelta))
			return true;

		if (scrollDelta > 0) {
			this.camera.scaleTarget /= 1.2;
			this.camera.scaleTarget = Math.max(this.camera.scaleTarget, 0.5);
			return true;
		} else if (scrollDelta < 0) {
			this.camera.scaleTarget *= 1.2;
			this.camera.scaleTarget = Math.min(this.camera.scaleTarget, 192.0);
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (super.charTyped(c, i))
			return true;
		if (c == 'e') {
			var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
			var system = volume.fullById(this.currentSystemId);
			var screen = new SystemMapScreen(this, system);
			this.client.setScreen(screen);
		} else if (c == 'r') {
			var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
			this.currentSystemId += 1;
			if (this.currentSystemId >= volume.size()) {
				this.currentSystemId = 0;
			}

			this.camera.focusPosTarget = volume.offsetById(this.currentSystemId);
		}
		return false;
	}

	private static class StarmapCamera {

		public Vec3 focusPosTarget = Vec3.ZERO;
		public Vec3 focusPos = Vec3.ZERO;
		public Vec3 focusPosOld = Vec3.ZERO;
		public double yawTarget = 0;
		public double yaw = 0;
		public double yawOld = 0;
		public double pitchTarget = 0;
		public double pitch = 0;
		public double pitchOld = 0;
		public double scaleTarget = 1;
		public double scale = 1;
		public double scaleOld = 1;

		// projection properties
		public double fovDeg = 90;
		public double nearPlane = 0.05;
		public double farPlane = 10000;

		private final Minecraft client = Minecraft.getInstance();

		public void tick() {
			this.focusPosOld = this.focusPos;
			this.yawOld = this.yaw;
			this.pitchOld = this.pitch;
			this.scaleOld = this.scale;

			var newFocusX = Mth.lerp(0.6, this.focusPos.x, this.focusPosTarget.x);
			var newFocusY = Mth.lerp(0.6, this.focusPos.y, this.focusPosTarget.y);
			var newFocusZ = Mth.lerp(0.6, this.focusPos.z, this.focusPosTarget.z);
			this.focusPos = new Vec3(newFocusX, newFocusY, newFocusZ);

			this.yaw = Mth.lerp(0.6, this.yaw, this.yawTarget);
			this.pitch = Mth.lerp(0.6, this.pitch, this.pitchTarget);
			this.scale = Mth.lerp(0.2, this.scale, this.scaleTarget);
		}

		public Vec3 getFocusPos(float partialTick) {
			var x = Mth.lerp(partialTick, this.focusPosOld.x, this.focusPos.x);
			var y = Mth.lerp(partialTick, this.focusPosOld.y, this.focusPos.y);
			var z = Mth.lerp(partialTick, this.focusPosOld.z, this.focusPos.z);
			return new Vec3(x, y, z);
		}

		public double getYaw(float partialTick) {
			return Mth.lerp(partialTick, this.yawOld, this.yaw);
		}

		public double getPitch(float partialTick) {
			return Mth.lerp(partialTick, this.pitchOld, this.pitch);
		}

		public double getScale(float partialTick) {
			return Mth.lerp(partialTick, this.scaleOld, this.scale);
		}

		public Vec3 getUpVector(float partialTick) {
			return new Vec3(0, 1, 0).xRot((float) -getPitch(partialTick)).yRot((float) -getYaw(partialTick));
		}

		public Vec3 getRightVector(float partialTick) {
			return new Vec3(1, 0, 0).xRot((float) -getPitch(partialTick)).yRot((float) -getYaw(partialTick));
		}

		public Matrix4f getProjectionMatrix() {
			var window = this.client.getWindow();
			var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
			return Matrix4f.perspective((float) this.fovDeg, aspectRatio, (float) this.nearPlane,
					(float) this.farPlane);
		}

		static Quaternion qmul(Quaternion a, Quaternion b) {
			var res = new Quaternion(a);
			res.mul(b);
			return res;
		}

		public Quaternion getOrientation(float partialTick) {
			var xRot = Vector3f.XP.rotation((float) getPitch(partialTick));
			var yRot = Vector3f.YP.rotation((float) (getYaw(partialTick) + Math.PI));
			return qmul(xRot, yRot);
		}
	}

	public static final double TM_PER_UNIT = 1000;
	public static final double UNITS_PER_SECTOR = Galaxy.TM_PER_SECTOR / TM_PER_UNIT;

	private void renderGrid(float partialTick) {

		var oldInvViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(this.camera.getProjectionMatrix());

		var rotation = this.camera.getOrientation(partialTick);

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

		modelViewStack.mulPose(rotation);
		var forward = camera.getUpVector(partialTick).cross(camera.getRightVector(partialTick));
		var aaa = this.camera.getFocusPos(partialTick).scale(1 / TM_PER_UNIT)
				.add(forward.scale(this.camera.getScale(partialTick)));
		modelViewStack.translate(-aaa.x, -aaa.y, -aaa.z);

		// setup

		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		// RenderSystem.defaultBlendFunc();
		RenderSystem.lineWidth(1);

		PoseStack aaaaaaaa = RenderSystem.getModelViewStack();
		aaaaaaaa.pushPose();
		aaaaaaaa.scale(0.99975586f, 0.99975586f, 0.99975586f);
		RenderSystem.applyModelViewMatrix();

		// Grid

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		bufferBuilder.begin(VertexFormat.Mode.LINES,
				DefaultVertexFormat.POSITION_COLOR_NORMAL);
		renderGrid(bufferBuilder, modelViewStack, partialTick);
		RenderSystem.depthMask(false);
		bufferBuilder.end();
		RenderSystem.applyModelViewMatrix();
		BufferUploader.end(bufferBuilder);
		RenderSystem.depthMask(true);

		// cleanup

		// PoseStack aaaaaaaa = RenderSystem.getModelViewStack();
		aaaaaaaa.popPose();
		RenderSystem.applyModelViewMatrix();

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		// RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();
		RenderSystem.setInverseViewRotationMatrix(oldInvViewRotationMatrix);
	}

	private void renderGrid(VertexConsumer builder, PoseStack poseStack, float partialTick) {

		double l = 0;
		double h = UNITS_PER_SECTOR;

		var gridLineCount = 100;
		var scale = 1;

		if (this.camera.scale > 10)
			scale = 10;
		if (this.camera.scale > 100)
			scale = 100;

		var gridResolution = scale * h / gridLineCount;
		var focusPos = this.camera.getFocusPos(partialTick).scale(1 / TM_PER_UNIT);

		var gridMinX = gridResolution * Math.floor(focusPos.x / gridResolution);
		var gridMinZ = gridResolution * Math.floor(focusPos.z / gridResolution);

		float r = 1, g = 1, b = 1, a = 0.05f;
		var p = poseStack.last().normal();

		var gridOffset = gridResolution * gridLineCount / 2;

		// NOTE: each line needs to be divided into sections, because the lines will
		// become distorted if they are too long.
		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridResolution - gridOffset;
			var lx = gridMinX + scale * l - gridOffset;
			var hx = gridMinX + scale * h - gridOffset;

			var zMark = (int) Math.floor(gridMinZ / gridResolution + i - gridLineCount / 2);
			var la = zMark % 10 == 0 ? 0.2f : a;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				builder.vertex(Mth.lerp(lt, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
				builder.vertex(Mth.lerp(ht, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
			}
		}
		// Z
		for (var i = 1; i < gridLineCount; ++i) {
			var x = gridMinX + i * gridResolution - gridOffset;
			var lz = gridMinZ + scale * l - gridOffset;
			var hz = gridMinZ + scale * h - gridOffset;

			var xMark = (int) Math.floor(gridMinX / gridResolution + i - gridLineCount / 2);
			var la = xMark % 10 == 0 ? 0.2f : a;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				builder.vertex(x, focusPos.y, Mth.lerp(lt, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
				builder.vertex(x, focusPos.y, Mth.lerp(ht, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
			}
		}

	}

	private void renderSectorBox(VertexConsumer builder, PoseStack poseStack, Vec3i sectorPos) {
		double lx = (double) sectorPos.getX() * UNITS_PER_SECTOR;
		double ly = (double) sectorPos.getY() * UNITS_PER_SECTOR;
		double lz = (double) sectorPos.getZ() * UNITS_PER_SECTOR;
		double hx = lx + UNITS_PER_SECTOR;
		double hy = ly + UNITS_PER_SECTOR;
		double hz = lz + UNITS_PER_SECTOR;

		float r = 1, g = 1, b = 1, a = 0.2f;

		var p = poseStack.last().normal();

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

	private static record StarInfo(Vec3 pos, Vec3 color) {
	}

	private void renderSectorStars(VertexConsumer builder, Vec3i sectorPos, Stream<StarInfo> sectorOffsets,
			float partialTick) {

		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		var upHalf = up.scale(0.5).scale(2);
		var rightHalf = right.scale(0.5).scale(2);
		var focusPos = this.camera.getFocusPos(partialTick);

		sectorOffsets.forEach(info -> {
			// var absolutePos =
			// info.pos.add(Vec3.atLowerCornerOf(sectorPos).scale(Galaxy.TM_PER_SECTOR /
			// TM_PER_UNIT));
			var distanceFromFocus = focusPos.distanceTo(info.pos);
			var alphaFactor = 1 - Mth.clamp(distanceFromFocus / this.starRenderRadius, 0, 1);

			if (alphaFactor == 0)
				return;

			var center = info.pos.scale(1 / TM_PER_UNIT);

			var qll = center.subtract(upHalf).subtract(rightHalf);
			var qlh = center.subtract(upHalf).add(rightHalf);
			var qhl = center.add(upHalf).subtract(rightHalf);
			var qhh = center.add(upHalf).add(rightHalf);

			float r = (float) info.color.x, g = (float) info.color.y, b = (float) info.color.z;
			float a = (float) alphaFactor;

			builder.vertex(qhl.x, qhl.y, qhl.z).color(r, g, b, a).uv(1, 0).endVertex();
			builder.vertex(qll.x, qll.y, qll.z).color(r, g, b, a).uv(0, 0).endVertex();
			builder.vertex(qlh.x, qlh.y, qlh.z).color(r, g, b, a).uv(0, 1).endVertex();
			builder.vertex(qhh.x, qhh.y, qhh.z).color(r, g, b, a).uv(1, 1).endVertex();
		});

	}

	private @Nullable CelestialNode.StellarBodyNode getBrightestStar(CelestialNode node) {
		if (node instanceof CelestialNode.BinaryNode binaryNode) {
			CelestialNode.StellarBodyNode a = getBrightestStar(binaryNode.a);
			CelestialNode.StellarBodyNode b = getBrightestStar(binaryNode.b);
			if (a == null)
				return b;
			if (b == null)
				return a;
			return a.luminosityLsol > b.luminosityLsol ? a : b;
		} else if (node instanceof CelestialNode.StellarBodyNode starNode) {
			return starNode;
		} else {
			return null;
		}
	}

	private void renderStars(LodVolume<StarSystem.Info, StarSystem> volume, float partialTick) {

		var oldInvViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(this.camera.getProjectionMatrix());

		var rotation = this.camera.getOrientation(partialTick);

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

		modelViewStack.mulPose(rotation);
		var forward = camera.getUpVector(partialTick).cross(camera.getRightVector(partialTick));
		var aaa = this.camera.getFocusPos(partialTick).scale(1 / TM_PER_UNIT)
				.add(forward.scale(this.camera.getScale(partialTick)));
		modelViewStack.translate(-aaa.x, -aaa.y, -aaa.z);

		// modelViewStack.translate(
		// volume.position.getX() * Galaxy.TM_PER_SECTOR / TM_PER_UNIT,
		// volume.position.getY() * Galaxy.TM_PER_SECTOR / TM_PER_UNIT,
		// volume.position.getZ() * Galaxy.TM_PER_SECTOR / TM_PER_UNIT);

		// rotate then translate rotates everything about the origin, then translates
		// translate then rotate translates everything away from the origin and then
		// rotates everything around it.

		// the last operation on a poseStack is the operation that happens FIRST!
		// so mulPose(); translate(); actually translates and then rotates.

		// setup

		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.defaultBlendFunc();

		// Stars

		// RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		renderSectorStars(bufferBuilder, volume.position, volume.streamIds().mapToObj(id -> {
			var node = getBrightestStar(volume.fullById(id).rootNode);
			var color = node.starClass().color;
			var pos = volume.offsetById(id).add(volume.getBasePos());
			return new StarInfo(pos, color);
		}), partialTick);

		// renderSectorStars(bufferBuilder, Vec3i.ZERO,
		// Stream.of(this.camera.getFocusPos(partialTick).scale(TM_PER_UNIT)),
		// partialTick);

		var textureManager = Minecraft.getInstance().getTextureManager();
		textureManager.getTexture(STAR_ICON_LOCATION).setFilter(true, false); // filtered, mipped
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		bufferBuilder.setQuadSortOrigin((float) aaa.x, (float) aaa.y, (float) aaa.z);
		bufferBuilder.end();
		RenderSystem.applyModelViewMatrix();
		BufferUploader.end(bufferBuilder);

		// Grid

		// RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		// bufferBuilder.begin(VertexFormat.Mode.LINES,
		// DefaultVertexFormat.POSITION_COLOR_NORMAL);
		// renderSectorBox(bufferBuilder, modelViewStack, volume.position);
		// bufferBuilder.end();
		// RenderSystem.applyModelViewMatrix();
		// BufferUploader.end(bufferBuilder);

		// RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		// bufferBuilder.begin(VertexFormat.Mode.LINES,
		// DefaultVertexFormat.POSITION_COLOR_NORMAL);
		// renderGrid(bufferBuilder, modelViewStack, partialTick);
		// RenderSystem.depthMask(false);
		// bufferBuilder.end();
		// RenderSystem.applyModelViewMatrix();
		// BufferUploader.end(bufferBuilder);
		// RenderSystem.depthMask(true);

		// cleanup

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();
		RenderSystem.setInverseViewRotationMatrix(oldInvViewRotationMatrix);
	}

	@Override
	public void tick() {
		super.tick();
		this.camera.tick();
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		final var partialTick = this.client.getFrameTime();

		// TODO: render distant galaxies or something as a backdrop so its not just
		// pitch black. or maybe thats just how space is :p

		// TODO: figure out how to render the shape of the galaxy. Might have to rethink
		// how i do the density field stuff. Maybe have like a list of parts that are
		// explicitly assembled, instead of chaining together interfaces.

		RenderSystem.depthMask(false);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		renderGrid(partialTick);

		var focusPos = this.camera.getFocusPos(partialTick);
		var focusedSectorX = (int) Math.floor(focusPos.x / Galaxy.TM_PER_SECTOR);
		var focusedSectorY = (int) Math.floor(focusPos.y / Galaxy.TM_PER_SECTOR);
		var focusedSectorZ = (int) Math.floor(focusPos.z / Galaxy.TM_PER_SECTOR);
		var focusedSector = new Vec3i(focusedSectorX, focusedSectorY, focusedSectorZ);

		var renderRadiusSectors = starRenderRadius / Galaxy.TM_PER_SECTOR;
		int sectorDistance = (int) Math.ceil(renderRadiusSectors);

		for (int x = -sectorDistance; x <= sectorDistance; ++x) {
			for (int y = -sectorDistance; y <= sectorDistance; ++y) {
				for (int z = -sectorDistance; z <= sectorDistance; ++z) {
					var sectorPos = focusedSector.offset(x, y, z);
					var volume = this.galaxy.getOrGenerateVolume(sectorPos);
					renderStars(volume, partialTick);
				}
			}
		}

		// TODO: mouse picking

		int h = 0;
		drawString(poseStack, this.client.font, "focused " + focusedSector.toShortString(), 0, h, 0xffffffff);
		h += 9;

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
