package net.xavil.universal.client.screen;

import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.NameTemplate;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.TicketedVolume;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

public class GalaxyMapScreen extends UniversalScreen {

	public static final double STAR_RENDER_RADIUS = 0.5 * Galaxy.TM_PER_SECTOR;
	public static final double TM_PER_UNIT = 1000;
	public static final double UNITS_PER_SECTOR = Galaxy.TM_PER_SECTOR / 1000;

	public static final Color SELECTION_LINE_COLOR = new Color(1f, 0f, 1f, 0.2f);
	public static final Color NEAREST_LINE_COLOR = new Color(1f, 1f, 1f, 1f);

	private final Minecraft client = Minecraft.getInstance();

	private ClientUniverse universe;
	private Galaxy galaxy;

	private boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false, isRightPressed = false;

	private OrbitCamera camera = new OrbitCamera(TM_PER_UNIT);
	private UniverseId.SectorId currentSystemId;

	private TicketedVolume.Ticket galaxyVolumeTicket;
	// private TicketedVolume.Ticket focusTicket;

	public GalaxyMapScreen(@Nullable Screen previousScreen, UniverseId.SystemId systemToFocus) {
		super(new TranslatableComponent("narrator.screen.starmap"), previousScreen);

		this.camera.pitch.set(Math.PI / 8);
		this.camera.yaw.set(Math.PI / 8);
		this.camera.scale.set(4.0);
		this.camera.scale.setTarget(8.0);

		this.universe = MinecraftClientAccessor.getUniverse(this.client);
		this.galaxyVolumeTicket = this.universe.volume.addTicket(systemToFocus.galaxySector().sectorPos(), 0, -1);
		this.galaxy = this.universe.volume.get(systemToFocus.galaxySector()).getFull();

		this.currentSystemId = systemToFocus.systemSector();

		var volumePos = systemToFocus.systemSector().sectorPos();
		var volume = this.galaxy.getVolumeAt(volumePos);
		this.camera.focus.set(volume.posById(this.currentSystemId.sectorId()));
	}

	private void updateTickets() {
		// this.galaxyVolumeTicket =
		// this.universe.volume.addTicket(startingSystem.galaxySector().sectorPos(), 0);

		// ticket around the camera's focus
	}

	@Override
	public void onClose() {
		super.onClose();
		// if (this.focusTicket != null)
		// this.galaxy.volume.removeTicket(this.focusTicket);
		if (this.galaxyVolumeTicket != null)
			this.galaxy.parentUniverse.volume.removeTicket(this.galaxyVolumeTicket);
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
		} else if (button == 1) {
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(0, dragScale * dy, 0));
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
			this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 2));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * 1.2, 100));
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

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

	public @Nullable UniverseId.SectorId getNearestSystem(Vec3 pos, double radius) {

		var nearest = new Object() {
			double distanceSqr = Double.MAX_VALUE;
			UniverseId.SectorId id = null;
		};
		enumerateSectors(pos, radius, sectorPos -> {
			var volume = this.galaxy.getVolumeAt(sectorPos);
			var nearestInSector = volume.nearestInRadius(pos, radius);
			if (nearestInSector != null) {
				if (nearestInSector.pos().distanceToSqr(pos) < nearest.distanceSqr) {
					nearest.distanceSqr = nearestInSector.pos().distanceToSqr(pos);
					nearest.id = new UniverseId.SectorId(sectorPos, nearestInSector.id());
				}
			}
		});

		return nearest.id;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (super.charTyped(c, i))
			return true;

		final var partialTick = this.client.getFrameTime();

		if (c == 'q') {
			var nameTemplate = NameTemplate.compile("^SL?(< >[<Major><Minor>])< >^*^*<->^*");
			// var nameTemplate = NameTemplate.compile("[(<M>d?d?d)(<NGC >dddddd?d?d)]");
			var name = nameTemplate.generate(new Random());
			Mod.LOGGER.info(name);
			return true;
		} else if (c == 'e') {
			var volume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
			var system = volume.getById(this.currentSystemId.sectorId()).getFull();
			var screen = new SystemMapScreen(this, system);
			this.client.setScreen(screen);
			return true;
		} else if (c == 'f') {
			var focusPos = this.camera.focus.get(partialTick);
			var nearestId = getNearestSystem(focusPos, 10000);
			if (nearestId != null) {
				var volume = this.galaxy.getVolumeAt(nearestId.sectorPos());
				this.currentSystemId = nearestId;
				this.camera.focus.setTarget(volume.posById(this.currentSystemId.sectorId()));
			}
			return true;
		}
		return false;
	}

	private void renderGrid(float partialTick) {
		var prevMatrices = this.camera.setupRenderMatrices(partialTick);

		// setup

		BufferBuilder builder = Tesselator.getInstance().getBuilder();

		// Grid

		RenderHelper.renderGrid(builder, this.camera, TM_PER_UNIT, 1, 10, 100, partialTick);

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		var focusPos = this.camera.focus.get(partialTick);
		var selectedVolume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
		var selectedPos = selectedVolume.posById(this.currentSystemId.sectorId()).scale(1 / TM_PER_UNIT);
		{
			var camPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
			var dir = selectedPos.subtract(camPos).normalize();
			RenderHelper.addLine(builder, camPos, selectedPos, SELECTION_LINE_COLOR);
			// builder.vertex(camPos.x, camPos.y, camPos.z).color(1f, 0f, 1f, 0.2f)
			// .normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();
			// builder.vertex(selectedPos.x, selectedPos.y, selectedPos.z).color(1f, 0f, 1f,
			// 0.2f)
			// .normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();

		}

		var nearest = getNearestSystem(focusPos, 10000);
		if (nearest != null) {
			var volume = this.galaxy.getVolumeAt(nearest.sectorPos());
			var nearestPos = volume.posById(nearest.sectorId()).scale(1 / TM_PER_UNIT);
			var camPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
			var dir = nearestPos.subtract(camPos).normalize();
			RenderHelper.addLine(builder, camPos, selectedPos, NEAREST_LINE_COLOR);
			// builder.vertex(camPos.x, camPos.y, camPos.z).color(1f, 1f, 1f, 1f)
			// .normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();
			// builder.vertex(camPos.x + dir.x, camPos.y + dir.y, camPos.z +
			// dir.z).color(1f, 1f, 1f, 0f)
			// .normal((float) dir.x, (float) dir.y, (float) dir.z).endVertex();
		}

		builder.end();

		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();

		prevMatrices.restore();
	}

	private void renderSectorBox(VertexConsumer builder, Vec3i sectorPos) {
		var lo = Vec3.atLowerCornerOf(sectorPos).scale(UNITS_PER_SECTOR);
		var hi = Vec3.atLowerCornerOf(sectorPos.offset(1, 1, 1)).scale(UNITS_PER_SECTOR);
		var color = new Color(1, 1, 1, 0.2f);
		RenderHelper.addAxisAlignedBox(builder, lo, hi, color);
	}

	private static record StarInfo(Vec3 pos, StarSystemNode node) {
	}

	private void renderSectorStars(VertexConsumer builder, Stream<StarInfo> sectorOffsets,
			float partialTick) {

		var focusPos = this.camera.focus.get(partialTick);
		var cameraPos = this.camera.getPos(partialTick).scale(TM_PER_UNIT);

		sectorOffsets.forEach(info -> {
			var distanceFromFocus = focusPos.distanceTo(info.pos);
			var alphaFactorFocus = 1 - Mth.clamp(distanceFromFocus / STAR_RENDER_RADIUS, 0, 1);
			if (alphaFactorFocus <= 0.05)
				return;

			var distanceFromCamera = cameraPos.distanceTo(info.pos);
			var alphaFactorCamera = 1 - Mth.clamp(distanceFromCamera / STAR_RENDER_RADIUS * 4, 0, 1);
			var alphaFactor = Math.max(alphaFactorFocus, alphaFactorCamera);

			var center = info.pos.scale(1 / TM_PER_UNIT);

			// TODO: do the billboarding in a vertex shader!

			RenderHelper.addBillboard(builder, this.camera, info.node, center, TM_PER_UNIT, partialTick);
		});

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
		RenderHelper.addAxisAlignedBox(builder, lo, hi, color);
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
		var prevMatrices = this.camera.setupRenderMatrices(partialTick);

		// rotate then translate rotates everything about the origin, then translates
		// translate then rotate translates everything away from the origin and then
		// rotates everything around it.

		// the last operation on a poseStack is the operation that happens FIRST!
		// so mulPose(); translate(); actually translates and then rotates.

		// setup

		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		// renderOctreeDebug(builder, volume.rootNode, new Color(1, 0, 1, 0.2f));
		builder.end();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);

		// Stars

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		renderSectorStars(builder, volume.streamIds().mapToObj(id -> {
			var initialInfo = Objects.requireNonNull(volume.getById(id)).getInitial();
			var brightestStar = initialInfo.stars.stream().max(Comparator.comparing(star -> star.luminosityLsol)).get();
			var pos = Objects.requireNonNull(volume.posById(id));
			return new StarInfo(pos, brightestStar);
		}), partialTick);

		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);

		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		var focusPos = this.camera.focus.get(partialTick).scale(1 / TM_PER_UNIT);
		for (var id = 0; id < volume.elementCount(); ++id) {
			var pos = Objects.requireNonNull(volume.posById(id)).scale(1 / TM_PER_UNIT);
			if (pos.distanceTo(focusPos) > 10)
				continue;
			RenderHelper.addLine(builder,
					new Vec3(pos.x, focusPos.y, pos.z),
					new Vec3(pos.x, pos.y, pos.z),
					NEAREST_LINE_COLOR.withA(0.2));
		}

		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1.5f);
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);

		prevMatrices.restore();
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

		// TODO: consolidate with the logic in mouseDragged()?
		final var partialTick = this.client.getFrameTime();
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.TM_PER_LY;

		var offset = new Vec3(right, 0, forward).yRot(-this.camera.yaw.get(partialTick).floatValue()).scale(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
	}

	public static void enumerateSectors(Vec3 centerPos, double radius, Consumer<Vec3i> posConsumer) {
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
		// fillGradient(poseStack, 0, 0, this.width, this.height, 0xcf000000,
		// 0xcf000000);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);

		var focusPos = this.camera.focus.get(partialTick);

		renderGrid(partialTick);

		enumerateSectors(focusPos, STAR_RENDER_RADIUS, sectorPos -> {
			// TODO: figure out how to evict old volumes that we're not using. Maybe use
			// something like vanilla's chunk ticketing system?
			var volume = this.galaxy.getVolumeAt(sectorPos);
			renderStars(volume, partialTick);
		});

		// selected system gizmo

		var selectedVolume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
		var selectedPos = selectedVolume.posById(this.currentSystemId.sectorId()).scale(1 / TM_PER_UNIT);

		var prevMatrices = this.camera.setupRenderMatrices(partialTick);

		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		// RenderHelper.addBillboard(builder, this.camera, selectedPos, 0.5, 0,
		// partialTick, new Color(1, 1, 1, 0.2f));
		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		RenderHelper.addBillboard(builder, up, right, selectedPos, 0.5, 0, new Color(1, 1, 1, 0.2f));

		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.SELECTION_CIRCLE_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		BufferUploader.end(builder);

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

		prevMatrices.restore();

		// sidebar

		fillGradient(poseStack, 0, 0, 200, this.height, 0xff0a0a0a, 0xff0a0a0a);

		var systemId = "";
		if (this.currentSystemId.sectorPos().getX() < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getX()) + ".";
		if (this.currentSystemId.sectorPos().getY() < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getY()) + ".";
		if (this.currentSystemId.sectorPos().getZ() < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().getZ()) + "#";
		systemId += this.currentSystemId.sectorId();

		var volume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
		var system = volume.getById(this.currentSystemId.sectorId()).getInitial();

		int h = 10;
		this.client.font.draw(poseStack, "System " + systemId, 10, h, 0xffffffff);
		h += this.client.font.lineHeight;
		this.client.font.draw(poseStack, system.name, 10, h, 0xffffffff);
		h += this.client.font.lineHeight;
		h += 10;

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
