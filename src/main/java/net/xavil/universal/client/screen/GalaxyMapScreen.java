package net.xavil.universal.client.screen;

import java.util.Comparator;
import java.util.Objects;
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
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.galaxy.Galaxy;
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

	public static final double STAR_RENDER_RADIUS = 0.9 * Galaxy.TM_PER_SECTOR;
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
		bufferBuilder.end();

		PoseStack poseStack = RenderSystem.getModelViewStack();
		poseStack.pushPose();
		// this scale is taken from `RenderStateShare.VIEW_OFFSET_Z_LAYERING`, which
		// `RenderType.lines()` uses. It seems to resolve z-fighting issues that line
		// rendering otherwise has.
		poseStack.scale(0.99975586f, 0.99975586f, 0.99975586f);
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

	private void renderSectorStars(VertexConsumer builder, Vec3i sectorPos, Stream<StarInfo> sectorOffsets,
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

		private PoseStack.Pose prevModelViewPose = null;
		private Matrix4f prevProjectionMatrix = null;
		private Matrix3f prevInverseViewRotationMatrix = null;

		public static RenderMatrices capture() {
			var mats = new RenderMatrices();
			mats.prevInverseViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix();
			mats.prevProjectionMatrix = RenderSystem.getProjectionMatrix();
			mats.prevModelViewPose = RenderSystem.getModelViewStack().last();
			return mats;
		}

		public void restore() {
			RenderSystem.getModelViewStack().last().pose().load(this.prevModelViewPose.pose());
			RenderSystem.getModelViewStack().last().normal().load(this.prevModelViewPose.normal());
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

	private void renderStars(LodVolume<StarSystem.Info, StarSystem> volume, float partialTick) {
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

		// Stars

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		renderSectorStars(builder, volume.position, volume.streamIds().mapToObj(id -> {
			var initialInfo = Objects.requireNonNull(volume.initialById(id));
			var brightestStar = initialInfo.stars.stream().max(Comparator.comparing(star -> star.luminosityLsol));
			var color = brightestStar.get().starClass().color;
			var pos = Objects.requireNonNull(volume.offsetById(id)).add(volume.getBasePos());
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

		renderGrid(partialTick);

		var focusPos = this.camera.getFocusPos(partialTick);
		var focusedSectorX = (int) Math.floor(focusPos.x / Galaxy.TM_PER_SECTOR);
		var focusedSectorY = (int) Math.floor(focusPos.y / Galaxy.TM_PER_SECTOR);
		var focusedSectorZ = (int) Math.floor(focusPos.z / Galaxy.TM_PER_SECTOR);
		var focusedSector = new Vec3i(focusedSectorX, focusedSectorY, focusedSectorZ);

		var renderRadiusSectors = STAR_RENDER_RADIUS / Galaxy.TM_PER_SECTOR;
		int sectorDistance = (int) Math.ceil(renderRadiusSectors);

		for (int x = -sectorDistance; x <= sectorDistance; ++x) {
			for (int y = -sectorDistance; y <= sectorDistance; ++y) {
				for (int z = -sectorDistance; z <= sectorDistance; ++z) {
					var sectorPos = focusedSector.offset(x, y, z);
					// TODO: figure out how to evict old volumes that we're not using. Maybe use
					// something like vanilla's chunk ticketing system?
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
