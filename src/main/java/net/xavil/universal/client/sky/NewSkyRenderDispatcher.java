package net.xavil.universal.client.sky;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.xavil.universal.client.GalaxyRenderingContext;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.PlanetRenderingContext;
import net.xavil.universal.client.camera.CachedCamera;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleRenderTarget;
import net.xavil.universal.client.flexible.FlexibleVertexConsumer;
import net.xavil.universal.client.screen.BillboardBatcher;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.galaxy.SectorTicket;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.common.universe.station.StationLocation;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.GalaxyTicket;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Option;
import net.xavil.util.Rng;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public class NewSkyRenderDispatcher {

	public static final NewSkyRenderDispatcher INSTANCE = new NewSkyRenderDispatcher();

	private final Minecraft client = Minecraft.getInstance();

	public FlexibleRenderTarget skyTarget = null;
	public boolean useMultisampling = false;
	// public PostChain compositeChain = null;

	private Location previousLocation = null;
	private GalaxyRenderingContext galaxyRenderingContext = null;
	private long galaxyRenderingSeed = 314159265358979L; // :p

	private SectorTicket<SectorTicketInfo.Multi> sectorTicket = null;
	private SystemTicket systemTicket = null;
	private GalaxyTicket galaxyTicket = null;

	public void resize(int width, int height) {
		if (this.skyTarget != null) {
			this.skyTarget.resize(width, height, false);
		}
	}

	public void setMultisampled(boolean useMultisampling) {
		if (this.useMultisampling != useMultisampling) {
			this.useMultisampling = useMultisampling;
			this.skyTarget = null;
		}
	}

	// public FlexibleRenderTarget getSkyTarget() {
	// if (this.skyTarget == null) {
	// final var window = this.client.getWindow();
	// final var format = new FlexibleRenderTarget.FormatPair(this.useMultisampling,
	// GL32.GL_RGBA32F,
	// OptionalInt.of(GL32.GL_DEPTH_COMPONENT32));
	// this.skyTarget = new FlexibleRenderTarget(window.getWidth(),
	// window.getHeight(), format);
	// }
	// return this.skyTarget;
	// }

	public RenderTarget getSkyCompositeTarget() {
		return ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).getTempTarget("sky");
	}

	private void addBackgroundBillboard(FlexibleVertexConsumer builder, Rng rng, Vec3 camPos, Vec3 pos, double s) {
		final var offset = pos.sub(camPos);
		final var forward = offset.normalize();

		final var rotationAngle = rng.uniformDouble(0, 2.0 * Math.PI);
		final var rotation = Quat.axisAngle(forward, rotationAngle);

		final var du = forward.dot(Vec3.YP);
		final var df = forward.dot(Vec3.ZN);
		final var v1 = Math.abs(du) < Math.abs(df) ? Vec3.YP : Vec3.ZN;
		final var right = rotation.transform(v1.cross(forward));
		final var up = forward.cross(right).neg();
		// RenderHelper.addBillboardCamspace(builder, up, right, forward.mul(100), s, 0,
		// Color.WHITE.withA(0.1));
		RenderHelper.addBillboardCamspace(builder, up, right, offset, offset.length() * s, 0, Color.WHITE.withA(0.1));
	}

	private CachedCamera<?> createCamera(Camera camera, Vec3 celestialPos, float partialTick) {
		// final var quat =
		// CachedCamera.orientationFromMinecraftCamera(camera).hamiltonProduct(planetViewRotation);
		final var quat = CachedCamera.orientationFromMinecraftCamera(camera);

		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer, 0.0001f, 1e9f, false,
				partialTick);

		// final var n = ((System.nanoTime() / 100000) % 1000000) / 0.1;
		// return CachedCamera.create(camera, celestialPos.add(n, 0.2 * n,
		// 1e4).div(1e12), quat, 1e12, 1e12, proj);
		return CachedCamera.create(camera, celestialPos, quat, 1e12, proj);
	}

	private Option<Vec3> posForLocation(Location location, float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		if (location instanceof Location.World loc) {
			return universe.getSystem(loc.id.system()).flatMap(sys -> {
				// sys.rootNode.updatePositions(galaxyRenderingSeed);
				final var node = sys.rootNode.lookup(loc.id.nodeId());
				if (node == null)
					return Option.none();
				return Option.some(sys.pos.add(node.getPosition(partialTick)));
			});
		} else if (location instanceof Location.Station loc) {
			return universe.getStation(loc.id).flatMap(station -> Option.fromNullable(station.getLocation().getPos()));
		}
		return Option.none();
	}

	private void createTickets(Universe universe, UniverseSectorId galaxyId) {
		if (this.galaxyTicket == null)
			this.galaxyTicket = universe.sectorManager.createGalaxyTicketManual(galaxyId);
		final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
		if (galaxy == null)
			return;
		if (this.systemTicket == null)
			this.systemTicket = galaxy.sectorManager.createSystemTicketManual(null);
		if (this.sectorTicket == null)
			this.sectorTicket = galaxy.sectorManager.createSectorTicketManual(null);

		if (this.galaxyRenderingContext == null)
			this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy);
	}

	private void disposeTickets() {
		if (this.galaxyTicket != null) {
			this.galaxyTicket.dispose();
			this.galaxyTicket = null;
		}
		if (this.systemTicket != null) {
			this.systemTicket.dispose();
			this.systemTicket = null;
		}
		if (this.sectorTicket != null) {
			this.sectorTicket.dispose();
			this.sectorTicket = null;
		}

		if (this.galaxyRenderingContext != null) {
			this.galaxyRenderingContext = null;
		}
	}

	private void drawSky(Camera srcCamera, RenderTarget target, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);

		target.bindWrite(false);
		target.setClearColor(0, 0, 0, 0);
		target.clear(false);
		target.bindWrite(false);

		// ticket management
		final var location = EntityAccessor.getLocation(this.client.player);
		if (!Objects.equals(previousLocation, location))
			disposeTickets();

		if (location instanceof Location.Station loc) {
			universe.getStation(loc.id).ifSome(station -> {
				if (station.getLocation() instanceof StationLocation.OrbitingCelestialBody sloc) {
					createTickets(universe, sloc.id.universeSector());
					this.systemTicket.id = sloc.id.galaxySector();
				} else if (station.getLocation() instanceof StationLocation.JumpingSystem sloc) {
					createTickets(universe, sloc.targetNode.universeSector());
					this.systemTicket.id = sloc.targetNode.galaxySector();
				}
			});
		} else if (location instanceof Location.World loc) {
			createTickets(universe, loc.id.universeSector());
			this.systemTicket.id = loc.id.galaxySector();
		}
		
		if (this.galaxyTicket == null)
		return;

		// if (this.systemTicket != null)
		// 	this.systemTicket.attachedManager.getSystem(this.systemTicket.id).ifSome(system -> {
		// 		system.rootNode.updatePositions(time);
		// 	});

		// render
		posForLocation(location, partialTick).ifSome(pos -> {
			final var camera = createCamera(srcCamera, pos, partialTick);
			final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
			final var snapshot = camera.setupRenderMatrices();
			if (galaxy != null) {
				profiler.push("galaxy");
				drawGalaxy(camera, galaxy, partialTick);
				profiler.popPush("stars");
				// drawStars(camera, galaxy, partialTick);
				profiler.popPush("system");
				drawSystem(camera, galaxy, partialTick);
				profiler.pop();
			}
			snapshot.restore();
		});

		this.previousLocation = location;
	}

	private void drawGalaxy(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		final var rng = Rng.wrap(new Random(1337));

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		this.galaxyRenderingContext.build();
		this.galaxyRenderingContext.enumerate((pos, s) -> {
			addBackgroundBillboard(builder, rng, camera.pos, pos.mul(1e12 / camera.metersPerUnit), 0.1 * s / 3e7);
		});

		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.GALAXY_GLOW_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.GALAXY_GLOW_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));

	}

	private void drawStars(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		final var batcher = new BillboardBatcher(builder, 10000);

		final var camPos = camera.pos.mul(camera.metersPerUnit / 1e12);

		if (this.sectorTicket.info == null)
			this.sectorTicket.info = SectorTicketInfo.visual(camPos);
		this.sectorTicket.info.centerPos = camPos;
		this.sectorTicket.info.multiplicitaveFactor = 2.0;

		batcher.begin(camera);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			// if (sector.pos().level() != 0) return;
			final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			// if (!camera.isAabbInFrustum(min, max))
			// return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(camPos) > levelSize)
					return;
				final var toStar = elem.pos().sub(camPos);
				if (toStar.dot(camera.forward) <= 0)
					return;
				if (elem.info().primaryStar.luminosityLsol < 1)
					return;
				batcher.add(elem.info().primaryStar, toStar);
			});
		});
		batcher.end();
	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var system = this.systemTicket.forceLoad();
		if (system.isSome())
			drawSystem(camera, galaxy, system.unwrap(), partialTick);
	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, StarSystem system, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);

		profiler.push("camera");
		camera.setupRenderMatrices();

		// system.rootNode.updatePositions(time);

		profiler.popPush("planet_context_setup");
		final var builder = BufferRenderer.immediateBuilder();
		final var ctx = new PlanetRenderingContext();
		system.rootNode.visit(node -> {
			if (node instanceof StellarCelestialNode starNode) {
				var light = PlanetRenderingContext.PointLight.fromStar(starNode);
				ctx.pointLights.add(light);
			}
		});

		profiler.popPush("visit");
		ctx.setSystemOrigin(system.pos);
		ctx.begin(time);
		system.rootNode.visit(node -> {
			final var profiler2 = Minecraft.getInstance().getProfiler();
			profiler2.push("id:" + node.getId());
			// ctx.render(builder, camera, node, new PoseStack(), Color.WHITE,
			// node.getId() == currentNodeId.nodeId());
			ctx.render(builder, camera, node, false);
			profiler2.pop();
		});

		ctx.end();
		profiler.pop();
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera,
			boolean isSkyVisible) {
		final var profiler = Minecraft.getInstance().getProfiler();

		final var partialTick = this.client.getFrameTime();

		final var compositeTarget = getSkyCompositeTarget();
		if (this.useMultisampling) {
			if (this.skyTarget == null) {
				final var window = this.client.getWindow();
				final var format = new FlexibleRenderTarget.FramebufferFormat(this.useMultisampling, GL32.GL_RGBA32F,
						OptionalInt.of(GL32.GL_DEPTH_COMPONENT32));
				this.skyTarget = new FlexibleRenderTarget(window.getWidth(), window.getHeight(), format);
			}
			drawSky(camera, this.skyTarget, partialTick);
			profiler.push("resolve");
			this.skyTarget.resolveTo(compositeTarget);
			profiler.pop();
		} else {
			drawSky(camera, compositeTarget, partialTick);
		}

		final var mainTarget = this.client.getMainRenderTarget();

		profiler.push("composite");
		mainTarget.setClearColor(0f, 0f, 0f, 1.0f);
		ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).process(partialTick);
		mainTarget.bindWrite(false);
		profiler.pop();

		// a bit scuffed...
		GlStateManager._clearDepth(1.0);
		GlStateManager._clear(GL32.GL_DEPTH_BUFFER_BIT, false);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();

		return true;
	}

}
