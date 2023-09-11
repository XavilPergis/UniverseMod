package net.xavil.ultraviolet.client;

import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public class SkyRenderer implements Disposable {

	public static final SkyRenderer INSTANCE = new SkyRenderer();
	private final Minecraft client = Minecraft.getInstance();

	public GlFramebuffer hdrSpaceTarget = null;
	public GlFramebuffer postProcessTarget = null;

	private WorldType previousLocation = null;

	private SystemTicket systemTicket = null;
	private GalaxyTicket galaxyTicket = null;

	private StarRenderManager starRenderer = null;

	private GalaxyRenderingContext galaxyRenderingContext = null;

	private SkyRenderer() {
	}

	public void tick() {
	}

	public void resize(int width, int height) {
		if (this.hdrSpaceTarget != null) {
			this.hdrSpaceTarget.close();
			this.hdrSpaceTarget = null;
		}
		if (this.postProcessTarget != null) {
			this.postProcessTarget.resize(new Vec2i(width, height));
		}
	}

	private CachedCamera<?> createCamera(Camera camera, TransformStack tfm,
			double nearPlane, double farPlane,
			float partialTick) {
		double n = 0;
		var offset = Vec3.ZERO;
		// n = ((System.nanoTime() / 1e8) % 100) / 100;
		// offset = Vec3.from(0, 10000000, 0);
		// tfm = new TransformStack();
		tfm.push();
		tfm.appendTranslation(new Vec3(n, 0.01 * n, 0));
		tfm.appendTranslation(offset);
		tfm.prependRotation(CachedCamera.orientationFromMinecraftCamera(camera).inverse());
		final var invView = tfm.copyCurrent();
		tfm.pop();

		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer,
				nearPlane, farPlane, false, partialTick);
		return new CachedCamera<>(camera, invView, proj, 1e12, nearPlane, farPlane);
	}

	private void applyPlanetTransform(TransformStack tfm, PlanetaryCelestialNode node, double time, Vec2 coords,
			float partialTick) {
		final var worldBorder = this.client.level.getWorldBorder();
		final var tx = Mth.inverseLerp(coords.x, worldBorder.getMinX(), worldBorder.getMaxX());
		final var tz = Mth.inverseLerp(coords.y, worldBorder.getMinZ(), worldBorder.getMaxZ());

		final var planetRadius = 1.0001 * Units.Tu_PER_ku * node.radius;
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, Math.PI / 2).inverse());
		tfm.appendTranslation(Vec3.ZN.mul(planetRadius));

		final var halfPi = Math.PI / 2;
		final var latitudeOffset = -Mth.clampedLerp(-halfPi, halfPi, tx);
		final var longitudeOffset = Mth.clampedLerp(-halfPi, halfPi, tz);

		tfm.appendRotation(Quat.axisAngle(Vec3.XP, longitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.YP, latitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.YP, -node.rotationalRate * time));
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));

		tfm.appendTranslation(node.getPosition(partialTick));
	}

	public boolean applyCelestialTransform(TransformStack tfm, Vec2 worldPosXZ, WorldType location, float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse();
		final var time = universe.getCelestialTime(partialTick);
		if (location instanceof WorldType.SystemNode loc) {
			final var sys = universe.getSystem(loc.id.system()).unwrapOrNull();
			if (sys == null)
				return false;
			final var node = sys.rootNode.lookup(loc.id.nodeId());
			if (node == null)
				return false;
			if (node instanceof PlanetaryCelestialNode planetNode) {
				applyPlanetTransform(tfm, planetNode, time, worldPosXZ, partialTick);
				tfm.appendTranslation(sys.pos);
				return true;
			}
			return false;
		} else if (location instanceof WorldType.Station loc) {
			final var station = universe.getStation(loc.id).unwrapOrNull();
			if (station == null)
				return false;
			final var p = station.getPos(partialTick);
			tfm.appendRotation(station.orientation.inverse());
			tfm.appendTranslation(p);
			return true;
		}
		return false;
	}

	private void createTickets(Universe universe, UniverseSectorId galaxyId) {
		if (this.galaxyTicket == null)
			this.galaxyTicket = universe.sectorManager.createGalaxyTicketManual(galaxyId);
		final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
		if (galaxy == null)
			return;
		if (this.starRenderer == null)
			this.starRenderer = new StarRenderManager(galaxy, SectorTicketInfo.visual(Vec3.ZERO));
		this.starRenderer.setBatchingHint(StarRenderManager.BatchingHint.STATIC);
		this.starRenderer.setMode(StarRenderManager.Mode.REALISTIC);
		if (this.systemTicket == null)
			this.systemTicket = galaxy.sectorManager.createSystemTicketManual(null);

		if (this.galaxyRenderingContext == null)
			this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy.densityFields);
	}

	public void disposeTickets() {
		if (this.galaxyTicket != null) {
			this.galaxyTicket.close();
			this.galaxyTicket = null;
		}
		if (this.systemTicket != null) {
			this.systemTicket.close();
			this.systemTicket = null;
		}
		if (this.starRenderer != null) {
			this.starRenderer.close();
			this.starRenderer = null;
		}
		if (this.galaxyRenderingContext != null) {
			this.galaxyRenderingContext.close();
			this.galaxyRenderingContext = null;
		}
	}

	private void drawCelestialObjects(Camera srcCamera, GlFramebuffer target, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse();

		target.bind();
		target.clear();
		// target.clearColorAttachment("fColor", new Color(0.1, 0.1, 0.12, 1));
		target.clearColorAttachment("fColor", Color.BLACK);

		// ticket management
		final var location = EntityAccessor.getWorldType(this.client.player);
		if (!Objects.equals(previousLocation, location))
			disposeTickets();

		if (location instanceof WorldType.Station loc) {
			final var station = universe.getStation(loc.id).unwrapOrNull();
			if (station != null) {
				if (station.getLocation() instanceof StationLocation.OrbitingCelestialBody sloc) {
					createTickets(universe, sloc.id.universeSector());
					this.systemTicket.id = sloc.id.galaxySector();
				} else if (station.getLocation() instanceof StationLocation.JumpingSystem sloc) {
					createTickets(universe, sloc.targetNode.universeSector());
					this.systemTicket.id = sloc.targetNode.galaxySector();
				}
			}
		} else if (location instanceof WorldType.SystemNode loc) {
			createTickets(universe, loc.id.universeSector());
			this.systemTicket.id = loc.id.galaxySector();
		}

		if (this.galaxyTicket == null)
			return;

		final var cameraTransform = new TransformStack();
		final var xz = new Vec2(srcCamera.getPosition().x, srcCamera.getPosition().z);
		applyCelestialTransform(cameraTransform, xz, location, partialTick);
		
		// render
		final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
		if (galaxy != null) {
			final var snapshot = RenderMatricesSnapshot.capture();

			// galaxy
			profiler.push("galaxy");
			final var galaxyCamera = createCamera(srcCamera, cameraTransform, 1e2, 1e10, partialTick);
			galaxyCamera.setupRenderMatrices();
			if (this.galaxyRenderingContext != null) {
				this.galaxyRenderingContext.draw(galaxyCamera, Vec3.ZERO);
			}
			if (this.starRenderer != null) {
				profiler.popPush("stars");
				this.starRenderer.draw(galaxyCamera, galaxyCamera.posTm);
			}

			// system
			profiler.popPush("system");
			final var systemCamera = createCamera(srcCamera, cameraTransform, 1e-6, 1e3, partialTick);
			drawSystem(systemCamera, galaxy, partialTick);

			profiler.pop();
			snapshot.restore();
		}
		this.previousLocation = location;
	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var system = this.systemTicket.forceLoad();
		if (system.isSome())
			drawSystem(camera, galaxy, system.unwrap(), partialTick);
	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, StarSystem system,
			float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse();
		final var time = universe.getCelestialTime(partialTick);

		profiler.push("camera");
		camera.setupRenderMatrices();

		// system.rootNode.updatePositions(time);

		profiler.popPush("planet_context_setup");
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var ctx = new PlanetRenderingContext();

		profiler.popPush("visit");
		ctx.setSystemOrigin(system.pos);
		ctx.begin(system, time);
		system.rootNode.visit(node -> {
			final var profiler2 = Minecraft.getInstance().getProfiler();
			profiler2.push("id:" + node.getId());
			if (EntityAccessor.getWorldType(this.client.player) instanceof WorldType.SystemNode loc) {
				final var skip = loc.id.nodeId() == node.getId();
				ctx.render(builder, camera, node, skip);
			} else {
				ctx.render(builder, camera, node, false);
			}
			profiler2.pop();
		});

		ctx.end();
		profiler.pop();
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta,
			Camera camera, boolean isSkyVisible) {

		final var profiler = Minecraft.getInstance().getProfiler();

		profiler.push("pushGlState");
		GlManager.pushState();
		profiler.pop();

		profiler.push("fboSetup");
		if (this.hdrSpaceTarget == null) {
			Mod.LOGGER.info("creating SkyRenderer framebuffer");
			this.hdrSpaceTarget = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY);
			this.hdrSpaceTarget.createColorTarget(GlFragmentWrites.COLOR, GlTexture.Format.RGBA16_FLOAT);
			this.hdrSpaceTarget.createDepthTarget(false, GlTexture.Format.DEPTH_UNSPECIFIED);
			this.hdrSpaceTarget.enableAllColorAttachments();
			this.hdrSpaceTarget.checkStatus();
		}
		profiler.pop();

		profiler.push("aaa");
		final var mainTarget = new GlFramebuffer(this.client.getMainRenderTarget());
		mainTarget.enableAllColorAttachments();
		profiler.pop();

		profiler.push("draw");
		final var partialTick = this.client.isPaused() ? 0 : this.client.getFrameTime();
		drawCelestialObjects(camera, this.hdrSpaceTarget, partialTick);
		// drawCelestialObjects(camera, mainTarget, partialTick);
		profiler.pop();

		final var sceneTexture = this.hdrSpaceTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		HawkRendering.doPostProcessing(mainTarget, sceneTexture);

		// TODO: draw atmosphere or something
		// TODO: figure out how the fuck we're gonna make vanilla fog not look like
		// total ass
		// could maybe replace the vanilla fog shader with one that takes in a
		// background image buffer and uses that as the fog color. idk.

		profiler.push("clear");
		mainTarget.bind();
		mainTarget.clearDepthAttachment(1.0f);
		// mainTarget.enableAllColorAttachments();
		profiler.pop();

		profiler.push("popGlState");
		GlManager.popState();
		profiler.pop();

		return true;
	}

	@Override
	public void close() {
		if (this.hdrSpaceTarget != null)
			this.hdrSpaceTarget.close();
		if (this.postProcessTarget != null)
			this.postProcessTarget.close();
		if (this.starRenderer != null)
			this.starRenderer.close();
		if (this.galaxyRenderingContext != null)
			this.galaxyRenderingContext.close();
	}

}
