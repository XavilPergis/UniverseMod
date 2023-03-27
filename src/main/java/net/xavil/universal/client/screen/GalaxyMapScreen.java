package net.xavil.universal.client.screen;

import java.util.Random;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.GalaxyRenderingContext;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.debug.GalaxyDensityDebugScreen;
import net.xavil.universal.client.screen.debug.SystemGenerationDebugScreen;
import net.xavil.universal.common.NameTemplate;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.TicketedVolume;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec3;

public class GalaxyMapScreen extends UniversalScreen {

	public static final double STAR_RENDER_RADIUS = 0.5 * Galaxy.TM_PER_SECTOR;
	public static final double TM_PER_UNIT = 1000;
	public static final double UNITS_PER_SECTOR = Galaxy.TM_PER_SECTOR / 1000;

	public static final Color SELECTION_LINE_COLOR = new Color(1f, 0f, 1f, 0.2f);
	public static final Color NEAREST_LINE_COLOR = new Color(1f, 1f, 1f, 1f);

	private final Minecraft client = Minecraft.getInstance();

	private ClientUniverse universe;
	private Galaxy galaxy;
	private GalaxyRenderingContext galaxyRenderingContext;

	private boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false, isRightPressed = false;

	private OrbitCamera camera = new OrbitCamera(1e12, TM_PER_UNIT);
	private SectorId galaxyId;
	private SectorId currentSystemId;

	private TicketedVolume.Ticket galaxyVolumeTicket;
	// private TicketedVolume.Ticket focusTicket;

	public GalaxyMapScreen(@Nullable Screen previousScreen, SystemId systemToFocus) {
		super(new TranslatableComponent("narrator.screen.starmap"), previousScreen);

		this.camera.pitch.set(Math.PI / 8);
		this.camera.yaw.set(Math.PI / 8);
		this.camera.scale.set(4.0);
		this.camera.scale.setTarget(8.0);

		this.universe = MinecraftClientAccessor.getUniverse(this.client);
		this.galaxyVolumeTicket = this.universe.volume.addTicket(systemToFocus.galaxySector().sectorPos(), 0, -1);
		this.galaxy = this.universe.volume.get(systemToFocus.galaxySector()).getFull();

		this.galaxyId = systemToFocus.galaxySector();
		this.currentSystemId = systemToFocus.systemSector();

		var volumePos = systemToFocus.systemSector().sectorPos();
		var volume = this.galaxy.getVolumeAt(volumePos);
		this.camera.focus.set(volume.posById(this.currentSystemId.sectorId()));

		this.galaxyRenderingContext = new GalaxyRenderingContext(this.galaxy);
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
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.Tm_PER_ly;

		if (button == 2) {
			var realDy = this.camera.pitch.get(partialTick) < 0 ? -dy : dy;
			var offset = Vec3.from(dx, 0, realDy).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
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
			this.camera.scale.setTarget(Math.max(prevTarget / 1.2, 0.001));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * 1.2, 1000000));
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
		} else if (keyCode == GLFW.GLFW_KEY_M) {
			this.client.setScreen(new GalaxyDensityDebugScreen(this, this.galaxy));
		} else if (keyCode == GLFW.GLFW_KEY_N) {
			this.client.setScreen(new SystemGenerationDebugScreen(this));
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

	public @Nullable SectorId getNearestSystem(Vec3 pos, double radius) {

		var nearest = new Object() {
			double distanceSqr = Double.MAX_VALUE;
			SectorId id = null;
		};
		TicketedVolume.enumerateSectors(pos, STAR_RENDER_RADIUS, Galaxy.TM_PER_SECTOR, sectorPos -> {
			var volume = this.galaxy.getVolumeAt(sectorPos);
			var nearestInSector = volume.nearestInRadius(pos, radius);
			if (nearestInSector != null) {
				if (nearestInSector.pos.distanceToSquared(pos) < nearest.distanceSqr) {
					nearest.distanceSqr = nearestInSector.pos.distanceToSquared(pos);
					nearest.id = new SectorId(sectorPos, nearestInSector.id);
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
			var name = nameTemplate.generate(Rng.wrap(new Random()));
			Mod.LOGGER.info(name);
			return true;
		} else if (c == 'e') {
			var volume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
			var system = volume.getById(this.currentSystemId.sectorId())
					.getFull();
			var screen = new SystemMapScreen(this, new SystemId(this.galaxyId, this.currentSystemId),
					system);
			this.client.setScreen(screen);
			return true;
		} else if (c == 'f') {
			var focusPos = this.camera.focus.get(partialTick);
			var nearestId = getNearestSystem(focusPos, 10000);
			if (nearestId != null) {
				var volume = this.galaxy.getVolumeAt(nearestId.sectorPos());
				this.currentSystemId = nearestId;
				this.camera.focus.setTarget(
						volume.posById(this.currentSystemId.sectorId()));
			}
			return true;
		}
		return false;
	}

	private void renderGrid(OrbitCamera.Cached camera, float partialTick) {
		var prevMatrices = camera.setupRenderMatrices();

		// setup

		final var builder = BufferRenderer.immediateBuilder();

		// Grid

		RenderHelper.renderGrid(builder, camera, TM_PER_UNIT, 1, 10, 40, partialTick);

		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		if (camera.scale < 300) {
			var selectedVolume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
			var selectedPos = selectedVolume.posById(this.currentSystemId.sectorId()).div(TM_PER_UNIT);
			var camPos = camera.focus.div(TM_PER_UNIT);
			RenderHelper.addLine(builder, camera, camPos, selectedPos, SELECTION_LINE_COLOR);

			var nearest = getNearestSystem(camera.focus, 10000);
			if (nearest != null) {
				var volume = this.galaxy.getVolumeAt(nearest.sectorPos());
				var nearestPos = volume.posById(nearest.sectorId()).div(TM_PER_UNIT);
				RenderHelper.addLine(builder, camera, camPos, nearestPos, new Color(1, 0, 1, 1));
			}
		}

		builder.end();

		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		builder.draw(GameRenderer.getRendertypeLinesShader());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();

		prevMatrices.restore();
	}

	// private void renderSectorBox(VertexConsumer builder, Vec3i sectorPos) {
	// var lo = sectorPos.lowerCorner().mul(UNITS_PER_SECTOR);
	// var hi = sectorPos.upperCorner().mul(UNITS_PER_SECTOR);
	// var color = new Color(1, 1, 1, 0.2f);
	// RenderHelper.addAxisAlignedBox(builder, lo, hi, color);
	// }

	// private <T> int countOctreeDescendants(Octree.Node<T> node) {
	// if (node instanceof Octree.Node.Branch<T> branchNode) {
	// int size = 0;
	// size += countOctreeDescendants(branchNode.nnn);
	// size += countOctreeDescendants(branchNode.nnp);
	// size += countOctreeDescendants(branchNode.npn);
	// size += countOctreeDescendants(branchNode.npp);
	// size += countOctreeDescendants(branchNode.pnn);
	// size += countOctreeDescendants(branchNode.pnp);
	// size += countOctreeDescendants(branchNode.ppn);
	// size += countOctreeDescendants(branchNode.ppp);
	// return size;
	// } else if (node instanceof Octree.Node.Leaf<T> leafNode) {
	// return leafNode.elements.size();
	// }
	// return 0;
	// }

	// private <T> void renderOctreeDebug(BufferBuilder builder, Octree.Node<T>
	// node, Color color) {
	// var lo = node.min.div(TM_PER_UNIT);
	// var hi = node.max.div(TM_PER_UNIT);
	// RenderHelper.addAxisAlignedBox(builder, lo, hi, color);
	// if (node instanceof Octree.Node.Branch<T> branchNode) {
	// renderOctreeDebug(builder, branchNode.nnn, color);
	// renderOctreeDebug(builder, branchNode.nnp, color);
	// renderOctreeDebug(builder, branchNode.npn, color);
	// renderOctreeDebug(builder, branchNode.npp, color);
	// renderOctreeDebug(builder, branchNode.pnn, color);
	// renderOctreeDebug(builder, branchNode.pnp, color);
	// renderOctreeDebug(builder, branchNode.ppn, color);
	// renderOctreeDebug(builder, branchNode.ppp, color);
	// }
	// }

	private void renderStars(OrbitCamera.Cached camera, Octree<Lazy<StarSystem.Info, StarSystem>> volume,
			float partialTick) {
		var prevMatrices = camera.setupRenderMatrices();

		// rotate then translate rotates everything about the origin, then translates
		// translate then rotate translates everything away from the origin and then
		// rotates everything around it.

		// the last operation on a poseStack is the operation that happens FIRST!
		// so mulPose(); translate(); actually translates and then rotates.

		// setup

		final var maxVisibleScale = 0;

		// if (camera.scale > maxVisibleScale) {
		// return;
		// }

		// final var scaleAlpha = 1 - Mth.clamp(camera.scale / maxVisibleScale, 0, 1);

		final var builder = BufferRenderer.immediateBuilder();

		// Stars

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		volume.enumerateElements(element -> {
			var distanceFromFocus = camera.pos.mul(TM_PER_UNIT).distanceTo(element.pos);
			var alphaFactorFocus = 1 - Mth.clamp(distanceFromFocus / STAR_RENDER_RADIUS, 0, 1);
			if (alphaFactorFocus <= 0.05)
				return;

			var center = element.pos.div(TM_PER_UNIT);

			// we should maybe consider doing the billboarding in a vertex shader, because
			// that way we can build all the geometry for a sector into a vertex buffer and
			// just emit a few draw calls, instead of having to build the buffer from
			// scratch each frame.

			var displayStar = element.value.getInitial().primaryStar;
			RenderHelper.addBillboard(builder, camera, new PoseStack(), displayStar, center);
		});

		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));

		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		var focusPos = camera.focus.div(TM_PER_UNIT);
		volume.enumerateElements(element -> {
			var pos = element.pos.div(TM_PER_UNIT);
			if (pos.distanceTo(focusPos) > 10)
				return;
			RenderHelper.addLine(builder, camera,
					Vec3.from(pos.x, focusPos.y, pos.z),
					Vec3.from(pos.x, pos.y, pos.z),
					NEAREST_LINE_COLOR.withA(0.2));
		});

		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1.5f);
		RenderSystem.depthMask(false);
		builder.draw(GameRenderer.getRendertypeLinesShader());

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
		final var dragScale = TM_PER_UNIT * this.camera.scale.get(partialTick) * 10 / Units.Tm_PER_ly;

		var offset = Vec3.from(right, 0, forward).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
	}

	@Override
	public boolean shouldRenderWorld() {
		return false;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		// This render method gets a tick delta instead of a tick completion percentage,
		// so we have to get the partialTick manually.
		final var partialTick = this.client.getFrameTime();
		final var camera = this.camera.cached(partialTick);
		var prevMatrices = camera.setupRenderMatrices();

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

		renderGrid(camera, partialTick);

		final var builder = BufferRenderer.immediateBuilder();

		this.galaxyRenderingContext.build();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new PoseStack(), pos.div(TM_PER_UNIT), size / TM_PER_UNIT, Color.WHITE.withA(0.2));
		});

		builder.end();

		this.client.getTextureManager().getTexture(Mod.namespaced("textures/misc/galaxyglow.png")).setFilter(true,
				false);
		RenderSystem.setShaderTexture(0, Mod.namespaced("textures/misc/galaxyglow.png"));
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));

		if (camera.scale < 300) {
			TicketedVolume.enumerateSectors(camera.pos.mul(TM_PER_UNIT), STAR_RENDER_RADIUS, Galaxy.TM_PER_SECTOR, sectorPos -> {
				// TODO: figure out how to evict old volumes that we're not using. Maybe use
				// something like vanilla's chunk ticketing system?
				var volume = this.galaxy.getVolumeAt(sectorPos);
				renderStars(camera, volume, partialTick);
			});
		}

		prevMatrices.restore();

		// sidebar

		fillGradient(poseStack, 0, 0, 200, this.height, 0xff0a0a0a, 0xff0a0a0a);

		var systemId = "";
		if (this.currentSystemId.sectorPos().x < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().x) + ".";
		if (this.currentSystemId.sectorPos().y < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().y) + ".";
		if (this.currentSystemId.sectorPos().z < 0)
			systemId += "M";
		systemId += Math.abs(this.currentSystemId.sectorPos().z) + "#";
		systemId += this.currentSystemId.sectorId();

		var volume = this.galaxy.getVolumeAt(this.currentSystemId.sectorPos());
		var system = volume.getById(this.currentSystemId.sectorId()).getInitial();

		int h = 10;
		this.client.font.draw(poseStack, "System " + systemId, 10, h, 0xffffffff);
		h += this.client.font.lineHeight;
		this.client.font.draw(poseStack, system.name, 10, h, 0xffffffff);
		h += this.client.font.lineHeight;
		h += 10;

		this.client.font.draw(poseStack, describeStar(system.primaryStar), 10, h, 0xffffffff);
		h += this.client.font.lineHeight;

		// TODO: mouse picking

		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

	private String describeStar(StellarCelestialNode starNode) {
		String starKind = "";
		var starClass = starNode.starClass();
		if (starClass != null) {
			starKind += "Class " + starClass.name + " ";
		}
		if (starNode.type == StellarCelestialNode.Type.BLACK_HOLE) {
			starKind += "Black Hole ";
		} else if (starNode.type == StellarCelestialNode.Type.NEUTRON_STAR) {
			starKind += "Neutron Star ";
		} else if (starNode.type == StellarCelestialNode.Type.WHITE_DWARF) {
			starKind += "White Dwarf ";
		} else if (starNode.type == StellarCelestialNode.Type.GIANT) {
			starKind += "Giant ";
		}

		return starKind;
	}

}
