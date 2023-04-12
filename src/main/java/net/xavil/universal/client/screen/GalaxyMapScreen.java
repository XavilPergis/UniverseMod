package net.xavil.universal.client.screen;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.ClientDebugFeatures;
import net.xavil.universal.client.GalaxyRenderingContext;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universal.client.screen.OrbitCamera.Cached;
import net.xavil.universal.client.screen.debug.GalaxyDensityDebugScreen;
import net.xavil.universal.client.screen.debug.SystemGenerationDebugScreen;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.universal.common.universe.galaxy.SectorTicket;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystem.Info;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.common.universe.universe.GalaxyTicket;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.math.Color;
import net.xavil.util.math.Ray;
import net.xavil.util.math.Vec3;

public class GalaxyMapScreen extends Universal3dScreen {

	// public static final double STAR_RENDER_RADIUS = 1.5 * Galaxy.SECTOR_WIDTH_Tm;
	public static final double TM_PER_UNIT = 1000;
	// public static final double UNITS_PER_SECTOR = Galaxy.SECTOR_WIDTH_Tm / 1000;

	public static final Color SELECTION_LINE_COLOR = new Color(1f, 0f, 1f, 0.2f);
	public static final Color NEAREST_LINE_COLOR = new Color(1f, 1f, 1f, 1f);
	public static final Color SECTOR_DEBUG_LINE_COLOR = new Color(0.8f, 0.8f, 0.3f, 1f);

	private final Minecraft client = Minecraft.getInstance();

	private final Universe universe;
	private final Galaxy galaxy;
	private final GalaxyRenderingContext galaxyRenderingContext;
	private final GalaxyTicket galaxyTicket;
	private final SectorTicket cameraTicket;
	private final SystemTicket selectedSystemTicket;

	private GalaxySectorId selectedSystemId;
	private Ray lastPickRay = null;
	private Vec3[] frustumPoints = null;

	public GalaxyMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, GalaxySectorId systemToFocus) {
		super(new TranslatableComponent("narrator.screen.starmap"), previousScreen, new OrbitCamera(1e12, TM_PER_UNIT),
				1e2, 1e5);

		final var tempDisposer = new Disposable.Multi();

		this.universe = MinecraftClientAccessor.getUniverse(this.client);

		this.galaxy = galaxy;
		this.galaxyRenderingContext = new GalaxyRenderingContext(this.galaxy);

		this.galaxyTicket = this.universe.sectorManager.createGalaxyTicket(this.disposer, galaxy.galaxyId);

		final var tempTicket = galaxy.sectorManager.createSectorTicket(tempDisposer,
				SectorTicketInfo.single(systemToFocus.sectorPos()));
		galaxy.sectorManager.forceLoad(tempTicket);
		final var initial = galaxy.sectorManager.getInitial(systemToFocus);
		if (initial.isNone()) {
			Mod.LOGGER.error("Tried to open starmap to nonexistent id {}", systemToFocus);
		}
		// final var initialPos = initial.map(i -> i.pos()).unwrapOr(Vec3.ZERO);
		final var initialPos = Vec3.ZERO;
		this.camera.focus.set(initialPos);
		this.cameraTicket = galaxy.sectorManager.createSectorTicket(this.disposer, SectorTicketInfo.visual(initialPos));

		this.selectedSystemTicket = galaxy.sectorManager.createSystemTicket(this.disposer, systemToFocus);

		if (initial.isSome())
			this.selectedSystemId = systemToFocus;

		tempDisposer.dispose();
	}

	private Vec3 getStarViewCenterPos(OrbitCamera.Cached camera) {
		if (ClientDebugFeatures.SECTOR_TICKET_AROUND_FOCUS.isEnabled())
			return camera.focus;
		return camera.pos.mul(TM_PER_UNIT).add(camera.forward.mul(GalaxySector.BASE_SIZE_Tm));
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		final var wasDragging = this.isDragging();
		if (super.mouseReleased(mouseX, mouseY, button))
			return true;

		if (!wasDragging && button == 0) {
			getLastCamera().ifSome(camera -> {
				final var ray = camera.rayForPicking(this.client.getWindow(), mouseX, mouseY);
				pickElement(camera, ray).ifSome(id -> this.selectedSystemId = id);
				this.lastPickRay = ray;
			});
			return true;
		}

		return false;
	}

	private Option<GalaxySectorId> pickElement(OrbitCamera.Cached camera, Ray ray) {
		// @formatter:off
		final var closest = new Object() {
			double    distance    = Double.POSITIVE_INFINITY;
			int       sectorIndex = -1;
			SectorPos sectorPos   = null;
		};
		// @formatter:on
		final var viewCenter = getStarViewCenterPos(camera);
		this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
			final var min = sector.pos().minBound();
			final var max = sector.pos().maxBound();
			if (!ray.intersectAABB(min, max))
				return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			// sector.initialElements.forEach(elem -> {
			sector.initialElements.iter().enumerate().forEach(elem -> {
				if (elem.item.pos().distanceTo(viewCenter) > levelSize)
					return;
				final var elemPos = elem.item.pos().div(TM_PER_UNIT);
				final var distance = ray.origin().distanceTo(elemPos);
				if (!ray.intersectsSphere(elemPos, 0.05 * distance))
					return;
				if (distance < closest.distance) {
					closest.distance = distance;
					closest.sectorPos = sector.pos();
					closest.sectorIndex = elem.index;
				}
			});
		});

		if (closest.sectorIndex == -1)
			return Option.none();
		return Option.some(GalaxySectorId.from(closest.sectorPos, closest.sectorIndex));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		// final var partialTick = this.client.getFrameTime();
		if (keyCode == GLFW.GLFW_KEY_M) {
			this.client.setScreen(new GalaxyDensityDebugScreen(this, this.galaxy));
		} else if (keyCode == GLFW.GLFW_KEY_N) {
			this.client.setScreen(new SystemGenerationDebugScreen(this));
		} else if (keyCode == GLFW.GLFW_KEY_B && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			ClientDebugFeatures.SHOW_SECTOR_BOUNDARIES.toggle();
		} else if (keyCode == GLFW.GLFW_KEY_H && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			ClientDebugFeatures.SECTOR_TICKET_AROUND_FOCUS.toggle();
		} else if (keyCode == GLFW.GLFW_KEY_F && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			getLastCamera().ifSome(camera -> {
				this.frustumPoints = new Vec3[8];
				// @formatter:off
				int i = 0;
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1, -1, -1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1, -1,  1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1,  1, -1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1,  1,  1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1, -1, -1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1, -1,  1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1,  1, -1));
				this.frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1,  1,  1));
				// @formatter:on
			});
		} else if (keyCode == GLFW.GLFW_KEY_R) {
			this.galaxy.sectorManager.getSystem(this.selectedSystemId).ifSome(system -> {
				final var id = new SystemId(this.galaxy.galaxyId, this.selectedSystemId);
				final var screen = new SystemMapScreen(this, id, system);
				this.client.setScreen(screen);
			});
			return true;
		}

		return false;
	}

	private static boolean isAabbInFrustum(OrbitCamera.Cached camera, Vec3 min, Vec3 max) {
		// final var nnn = camera.projectWorldSpace(Vec3.from(min.x, min.y, min.z));
		// final var nnp = camera.projectWorldSpace(Vec3.from(min.x, min.y, max.z));
		// final var npn = camera.projectWorldSpace(Vec3.from(min.x, max.y, min.z));
		// final var npp = camera.projectWorldSpace(Vec3.from(min.x, max.y, max.z));
		// final var pnn = camera.projectWorldSpace(Vec3.from(max.x, min.y, min.z));
		// final var pnp = camera.projectWorldSpace(Vec3.from(max.x, min.y, max.z));
		// final var ppn = camera.projectWorldSpace(Vec3.from(max.x, max.y, min.z));
		// final var ppp = camera.projectWorldSpace(Vec3.from(max.x, max.y, max.z));

		// this approach should work for any convex shape, which a cube definitely is :p
		// @formatter:off
		// final var ltX = nnn.x <=  1 | nnp.x <=  1 | npn.x <=  1 | npp.x <=  1 | pnn.x <=  1 | pnp.x <=  1 | ppn.x <=  1 | ppp.x <=  1;
		// final var gtX = nnn.x >= -1 | nnp.x >= -1 | npn.x >= -1 | npp.x >= -1 | pnn.x >= -1 | pnp.x >= -1 | ppn.x >= -1 | ppp.x >= -1;
		// final var ltY = nnn.y <=  1 | nnp.y <=  1 | npn.y <=  1 | npp.y <=  1 | pnn.y <=  1 | pnp.y <=  1 | ppn.y <=  1 | ppp.y <=  1;
		// final var gtY = nnn.y >= -1 | nnp.y >= -1 | npn.y >= -1 | npp.y >= -1 | pnn.y >= -1 | pnp.y >= -1 | ppn.y >= -1 | ppp.y >= -1;
		// final var ltZ = nnn.z <=  1 | nnp.z <=  1 | npn.z <=  1 | npp.z <=  1 | pnn.z <=  1 | pnp.z <=  1 | ppn.z <=  1 | ppp.z <=  1;
		// final var gtZ = nnn.z >= -1 | nnp.z >= -1 | npn.z >= -1 | npp.z >= -1 | pnn.z >= -1 | pnp.z >= -1 | ppn.z >= -1 | ppp.z >= -1;
		// @formatter:on

		// return (ltX && gtX) && (ltY && gtY) && (ltZ && gtZ);
		return true;
	}

	private void renderDebugLines(OrbitCamera.Cached camera, float partialTick) {

		final var builder = BufferRenderer.immediateBuilder();
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		if (ClientDebugFeatures.SHOW_SECTOR_BOUNDARIES.isEnabled()) {
			this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
				final var min = sector.pos().minBound().div(TM_PER_UNIT);
				final var max = sector.pos().maxBound().div(TM_PER_UNIT);
				if (!sector.initialElements.isEmpty())
					RenderHelper.addAxisAlignedBox(builder, camera, min, max, SECTOR_DEBUG_LINE_COLOR);
			});
		}

		if (this.lastPickRay != null) {
			RenderHelper.addLine(builder, camera, this.lastPickRay.origin(), this.lastPickRay.stepBy(10000),
					Color.CYAN, Color.MAGENTA);
		}

		if (this.frustumPoints != null) {
			int i = 0;
			final var nnn = this.frustumPoints[i++];
			final var nnp = this.frustumPoints[i++];
			final var npn = this.frustumPoints[i++];
			final var npp = this.frustumPoints[i++];
			final var pnn = this.frustumPoints[i++];
			final var pnp = this.frustumPoints[i++];
			final var ppn = this.frustumPoints[i++];
			final var ppp = this.frustumPoints[i++];

			// near
			RenderHelper.addLine(builder, camera, nnn, npn, Color.YELLOW);
			RenderHelper.addLine(builder, camera, pnn, ppn, Color.YELLOW);
			RenderHelper.addLine(builder, camera, nnn, pnn, Color.YELLOW);
			RenderHelper.addLine(builder, camera, npn, ppn, Color.YELLOW);
			// far
			RenderHelper.addLine(builder, camera, nnp, npp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, pnp, ppp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, nnp, pnp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, npp, ppp, Color.YELLOW);
			// sides
			RenderHelper.addLine(builder, camera, nnn, nnp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, npn, npp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, pnn, pnp, Color.YELLOW);
			RenderHelper.addLine(builder, camera, ppn, ppp, Color.YELLOW);
		}

		builder.end();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.lineWidth(2.0f);
		builder.draw(GameRenderer.getRendertypeLinesShader());
	}

	private static class BillboardBatcher {
		private final Minecraft client = Minecraft.getInstance();

		public final FlexibleBufferBuilder builder;
		public final int billboardsPerBatch;
		public final CachedCamera<?> camera;

		private int current = 0;

		public BillboardBatcher(FlexibleBufferBuilder builder, int billboardsPerBatch, CachedCamera<?> camera) {
			this.builder = builder;
			this.billboardsPerBatch = billboardsPerBatch;
			this.camera = camera;
		}

		public void begin() {
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		}

		public void end() {
			builder.end();

			this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
			RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			RenderSystem.depthMask(false);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			RenderSystem.enableBlend();
			builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));

			this.current = 0;
		}

		public void add(StellarCelestialNode node, Vec3 pos) {
			RenderHelper.addBillboard(builder, camera, new PoseStack(), node, pos.div(TM_PER_UNIT));
			this.current += 1;
			if (this.current > this.billboardsPerBatch) {
				end();
				begin();
			}
		}
	}

	private void renderStars(OrbitCamera.Cached camera, float partialTick) {
		// final var camPos = camera.pos.mul(TM_PER_UNIT);
		// starRenderTicket.position = camPos;

		final var builder = BufferRenderer.immediateBuilder();
		final var batcher = new BillboardBatcher(builder, 10000, camera);

		final var viewCenter = getStarViewCenterPos(camera);
		// builder.begin(VertexFormat.Mode.QUADS,
		// DefaultVertexFormat.POSITION_COLOR_TEX);
		batcher.begin();
		this.galaxy.sectorManager.enumerate(this.cameraTicket, sector -> {
			// final var min = sector.pos().minBound().div(TM_PER_UNIT);
			// final var max = sector.pos().maxBound().div(TM_PER_UNIT);
			// if (!isAabbInFrustum(camera, min, max))
			// return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(viewCenter) > levelSize)
					return;
				batcher.add(elem.info().primaryStar, elem.pos());
			});
		});
		// builder.end();
		batcher.end();

		// this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true,
		// false);
		// RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		// RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
		// GlStateManager.DestFactor.ONE);
		// RenderSystem.depthMask(false);
		// RenderSystem.enableDepthTest();
		// RenderSystem.disableCull();
		// RenderSystem.enableBlend();
		// builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));

	}

	// private void renderStars(OrbitCamera.Cached camera,
	// Octree<Lazy<StarSystem.Info, StarSystem>> volume,
	// float partialTick) {
	// final var builder = BufferRenderer.immediateBuilder();

	// builder.begin(VertexFormat.Mode.QUADS,
	// DefaultVertexFormat.POSITION_COLOR_TEX);

	// volume.enumerateElements(element -> {
	// if (camera.pos.mul(TM_PER_UNIT).dot(element.pos) < 0)
	// return;
	// var distanceFromFocus = camera.pos.mul(TM_PER_UNIT).distanceTo(element.pos);
	// var alphaFactorFocus = 1 - Mth.clamp(distanceFromFocus / STAR_RENDER_RADIUS,
	// 0, 1);
	// if (alphaFactorFocus <= 0.05)
	// return;

	// var center = element.pos.div(TM_PER_UNIT);

	// // we should maybe consider doing the billboarding in a vertex shader,
	// because
	// // that way we can build all the geometry for a sector into a vertex buffer
	// and
	// // just emit a few draw calls, instead of having to build the buffer from
	// // scratch each frame.

	// var displayStar = element.value.getInitial().primaryStar;
	// RenderHelper.addBillboard(builder, camera, new PoseStack(), displayStar,
	// center);
	// });

	// builder.end();

	// this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true,
	// false);
	// RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
	// RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
	// GlStateManager.DestFactor.ONE);
	// RenderSystem.depthMask(false);
	// RenderSystem.enableDepthTest();
	// RenderSystem.disableCull();
	// RenderSystem.enableBlend();
	// builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));

	// builder.begin(VertexFormat.Mode.LINES,
	// DefaultVertexFormat.POSITION_COLOR_NORMAL);

	// var focusPos = camera.focus.div(TM_PER_UNIT);
	// volume.enumerateElements(element -> {
	// var pos = element.pos.div(TM_PER_UNIT);
	// if (pos.distanceTo(focusPos) > 10)
	// return;
	// RenderHelper.addLine(builder, camera,
	// Vec3.from(pos.x, focusPos.y, pos.z),
	// Vec3.from(pos.x, pos.y, pos.z),
	// NEAREST_LINE_COLOR.withA(0.2));
	// });

	// builder.end();
	// RenderSystem.enableBlend();
	// RenderSystem.disableTexture();
	// RenderSystem.defaultBlendFunc();
	// RenderSystem.disableCull();
	// RenderSystem.lineWidth(1.5f);
	// RenderSystem.depthMask(false);
	// builder.draw(GameRenderer.getRendertypeLinesShader());
	// }

	@Override
	public void tick() {
		super.tick();
		if (this.selectedSystemId != null)
			this.selectedSystemTicket.id = this.selectedSystemId;
		getLastCamera().ifSome(camera -> {
			if (this.cameraTicket.info instanceof SectorTicketInfo.Multi multi) {
				// multi.centerPos = camera.pos.mul(TM_PER_UNIT);
				multi.centerPos = getStarViewCenterPos(camera);
			}
		});
	}

	@Override
	public Cached setupCamera(float partialTick) {
		return this.camera.cached(partialTick);
	}

	@Override
	public void render3d(Cached camera, float partialTick) {

		final var builder = BufferRenderer.immediateBuilder();

		RenderHelper.renderGrid(builder, camera, TM_PER_UNIT, 1, 10, 40, partialTick);

		this.galaxyRenderingContext.build();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new PoseStack(), pos.div(TM_PER_UNIT), size / TM_PER_UNIT,
					Color.WHITE.withA(0.18));
		});

		builder.end();

		this.client.getTextureManager().getTexture(Mod.namespaced("textures/misc/galaxyglow.png")).setFilter(true,
				false);
		RenderSystem.setShaderTexture(0, Mod.namespaced("textures/misc/galaxyglow.png"));
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));

		renderStars(camera, partialTick);
		renderDebugLines(camera, partialTick);

		if (this.selectedSystemId != null) {
			// final var systemSector = this.selectedSystemId;
			// final var sectorPos = systemSector.levelCoords();
			this.galaxy.sectorManager.getInitial(this.selectedSystemId).ifSome(info -> {
				final var pos = info.pos().div(TM_PER_UNIT);
				final var size = 0.05 * camera.pos.distanceTo(pos);
				RenderHelper.renderBillboard(builder, camera, new PoseStack(), pos, size,
						Color.WHITE, RenderHelper.SELECTION_CIRCLE_ICON_LOCATION,
						GameRenderer.getPositionColorTexShader());
			});
		}

	}

	@Override
	public void render2d(PoseStack poseStack, float partialTick) {

		// sidebar

		// fillGradient(poseStack, 0, 0, 200, this.height, 0xff0a0a0a, 0xff0a0a0a);

		if (this.selectedSystemId != null) {
			final var systemSector = this.selectedSystemId;
			final var sectorPos = systemSector.levelCoords();

			// @formatter:off
			var systemId = "";
			if (sectorPos.x < 0) systemId += "M";
			systemId += Math.abs(sectorPos.x) + ".";
			if (sectorPos.y < 0) systemId += "M";
			systemId += Math.abs(sectorPos.y) + ".";
			if (sectorPos.z < 0) systemId += "M";
			systemId += Math.abs(sectorPos.z);
			systemId += ":";
			systemId += systemSector.level();
			systemId += "#";
			systemId += systemSector.elementIndex();
			// @formatter:on

			final var holder = new Object() {
				int height = 10;

				void emit(String text, int color) {
					client.font.draw(poseStack, text, 10, this.height, 0xffffffff);
					this.height += client.font.lineHeight;
				}
			};

			holder.emit("System " + systemId, 0xffffffff);
			this.galaxy.sectorManager.getInitial(this.selectedSystemId).ifSome(info -> {
				final var system = info.info();
				holder.emit(system.name, 0xffffffff);
				holder.height += 10;
				holder.emit(describeStar(system.primaryStar), 0xffffffff);
			});
		}

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
