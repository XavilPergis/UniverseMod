package net.xavil.universal.client.screen;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class GalaxyMapScreen extends Screen {

	private final Minecraft client = Minecraft.getInstance();
	public final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");

	public GalaxyMapScreen() {
		super(new TranslatableComponent("narrator.screen.starmap"));

		this.universe = MinecraftClientAccessor.getUniverse(this.client);

		var galaxyVolume = this.universe.getOrGenerateGalaxyVolume(this.universe.getStartingGalaxyId().sectorPos());
		var startingGalaxy = galaxyVolume.getById(this.universe.getStartingGalaxyId().sectorId()).getFull();
		this.galaxy = startingGalaxy;

		var volumePos = this.universe.getStartingSystemId().sectorPos();
		this.currentSystemId = this.universe.getStartingSystemId();

		var volume = this.galaxy.getOrGenerateVolume(volumePos);
		this.camera.focusPos = this.camera.focusPosOld = this.camera.focusPosTarget = volume
				.posById(this.currentSystemId.sectorId());

		System.out.println("focus pos: " + this.camera.focusPos);

		this.camera.pitch = this.camera.pitchOld = this.camera.pitchTarget = Math.PI / 8;
		this.camera.yaw = this.camera.yawOld = this.camera.yawTarget = Math.PI / 8;
		this.camera.scaleTarget = 8;
	}

	private ClientUniverse universe;
	private Galaxy galaxy;
	// private Vec3i volumePos;

	private StarmapCamera camera = new StarmapCamera();
	private UniverseId.SectorId currentSystemId;

	public static final double STAR_RENDER_RADIUS = 0.5 * Galaxy.TM_PER_SECTOR;
	public static final double TM_PER_UNIT = 1000;
	public static final double UNITS_PER_SECTOR = Galaxy.TM_PER_SECTOR / TM_PER_UNIT;

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
			// var volumePos = this.universe.getStartingSystemId().sectorPos();
			// this.currentSystemId = this.universe.getStartingSystemId().sectorId();

			var volume = this.galaxy.getOrGenerateVolume(this.currentSystemId.sectorPos());
			var system = volume.getById(this.currentSystemId.sectorId()).getFull();
			var screen = new SystemMapScreen(this, system);
			this.client.setScreen(screen);
			// } else if (c == 'r') {
			// var volume = this.galaxy.getOrGenerateVolume(this.volumePos);
			// this.currentSystemId += 1;
			// if (this.currentSystemId >= volume.elementCount()) {
			// this.currentSystemId = 0;
			// }

			// this.camera.focusPosTarget = volume.posById(this.currentSystemId);
		} else if (c == 'f') {
			var partialTick = this.client.getFrameTime();
			Mod.LOGGER.warn("before: " + this.currentSystemId);
			var focusPos = this.camera.getFocusPos(partialTick);

			var nearest = new Object() {
				double distanceSqr = Double.MAX_VALUE;
				UniverseId.SectorId id = null;
			};
			enumerateSectors(focusPos, 10000, sectorPos -> {
				var volume = this.galaxy.getOrGenerateVolume(sectorPos);
				var nearestInSector = volume.nearestInRadius(this.camera.focusPos, 10000);
				if (nearestInSector != null) {
					if (nearestInSector.pos().distanceToSqr(focusPos) < nearest.distanceSqr) {
						nearest.distanceSqr = nearestInSector.pos().distanceToSqr(focusPos);
						nearest.id = new UniverseId.SectorId(sectorPos, nearestInSector.id());
					}
				}
			});

			if (nearest.id != null) {
				var volume = this.galaxy.getOrGenerateVolume(nearest.id.sectorPos());
				// var nearestPos = volume.posById(nearest.id).scale(1 / TM_PER_UNIT);
				this.currentSystemId = nearest.id;
				Mod.LOGGER.warn("after: " + this.currentSystemId);
				this.camera.focusPosTarget = volume.posById(this.currentSystemId.sectorId());
			}

		}
		return false;
	}

	private static class StarmapCamera {

		// i kinda hate this!
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
			// this works well enough, and gets us framerate independence, but it feels
			// slightly laggy to use, since we're always a tick behind the current target.
			// would be nice if there was a better way to do this.
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

		public Vec3 getPos(float partialTick) {
			var backwards = getUpVector(partialTick).cross(getRightVector(partialTick));
			var backwardsTranslation = backwards.scale(getScale(partialTick));
			var cameraPos = getFocusPos(partialTick).scale(1 / TM_PER_UNIT).add(backwardsTranslation);
			return cameraPos;
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

	private void renderGrid(float partialTick) {
		var prevMatrices = RenderMatrices.capture();
		setupRenderMatrices(partialTick);

		// setup

		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);

		// Grid

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		renderGrid(bufferBuilder, partialTick);

		var focusPos = this.camera.getFocusPos(partialTick);
		enumerateSectors(focusPos, 10000, sectorPos -> {
			var volume = this.galaxy.getOrGenerateVolume(sectorPos);
			var nearest = volume.nearestInRadius(this.camera.focusPos, 10000);
			if (nearest != null) {
				var nearestPos = volume.posById(nearest.id()).scale(1 / TM_PER_UNIT);
				var camPos = this.camera.getFocusPos(partialTick).scale(1 / TM_PER_UNIT);
				var dir = nearestPos.subtract(camPos).normalize();

				bufferBuilder.vertex(camPos.x, camPos.y, camPos.z).color(1f, 0f, 0f, 1f)
						.normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();
				bufferBuilder.vertex(nearestPos.x, nearestPos.y, nearestPos.z).color(0f, 1f, 0f, 1f)
						.normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();
			}
		});

		bufferBuilder.end();

		PoseStack poseStack = RenderSystem.getModelViewStack();
		poseStack.pushPose();
		// this scale is taken from `RenderStateShare.VIEW_OFFSET_Z_LAYERING`, which
		// `RenderType.lines()` uses. It seems to resolve z-fighting issues that line
		// rendering otherwise has.
		// poseStack.scale(0.99975586f, 0.99975586f, 0.99975586f);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(false);
		BufferUploader.end(bufferBuilder);
		RenderSystem.depthMask(true);
		poseStack.popPose();

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();

		prevMatrices.restore();
	}

	private void renderGrid(VertexConsumer builder, float partialTick) {
		var currentThreshold = 1;
		var scaleFactor = 4;

		var scale = 1;
		for (var i = 0; i < 10; ++i) {
			currentThreshold *= scaleFactor;
			if (this.camera.scale > currentThreshold)
				scale = currentThreshold;
		}

		var focusPos = this.camera.getFocusPos(partialTick).scale(1 / TM_PER_UNIT);
		RenderHelper.renderGrid(builder, focusPos, scale * UNITS_PER_SECTOR, scaleFactor, 100);
	}

	private void renderSectorBox(VertexConsumer builder, Vec3i sectorPos) {
		var lo = Vec3.atLowerCornerOf(sectorPos).scale(UNITS_PER_SECTOR);
		var hi = Vec3.atLowerCornerOf(sectorPos.offset(1, 1, 1)).scale(UNITS_PER_SECTOR);
		var color = new Color(1, 1, 1, 0.2f);
		RenderHelper.renderAxisAlignedBox(builder, lo, hi, color);
	}

	private static record StarInfo(Vec3 pos, Vec3 color) {
	}

	private void renderSectorStars(VertexConsumer builder, Stream<StarInfo> sectorOffsets,
			float partialTick) {

		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		var backwards = up.cross(right);

		var billboardUp = up.scale(1);
		var billboardRight = right.scale(1);
		var billboardUpInner = billboardUp.scale(0.5);
		var billboardRightInner = billboardRight.scale(0.5);
		var billboardForwardInner = backwards.scale(-0.01);

		var focusPos = this.camera.getFocusPos(partialTick);

		sectorOffsets.forEach(info -> {
			var distanceFromFocus = focusPos.distanceTo(info.pos);
			var alphaFactor = 1 - Mth.clamp(distanceFromFocus / STAR_RENDER_RADIUS, 0, 1);

			if (alphaFactor == 0)
				return;

			var center = info.pos.scale(1 / TM_PER_UNIT);

			// TODO: do the billboarding in a vertex shader!

			float r = (float) info.color.x, g = (float) info.color.y, b = (float) info.color.z;
			float a = (float) alphaFactor;

			var qll = center.subtract(billboardUp).subtract(billboardRight);
			var qlh = center.subtract(billboardUp).add(billboardRight);
			var qhl = center.add(billboardUp).subtract(billboardRight);
			var qhh = center.add(billboardUp).add(billboardRight);
			builder.vertex(qhl.x, qhl.y, qhl.z).color(r, g, b, a).uv(1, 0).endVertex();
			builder.vertex(qll.x, qll.y, qll.z).color(r, g, b, a).uv(0, 0).endVertex();
			builder.vertex(qlh.x, qlh.y, qlh.z).color(r, g, b, a).uv(0, 1).endVertex();
			builder.vertex(qhh.x, qhh.y, qhh.z).color(r, g, b, a).uv(1, 1).endVertex();

			var qlls = center.subtract(billboardUpInner).subtract(billboardRightInner).add(billboardForwardInner);
			var qlhs = center.subtract(billboardUpInner).add(billboardRightInner).add(billboardForwardInner);
			var qhls = center.add(billboardUpInner).subtract(billboardRightInner).add(billboardForwardInner);
			var qhhs = center.add(billboardUpInner).add(billboardRightInner).add(billboardForwardInner);
			builder.vertex(qhls.x, qhls.y, qhls.z).color(1, 1, 1, a).uv(1, 0).endVertex();
			builder.vertex(qlls.x, qlls.y, qlls.z).color(1, 1, 1, a).uv(0, 0).endVertex();
			builder.vertex(qlhs.x, qlhs.y, qlhs.z).color(1, 1, 1, a).uv(0, 1).endVertex();
			builder.vertex(qhhs.x, qhhs.y, qhhs.z).color(1, 1, 1, a).uv(1, 1).endVertex();
		});

	}

	private static class RenderMatrices {

		private Matrix4f prevModelViewMatrix = null;
		private Matrix3f prevModelViewNormalMatrix = null;
		private Matrix4f prevProjectionMatrix = null;
		private Matrix3f prevInverseViewRotationMatrix = null;

		public static RenderMatrices capture() {
			var mats = new RenderMatrices();
			mats.prevInverseViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();
			mats.prevProjectionMatrix = RenderSystem.getProjectionMatrix();
			mats.prevModelViewMatrix = RenderSystem.getModelViewStack().last().pose().copy();
			mats.prevModelViewNormalMatrix = RenderSystem.getModelViewStack().last().normal().copy();
			RenderSystem.getModelViewStack().pushPose();
			return mats;
		}

		public void restore() {
			RenderSystem.getModelViewStack().last().pose().load(this.prevModelViewMatrix);
			RenderSystem.getModelViewStack().last().normal().load(this.prevModelViewNormalMatrix);
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(this.prevProjectionMatrix);
			RenderSystem.setInverseViewRotationMatrix(this.prevInverseViewRotationMatrix);
		}

	}

	// this should only be called once the render matrices have been captured,
	// because it overwrites them.
	private void setupRenderMatrices(float partialTick) {
		RenderSystem.setProjectionMatrix(this.camera.getProjectionMatrix());

		var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		var rotation = this.camera.getOrientation(partialTick);
		poseStack.mulPose(rotation);
		Matrix3f inverseViewRotationMatrix = poseStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}

		var cameraPos = this.camera.getPos(partialTick);
		poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
	}

	private <T> int countOctreeDescendants(Octree.Node<T> node) {
		if (node instanceof Octree.Node.Branch<T> branchNode) {
			int size = 0;
			size += countOctreeDescendants(branchNode.nnn);
			size += countOctreeDescendants(branchNode.nnp);
			size += countOctreeDescendants(branchNode.npn);
			size += countOctreeDescendants(branchNode.npp);
			size += countOctreeDescendants(branchNode.pnn);
			size += countOctreeDescendants(branchNode.pnp);
			size += countOctreeDescendants(branchNode.ppn);
			size += countOctreeDescendants(branchNode.ppp);
			return size;
		} else if (node instanceof Octree.Node.Leaf<T> leafNode) {
			return leafNode.elements.size();
		}
		return 0;
	}

	private <T> void renderOctreeDebug(BufferBuilder builder, Octree.Node<T> node, Color color) {
		var lo = node.min.scale(1 / TM_PER_UNIT);
		var hi = node.max.scale(1 / TM_PER_UNIT);
		RenderHelper.renderAxisAlignedBox(builder, lo, hi, color);
		if (node instanceof Octree.Node.Branch<T> branchNode) {
			renderOctreeDebug(builder, branchNode.nnn, color);
			renderOctreeDebug(builder, branchNode.nnp, color);
			renderOctreeDebug(builder, branchNode.npn, color);
			renderOctreeDebug(builder, branchNode.npp, color);
			renderOctreeDebug(builder, branchNode.pnn, color);
			renderOctreeDebug(builder, branchNode.pnp, color);
			renderOctreeDebug(builder, branchNode.ppn, color);
			renderOctreeDebug(builder, branchNode.ppp, color);
		}
	}

	private void renderStars(Octree<Lazy<StarSystem.Info, StarSystem>> volume, float partialTick) {
		var prevMatrices = RenderMatrices.capture();
		setupRenderMatrices(partialTick);

		// rotate then translate rotates everything about the origin, then translates
		// translate then rotate translates everything away from the origin and then
		// rotates everything around it.

		// the last operation on a poseStack is the operation that happens FIRST!
		// so mulPose(); translate(); actually translates and then rotates.

		// setup

		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		// renderOctreeDebug(builder, volume.rootNode, new Color(1, 0, 1, 0.2f));
		builder.end();

		PoseStack poseStack = RenderSystem.getModelViewStack();
		poseStack.pushPose();
		// this scale is taken from `RenderStateShare.VIEW_OFFSET_Z_LAYERING`, which
		// `RenderType.lines()` uses. It seems to resolve z-fighting issues that line
		// rendering otherwise has.
		// poseStack.scale(0.99975586f, 0.99975586f, 0.99975586f);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);
		RenderSystem.depthMask(true);
		poseStack.popPose();

		// Stars

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		renderSectorStars(builder, volume.streamIds().mapToObj(id -> {
			var initialInfo = Objects.requireNonNull(volume.getById(id)).getInitial();
			var brightestStar = initialInfo.stars.stream().max(Comparator.comparing(star -> star.luminosityLsol)).get();
			var starClass = brightestStar.starClass();
			var color = new Vec3(0, 1, 0);
			if (starClass != null) {
				color = starClass.color;
			} else if (brightestStar.type == StarNode.Type.WHITE_DWARF) {
				color = new Vec3(1, 1, 1);
			} else if (brightestStar.type == StarNode.Type.NEUTRON_STAR) {
				color = new Vec3(0.4, 0.4, 1);
			}
			var pos = Objects.requireNonNull(volume.posById(id));
			return new StarInfo(pos, color);
		}), partialTick);

		var cameraPos = this.camera.getPos(partialTick);
		builder.setQuadSortOrigin((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
		builder.end();

		this.client.getTextureManager().getTexture(STAR_ICON_LOCATION).setFilter(true, false); // filtered, mipped
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.applyModelViewMatrix();
		BufferUploader.end(builder);

		// cleanup

		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();

		prevMatrices.restore();
	}

	@Override
	public void tick() {
		super.tick();
		this.camera.tick();
	}

	private void enumerateSectors(Vec3 centerPos, double radius, Consumer<Vec3i> posConsumer) {
		var minSectorX = (int) Math.floor((centerPos.x - radius) / Galaxy.TM_PER_SECTOR);
		var maxSectorX = (int) Math.floor((centerPos.x + radius) / Galaxy.TM_PER_SECTOR);
		var minSectorY = (int) Math.floor((centerPos.y - radius) / Galaxy.TM_PER_SECTOR);
		var maxSectorY = (int) Math.floor((centerPos.y + radius) / Galaxy.TM_PER_SECTOR);
		var minSectorZ = (int) Math.floor((centerPos.z - radius) / Galaxy.TM_PER_SECTOR);
		var maxSectorZ = (int) Math.floor((centerPos.z + radius) / Galaxy.TM_PER_SECTOR);

		for (int x = minSectorX; x <= maxSectorX; ++x) {
			for (int y = minSectorY; y <= maxSectorY; ++y) {
				for (int z = minSectorZ; z <= maxSectorZ; ++z) {
					posConsumer.accept(new Vec3i(x, y, z));
				}
			}
		}

	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		// This render method gets a tick delta instead of a tick completion percentage,
		// so we have to get the partialTick manually.
		final var partialTick = this.client.getFrameTime();

		// TODO: render distant galaxies or something as a backdrop so its not just
		// pitch black. or maybe thats just how space is :p

		// TODO: figure out how to render the shape of the galaxy. Might have to rethink
		// how i do the density field stuff. Maybe have like a list of parts that are
		// explicitly assembled, instead of chaining together interfaces.

		RenderSystem.depthMask(false);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		var focusPos = this.camera.getFocusPos(partialTick);

		// var aaa = new Object() {
		// int verticalOffset = 0;
		// };
		// enumerateSectors(focusPos, STAR_RENDER_RADIUS, sectorPos -> {
		// var volume = this.galaxy.getOrGenerateVolume(sectorPos);
		// // RenderSystem.disableDepthTest();
		// drawString(
		// poseStack, this.client.font, "octree " + sectorPos.toShortString() + ":
		// #elements="
		// + volume.elements.size() + ", #descendants=" +
		// countOctreeDescendants(volume.rootNode),
		// 0, aaa.verticalOffset, 0xffffffff);
		// // RenderSystem.enableDepthTest();
		// aaa.verticalOffset += this.client.font.lineHeight;
		// });

		renderGrid(partialTick);

		enumerateSectors(focusPos, STAR_RENDER_RADIUS, sectorPos -> {
			// TODO: figure out how to evict old volumes that we're not using. Maybe use
			// something like vanilla's chunk ticketing system?
			var volume = this.galaxy.getOrGenerateVolume(sectorPos);
			renderStars(volume, partialTick);
		});

		fillGradient(poseStack, 0, 0, 200, this.height, 0xff110005, 0xff050002);

		var systemId = "";
		if (this.currentSystemId.sectorPos().getX() < 0) systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getX()) + ".";
		if (this.currentSystemId.sectorPos().getY() < 0) systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getY()) + ".";
		if (this.currentSystemId.sectorPos().getZ() < 0) systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getZ()) + "#";
		systemId += this.currentSystemId.sectorId();

		int h = 10;
		this.client.font.draw(poseStack, "System " + systemId, 10, h, 0xffffffff);
		h += this.client.font.lineHeight;
		h += 10;

		var volume = this.galaxy.getOrGenerateVolume(this.currentSystemId.sectorPos());
		var system = volume.getById(this.currentSystemId.sectorId()).getInitial();

		for (var star : system.stars) {
			this.client.font.draw(poseStack, describeStar(star), 10, h, 0xffffffff);
			h += this.client.font.lineHeight;
		}

		// TODO: mouse picking

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

	private String describeStar(StarNode starNode) {
		String starKind = "";
		var starClass = starNode.starClass();
		if (starClass != null) {
			starKind += "Class " + starClass.name + " ";
		}
		if (starNode.type == StarNode.Type.BLACK_HOLE) {
			starKind += "Black Hole ";
		} else if (starNode.type == StarNode.Type.NEUTRON_STAR) {
			starKind += "Neutron Star ";
		} else if (starNode.type == StarNode.Type.WHITE_DWARF) {
			starKind += "White Dwarf ";
		} else if (starNode.type == StarNode.Type.GIANT) {
			starKind += "Giant ";
		}

		return starKind;
	}

}
