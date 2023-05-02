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
import net.minecraft.util.Mth;
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
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Option;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.Vec2;
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
		final var right = rotation.transform(v1.cross(forward).neg());
		final var up = forward.cross(right).neg();
		// RenderHelper.addBillboardCamspace(builder, up, right, forward.mul(100), s, 0,
		// Color.WHITE.withA(0.1));
		RenderHelper.addBillboardCamspace(builder, up, right, offset, offset.length() * s, Color.WHITE.withA(0.1));
	}

	private CachedCamera<?> createCamera(Camera camera, TransformStack tfm, float partialTick) {
		double n = 0;
		var offset = Vec3.ZERO;
		// n = ((System.nanoTime() / 100000) % 1000000) / 1e0;
		// offset = Vec3.from(0, 10000000, 0);
		tfm.push();
		tfm.appendTranslation(Vec3.from(n, 0.01 * n, 0));
		tfm.appendTranslation(offset);
		tfm.prependRotation(CachedCamera.orientationFromMinecraftCamera(camera).inverse());
		final var invView = tfm.get();
		tfm.pop();
		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer,
				0.000000001f, 1e5f, false, partialTick);
		return new CachedCamera<>(camera, invView, proj, 1e12);
	}

	private void applyPlanetTransform(TransformStack tfm, CelestialNode node, double time, Vec2 coords, float partialTick) {
 		final var worldBorder = this.client.level.getWorldBorder();
		final var tx = Mth.inverseLerp(coords.x, worldBorder.getMinX(), worldBorder.getMaxX());
		final var tz = Mth.inverseLerp(coords.y, worldBorder.getMinZ(), worldBorder.getMaxZ());

		if (node instanceof PlanetaryCelestialNode planetNode) {
			final var planetRadius = 1.001 * planetNode.radiusRearth * (Units.m_PER_Rearth / Units.TERA);
			tfm.appendRotation(Quat.axisAngle(Vec3.XP, Math.PI / 2).inverse());
			tfm.appendTranslation(Vec3.ZN.mul(planetRadius));
		}

		final var halfPi = Math.PI / 2;
		final var latitudeOffset = -Mth.clampedLerp(-halfPi, halfPi, tx);
		final var longitudeOffset = Mth.clampedLerp(-halfPi, halfPi, tz);

		final var rotationalSpeed = -2 * Math.PI / node.rotationalPeriod;

		tfm.appendRotation(Quat.axisAngle(Vec3.YP, latitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, longitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.YP, rotationalSpeed * time));
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));
		
		tfm.appendTranslation(node.getPosition(partialTick));

	}

	private Option<CachedCamera<?>> createCamera(Camera camera, Location location, float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);
		final var tfm = new TransformStack();
		if (location instanceof Location.World loc) {
			return universe.getSystem(loc.id.system()).flatMap(sys -> {
				final var node = sys.rootNode.lookup(loc.id.nodeId());
				if (node == null)
					return Option.none();
				final var srcCamPos = Vec3.from(camera.getPosition());
				// var nodePos = sys.pos.add(node.getPosition(partialTick));
				// final var quat = toCelestialWorldSpaceRotation(node, time, srcCamPos.xz());
				// if (node instanceof PlanetaryCelestialNode planetNode) {
				// 	nodePos = nodePos.add(getPlanetSurfaceOffset(planetNode, quat));
				// }
				applyPlanetTransform(tfm, node, time, srcCamPos.xz(), partialTick);
				tfm.appendTranslation(sys.pos);
				return Option.some(createCamera(camera, tfm, partialTick));
			});
		} else if (location instanceof Location.Station loc) {
			return universe.getStation(loc.id).flatMap(station -> {
				final var p = station.getPos(partialTick);
				tfm.appendRotation(station.orientation.inverse());
				tfm.appendTranslation(p);
				return Option.some(createCamera(camera, tfm, partialTick));
			});
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

		// render
		createCamera(srcCamera, location, partialTick).ifSome(camera -> {
			final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
			final var snapshot = camera.setupRenderMatrices();
			if (galaxy != null) {
				profiler.push("galaxy");
				drawGalaxy(camera, galaxy, partialTick);
				profiler.popPush("stars");
				drawStars(camera, galaxy, partialTick);
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
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new TransformStack(),
					pos,
					0.4 * size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.1));
		});

		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.GALAXY_GLOW_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.GALAXY_GLOW_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
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
			final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			// if (!camera.isAabbInFrustum(min, max))
			// 	return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(camPos) > levelSize)
					return;
				final var toStar = elem.pos().sub(camPos);
				if (toStar.dot(camera.forward) >= 0)
					return;
				// if (elem.info().primaryStar.luminosityLsol < 1)
				// return;
				batcher.add(elem.info().primaryStar, elem.pos());
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
			if (EntityAccessor.getLocation(this.client.player) instanceof Location.World loc) {
				final var skip = loc.id.nodeId() == node.getId();
				ctx.render(builder, camera, node, false);
			} else {
				ctx.render(builder, camera, node, false);
			}
			profiler2.pop();
		});

		ctx.end();
		profiler.pop();
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera,
			boolean isSkyVisible) {
		final var profiler = Minecraft.getInstance().getProfiler();

		final var partialTick = this.client.isPaused() ? 0 : this.client.getFrameTime();

		final var compositeTarget = getSkyCompositeTarget();
		useMultisampling = true;
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
