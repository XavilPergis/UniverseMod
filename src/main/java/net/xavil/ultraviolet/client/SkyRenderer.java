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
import net.xavil.hawklib.client.gl.texture.GlTextureCubemap;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class SkyRenderer implements Disposable {

	public static final SkyRenderer INSTANCE = new SkyRenderer();
	private final Minecraft client = Minecraft.getInstance();

	public GlFramebuffer mainSkyTarget = null;

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
		if (this.mainSkyTarget != null) {
			this.mainSkyTarget.close();
			this.mainSkyTarget = null;
		}
	}

	private static Quat orientationFromMinecraftCamera(Camera camera) {
		final var px = new Vec3(camera.getLeftVector()).neg();
		final var py = new Vec3(camera.getUpVector());
		final var pz = px.cross(py);
		return Quat.fromOrthonormalBasis(px, py, pz);
	}

	private CachedCamera createCamera(Camera camera, TransformStack tfm,
			double nearPlane, double farPlane,
			float partialTick) {
		tfm.push();
		// double n = 0;
		// var offset = Vec3.ZERO;
		// // offset = new Vec3(0, 10000000, 0);
		// tfm.appendTranslation(offset);
		// n = ((System.nanoTime() / 1e8) % 100) / 100;
		// tfm.appendTranslation(new Vec3(0, 100000.0 * n, 0));
		tfm.prependRotation(orientationFromMinecraftCamera(camera).inverse());
		final var invView = tfm.copyCurrent();
		tfm.pop();

		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer,
				nearPlane, farPlane, false, partialTick);
		final var cam = new CachedCamera();
		cam.load(invView, proj, 1e12);
		return cam;
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
			this.starRenderer = new StarRenderManager(galaxy, new SectorTicketInfo.Multi(Vec3.ZERO,
					GalaxySector.BASE_SIZE_Tm, SectorTicketInfo.Multi.SCALES_EXP_ADJUSTED));
		this.starRenderer.setBatchingHint(StarRenderManager.BatchingHint.STATIC);
		this.starRenderer.setMode(StarRenderManager.Mode.REALISTIC);
		if (this.systemTicket == null)
			this.systemTicket = galaxy.sectorManager.createSystemTicketManual(null);

		if (this.galaxyRenderingContext == null)
			this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy.parameters);
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

	private GlTextureCubemap captureSkybox(Vec3 captureLocation) {
		final var cubemap = new GlTextureCubemap();
		cubemap.createStorage(GlTexture.Format.RGBA16_FLOAT, 1024);

		final var cam = new CachedCamera();
		final var projMat = Mat4.perspectiveProjection(0.5 * Math.PI, 1, 1e2, 1e10);

		try (final var disposer = Disposable.scope()) {
			final var framebuffer = disposer
					.attach(new GlFramebuffer(GlFragmentWrites.COLOR_ONLY, cubemap.size().d2()));
			framebuffer.enableAllColorAttachments();
			framebuffer.createDepthTarget(false, GlTexture.Format.DEPTH_UNSPECIFIED);
			for (final var face : GlTextureCubemap.Face.values()) {
				framebuffer.setColorTarget(GlFragmentWrites.COLOR, cubemap.createFaceTarget(face));
				framebuffer.checkStatus();
				framebuffer.bind();

				final var inverseViewMat = new Mat4.Mutable();
				switch (face) {
					case XN -> Mat4.setLookAt(inverseViewMat, Vec3.XN, captureLocation, Vec3.YP);
					case XP -> Mat4.setLookAt(inverseViewMat, Vec3.XP, captureLocation, Vec3.YP);
					case YN -> Mat4.setLookAt(inverseViewMat, Vec3.YN, captureLocation, Vec3.ZN);
					case YP -> Mat4.setLookAt(inverseViewMat, Vec3.YP, captureLocation, Vec3.ZP);
					case ZN -> Mat4.setLookAt(inverseViewMat, Vec3.ZN, captureLocation, Vec3.YP);
					case ZP -> Mat4.setLookAt(inverseViewMat, Vec3.ZP, captureLocation, Vec3.YP);
				};

				cam.load(inverseViewMat, projMat, 1e12);

				this.starRenderer.draw(cam, captureLocation);
			}
		}

		return cubemap;
	}

	private void drawCelestialObjects(Camera srcCamera, GlFramebuffer target, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse();

		target.bind();
		target.clear();
		// target.clearColorAttachment("fColor", new Color(0.1, 0.1, 0.12, 1));
		// target.clearColorAttachment("fColor", Color.BLACK);

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
			galaxyCamera.applyProjection();
			CachedCamera.applyView(galaxyCamera.orientation);
			if (this.galaxyRenderingContext != null) {
				this.galaxyRenderingContext.draw(galaxyCamera, Vec3.ZERO);
			}
			if (this.starRenderer != null) {
				profiler.popPush("stars");
				this.starRenderer.draw(galaxyCamera, galaxyCamera.posTm.xyz());
			}

			// system
			profiler.popPush("system");
			final var systemCamera = createCamera(srcCamera, cameraTransform, 1e-7, 1e3, partialTick);
			drawSystem(systemCamera, galaxy, partialTick);

			profiler.pop();
			snapshot.restore();
		}
		this.previousLocation = location;
	}

	private void drawSystem(CachedCamera camera, Galaxy galaxy, float partialTick) {
		final var system = this.systemTicket.forceLoad();
		if (system.isSome())
			drawSystem(camera, galaxy, system.unwrap(), partialTick);
	}

	private final PlanetRenderingContext planetContext = new PlanetRenderingContext();

	private void drawSystem(CachedCamera camera, Galaxy galaxy, StarSystem system,
			float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse();
		final var time = universe.getCelestialTime(partialTick);

		profiler.push("camera");
		camera.applyProjection();
		CachedCamera.applyView(camera.orientation);

		// system.rootNode.updatePositions(time);

		profiler.popPush("planet_context_setup");
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;

		final var modelTfm = new TransformStack();

		profiler.popPush("visit");
		this.planetContext.setSystemOrigin(system.pos);
		this.planetContext.begin(time);
		this.planetContext.setupLights(system, camera);
		system.rootNode.visit(node -> {
			modelTfm.push();

			if (node instanceof UnaryCelestialNode unaryNode) {
				final var radiusTm = ClientConfig.get(ConfigKey.PLANET_EXAGGERATION_FACTOR) * Units.Tu_PER_ku
						* unaryNode.radius;
				final var radiusUnits = radiusTm * (1e12 / camera.metersPerUnit);
				final var nodePosUnits = node.position.mul(1e12 / camera.metersPerUnit);

				// final var distanceRatio = radiusTm / camera.posTm.distanceTo(pos);
				// if (distanceRatio < 0.0001)
				// return;
				// final var offset = camera.posTm.mul(1e12 / camera.metersPerUnit);

				modelTfm.push();
				// modelTfm.appendRotation(Quat.axisAngle(Vec3.YP, -unaryNode.rotationalRate *
				// time));
				modelTfm.appendRotation(Quat.axisAngle(Vec3.XP, unaryNode.obliquityAngle));
				modelTfm.appendRotation(node.referencePlane.rotationFromReference());
				// modelTfm.appendScale(radiusUnits);
				modelTfm.appendTransform(Mat4.scale(radiusUnits));
				// modelTfm.appendTranslation(nodePosUnits.mul(camera.metersPerUnit / 1e12));
				// modelTfm.appendRotation(camera.orientation.inverse());
				modelTfm.appendTranslation(camera.toCameraSpace(nodePosUnits));

				final var profiler2 = Minecraft.getInstance().getProfiler();
				profiler2.push("id:" + node.getId());
				if (EntityAccessor.getWorldType(this.client.player) instanceof WorldType.SystemNode loc) {
					final var skip = loc.id.nodeId() == node.getId();
					this.planetContext.render(builder, camera, unaryNode, modelTfm, skip);
				} else {
					this.planetContext.render(builder, camera, unaryNode, modelTfm, false);
				}
				profiler2.pop();
			}
			modelTfm.pop();
		});

		this.planetContext.end();
		profiler.pop();
	}

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta,
			Camera camera, boolean isSkyVisible) {

		final var worldType = EntityAccessor.getWorldType(this.client.player);
		if (worldType == null)
			return false;

		final var profiler = Minecraft.getInstance().getProfiler();

		profiler.push("pushGlState");
		GlManager.pushState();
		profiler.pop();

		profiler.push("fboSetup");
		if (this.mainSkyTarget == null) {
			Mod.LOGGER.info("creating SkyRenderer framebuffer");
			this.mainSkyTarget = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY);
			this.mainSkyTarget.createColorTarget(GlFragmentWrites.COLOR, GlTexture.Format.RGBA16_FLOAT);
			this.mainSkyTarget.createDepthTarget(false, GlTexture.Format.DEPTH_UNSPECIFIED);
			this.mainSkyTarget.enableAllColorAttachments();
			this.mainSkyTarget.checkStatus();
		}
		profiler.pop();

		// final var mainTarget = GlFramebuffer.getMainFramebuffer();

		profiler.push("draw");
		final var partialTick = this.client.isPaused() ? 0 : this.client.getFrameTime();
		drawCelestialObjects(camera, this.mainSkyTarget, partialTick);
		// drawCelestialObjects(camera, mainTarget, partialTick);
		profiler.pop();

		// profiler.push("postprocess");
		// final var sceneTexture =
		// this.hdrSpaceTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		// HawkRendering.applyPostProcessing(GlFramebuffer.MAIN, sceneTexture);
		// profiler.pop();

		// TODO: draw atmosphere or something
		// TODO: figure out how the fuck we're gonna make vanilla fog not look like
		// total ass
		// could maybe replace the vanilla fog shader with one that takes in a
		// background image buffer and uses that as the fog color. idk.

		// profiler.push("clear");
		// mainTarget.bind();
		// // clear depth because celestial objects use a totally different coordinate
		// // system than the actual minecraft world, so we would potentially get
		// planets
		// // drawn in front of terrain...
		// mainTarget.clearDepthAttachment(1.0f);
		// profiler.pop();

		profiler.push("popGlState");
		GlManager.popState();
		profiler.pop();

		return true;
	}

	public GlTexture2d compositeMainWorld(GlFramebuffer mainTarget) {
		final var mainImage = mainTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		if (this.mainSkyTarget == null)
			return mainImage;

		this.mainSkyTarget.bind();
		final var shader = UltravioletShaders.SHADER_UN_VANILLA.get();
		shader.setUniformSampler("uSampler", mainImage);
		BufferRenderer.drawFullscreen(shader);

		return this.mainSkyTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
	}

	@Override
	public void close() {
		this.planetContext.close();
		if (this.mainSkyTarget != null)
			this.mainSkyTarget.close();
		if (this.starRenderer != null)
			this.starRenderer.close();
		if (this.galaxyRenderingContext != null)
			this.galaxyRenderingContext.close();
	}

}
