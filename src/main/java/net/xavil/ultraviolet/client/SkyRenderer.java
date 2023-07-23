package net.xavil.ultraviolet.client;

import java.util.Objects;
import java.util.Random;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlCubemapTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public class SkyRenderer implements Disposable {

	public static final SkyRenderer INSTANCE = new SkyRenderer();
	private final Minecraft client = Minecraft.getInstance();

	public boolean useMultisampling = false;
	public GlFramebuffer hdrSpaceTarget = null;
	public GlFramebuffer postProcessTarget = null;

	private Location previousLocation = null;

	private SystemTicket systemTicket = null;
	private GalaxyTicket galaxyTicket = null;

	private StarRenderManager starRenderer = null;

	private GalaxyRenderingContext galaxyRenderingContext = null;
	private GlCubemapTexture galaxyCubemap = new GlCubemapTexture();
	private boolean shouldRenderGalaxyToCubemap = true;

	private SkyRenderer() {
		this.galaxyCubemap.createStorage(GlTexture.Format.RGBA32_FLOAT, 1024);
	}

	public void tick() {
		if (this.starRenderer != null)
			this.starRenderer.tick();
	}

	public void resize(int width, int height) {
		if (this.hdrSpaceTarget != null) {
			this.hdrSpaceTarget.close();
			this.hdrSpaceTarget = null;
			// this.hdrSpaceTarget.resize(new Vec2i(width, height));
		}
		if (this.postProcessTarget != null) {
			this.postProcessTarget.resize(new Vec2i(width, height));
		}
	}

	public void setMultisampled(boolean useMultisampling) {
		if (this.useMultisampling != useMultisampling) {
			this.useMultisampling = useMultisampling;
			this.hdrSpaceTarget = null;
		}
	}

	// public RenderTarget getSkyCompositeTarget() {
	// return
	// ModRendering.getPostChain(ModRendering.COMPOSITE_SKY_CHAIN).getTempTarget("sky");
	// }

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
		// n = ((System.nanoTime() / 1e8) % 100) / 100;
		// offset = Vec3.from(0, 10000000, 0);
		// tfm = new TransformStack();
		tfm.push();
		tfm.appendTranslation(Vec3.from(n, 0.01 * n, 0));
		tfm.appendTranslation(offset);
		tfm.prependRotation(CachedCamera.orientationFromMinecraftCamera(camera).inverse());
		final var invView = tfm.get();
		tfm.pop();

		final var nearPlane = ClientConfig.get(ConfigKey.SKY_CAMERA_NEAR_PLANE);
		final var farPlane = ClientConfig.get(ConfigKey.SKY_CAMERA_FAR_PLANE);
		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer,
				nearPlane.floatValue(), farPlane.floatValue(), false, partialTick);
		return new CachedCamera<>(camera, invView, proj, 1e12, nearPlane, farPlane);
	}

	private CachedCamera<?> createCubemapCamera(Object camera, TransformStack tfm, float partialTick) {
		final var invView = tfm.get();
		final var nearPlane = ClientConfig.get(ConfigKey.SKY_CAMERA_NEAR_PLANE);
		final var farPlane = ClientConfig.get(ConfigKey.SKY_CAMERA_FAR_PLANE);
		final var proj = Mat4.perspectiveProjection(Math.PI, 1, nearPlane, farPlane);
		return new CachedCamera<>(camera, invView, proj, 1e12, nearPlane, farPlane);
	}

	private void applyPlanetTransform(TransformStack tfm, CelestialNode node, double time, Vec2 coords,
			float partialTick) {
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
		final var longitudeOffset = Mth.clampedLerp(-Math.PI, Math.PI, tz);

		final var rotationalSpeed = -2 * Math.PI / node.rotationalPeriod;

		tfm.appendRotation(Quat.axisAngle(Vec3.YP, latitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, longitudeOffset));
		tfm.appendRotation(Quat.axisAngle(Vec3.YP, rotationalSpeed * time));
		tfm.appendRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));

		tfm.appendTranslation(node.getPosition(partialTick));
	}

	private void applyFaceDir(TransformStack tfm, Vec3 faceDir) {
		if (faceDir == Vec3.ZN)
			tfm.prependRotation(Quat.axisAngle(Vec3.YP, 0));
		if (faceDir == Vec3.ZP)
			tfm.prependRotation(Quat.axisAngle(Vec3.YP, Math.PI));
		if (faceDir == Vec3.XN)
			tfm.prependRotation(Quat.axisAngle(Vec3.YP, Math.PI / 2));
		if (faceDir == Vec3.XP)
			tfm.prependRotation(Quat.axisAngle(Vec3.YP, -Math.PI / 2));
		if (faceDir == Vec3.YN)
			tfm.prependRotation(Quat.axisAngle(Vec3.XP, Math.PI / 2));
		if (faceDir == Vec3.YP)
			tfm.prependRotation(Quat.axisAngle(Vec3.XP, -Math.PI / 2));
	}

	private boolean applyGalaxyTranslation(TransformStack tfm, Location location, float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		if (location instanceof Location.World loc) {
			final var sys = universe.getSystem(loc.id.system()).unwrapOrNull();
			if (sys == null)
				return false;
			final var node = sys.rootNode.lookup(loc.id.nodeId());
			if (node == null)
				return false;
			tfm.appendTranslation(node.getPosition(partialTick));
			tfm.appendTranslation(sys.pos);
			return true;
		} else if (location instanceof Location.Station loc) {
			final var station = universe.getStation(loc.id).unwrapOrNull();
			if (station == null)
				return false;
			final var p = station.getPos(partialTick);
			tfm.appendTranslation(p);
			return true;
		}
		return false;
	}

	private Maybe<CachedCamera<?>> createCubemapCamera(CachedCamera<?> srcCamera, Location location, Vec3 faceDir,
			float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);
		final var tfm = new TransformStack();
		if (location instanceof Location.World loc) {
			return universe.getSystem(loc.id.system()).flatMap(sys -> {
				final var node = sys.rootNode.lookup(loc.id.nodeId());
				if (node == null)
					return Maybe.none();
				tfm.appendTranslation(node.getPosition(partialTick));
				tfm.appendTranslation(sys.pos);
				return Maybe.some(createCubemapCamera(srcCamera.uncached, tfm, partialTick));
			});
		} else if (location instanceof Location.Station loc) {
			return universe.getStation(loc.id).flatMap(station -> {
				final var p = station.getPos(partialTick);
				tfm.appendTranslation(p);
				applyFaceDir(tfm, faceDir);
				return Maybe.some(createCubemapCamera(srcCamera.uncached, tfm, partialTick));
			});
		}
		return Maybe.none();
	}

	private Maybe<CachedCamera<?>> createCamera(Camera camera, Location location, float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);
		final var tfm = new TransformStack();
		if (location instanceof Location.World loc) {
			return universe.getSystem(loc.id.system()).flatMap(sys -> {
				final var node = sys.rootNode.lookup(loc.id.nodeId());
				if (node == null)
					return Maybe.none();
				final var srcCamPos = Vec3.from(camera.getPosition());
				// var nodePos = sys.pos.add(node.getPosition(partialTick));
				// final var quat = toCelestialWorldSpaceRotation(node, time, srcCamPos.xz());
				// if (node instanceof PlanetaryCelestialNode planetNode) {
				// nodePos = nodePos.add(getPlanetSurfaceOffset(planetNode, quat));
				// }
				applyPlanetTransform(tfm, node, time, srcCamPos.xz(), partialTick);
				tfm.appendTranslation(sys.pos);
				return Maybe.some(createCamera(camera, tfm, partialTick));
			});
		} else if (location instanceof Location.Station loc) {
			return universe.getStation(loc.id).flatMap(station -> {
				final var p = station.getPos(partialTick);
				tfm.appendRotation(station.orientation.inverse());
				tfm.appendTranslation(p);
				return Maybe.some(createCamera(camera, tfm, partialTick));
			});
		}
		return Maybe.none();
	}

	private void createTickets(Universe universe, UniverseSectorId galaxyId) {
		if (this.galaxyTicket == null)
			this.galaxyTicket = universe.sectorManager.createGalaxyTicketManual(galaxyId);
		final var galaxy = this.galaxyTicket.forceLoad().unwrapOrNull();
		if (galaxy == null)
			return;
		if (this.starRenderer == null)
			this.starRenderer = new StarRenderManager(galaxy, Vec3.ZERO);
		if (this.systemTicket == null)
			this.systemTicket = galaxy.sectorManager.createSystemTicketManual(null);

		if (this.galaxyRenderingContext == null)
			this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy);
	}

	private void disposeTickets() {
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
			this.galaxyRenderingContext = null;
		}
	}

	private void drawCelestialObjects(Camera srcCamera, GlFramebuffer target, float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);

		target.bind();
		// final var skyColor = this.client.level.getSkyColor(this.client.gameRenderer.getMainCamera().getPosition(), partialTick);
        // Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), partialTick);

		// target.clearColorAttachment("fragColor", new Color(skyColor.x, skyColor.y, skyColor.z, 1));
		target.clear();
		target.clearColorAttachment("fColor", new Color(0.1, 0.1, 0.12, 1));

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
				// drawGalaxyCubemap(camera, galaxy, partialTick);
				drawGalaxy(camera, galaxy, partialTick);

				// final var builder = BufferRenderer.IMMEDIATE_BUILDER;
				// builder.begin(PrimitiveType.POINTS, UltravioletVertexFormats.BILLBOARD_FORMAT);

				// RenderHelper.addBillboard(builder, camera, new TransformStack(), Vec3.YP, 1, Color.WHITE);

				// final var shader = getShader(SHADER_STAR_BILLBOARD);
				// shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
				// builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

				if (this.starRenderer != null) {
					profiler.popPush("stars");
					this.starRenderer.draw(camera);
				}
				profiler.popPush("system");
				drawSystem(camera, galaxy, partialTick);
				profiler.pop();
			}
			snapshot.restore();
		});

		this.previousLocation = location;
	}

	private void renderGalaxyToCubemap(Galaxy galaxy, float partialTick) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;

		final var tfm = new TransformStack();
		final var location = EntityAccessor.getLocation(this.client.player);
		applyGalaxyTranslation(tfm, location, partialTick);

		this.galaxyCubemap.renderToCubemap(faceDir -> {
			tfm.push();
			applyFaceDir(tfm, faceDir);
			final var camera = createCubemapCamera(null, tfm, partialTick);

			if (this.shouldRenderGalaxyToCubemap)
				return;

			final var shader = getShader(SHADER_GALAXY_PARTICLE);
			shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));

			builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
			this.galaxyRenderingContext.build();
			this.galaxyRenderingContext.enumerate((pos, size) -> {
				RenderHelper.addBillboard(builder, camera, new TransformStack(),
						pos,
						0.4 * size * (1e12 / camera.metersPerUnit),
						Color.WHITE.withA(1.0));
			});
			builder.end().draw(shader, DRAW_STATE_DIRECT_ADDITIVE_BLENDING);
			tfm.pop();
		});

		this.shouldRenderGalaxyToCubemap = false;
	}

	private void drawGalaxyCubemap(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		if (this.shouldRenderGalaxyToCubemap)
			renderGalaxyToCubemap(galaxy, partialTick);
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;

		final var shader = getShader(SHADER_SKYBOX);
		shader.setUniformSampler("SkyboxSampler", this.galaxyCubemap);

		builder.begin(PrimitiveType.QUADS, DefaultVertexFormat.POSITION);
		// -Y
		builder.vertex(1, -1, 1).endVertex();
		builder.vertex(-1, -1, 1).endVertex();
		builder.vertex(-1, -1, -1).endVertex();
		builder.vertex(1, -1, -1).endVertex();
		// +Y
		builder.vertex(-1, 1, -1).endVertex();
		builder.vertex(1, 1, -1).endVertex();
		builder.vertex(1, 1, 1).endVertex();
		builder.vertex(-1, 1, 1).endVertex();
		// -X
		builder.vertex(-1, 1, 1).endVertex();
		builder.vertex(-1, -1, 1).endVertex();
		builder.vertex(-1, -1, -1).endVertex();
		builder.vertex(-1, 1, -1).endVertex();
		// +X
		builder.vertex(1, -1, -1).endVertex();
		builder.vertex(1, 1, -1).endVertex();
		builder.vertex(1, 1, 1).endVertex();
		builder.vertex(1, -1, 1).endVertex();
		// -Z
		builder.vertex(1, 1, -1).endVertex();
		builder.vertex(-1, 1, -1).endVertex();
		builder.vertex(-1, -1, -1).endVertex();
		builder.vertex(1, -1, -1).endVertex();
		// +Z
		builder.vertex(-1, -1, 1).endVertex();
		builder.vertex(1, -1, 1).endVertex();
		builder.vertex(1, 1, 1).endVertex();
		builder.vertex(-1, 1, 1).endVertex();
		builder.end().draw(shader, DRAW_STATE_DIRECT);
	}

	private void drawGalaxy(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var rng = Rng.wrap(new Random(1337));

		final var shader = getShader(SHADER_GALAXY_PARTICLE);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));

		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		this.galaxyRenderingContext.build();
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new TransformStack(),
					pos,
					0.4 * size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.1));
		});
		builder.end().draw(shader, DRAW_STATE_DIRECT_ADDITIVE_BLENDING);

	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var system = this.systemTicket.forceLoad();
		if (system.isSome())
			drawSystem(camera, galaxy, system.unwrap(), partialTick);
	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, StarSystem system,
			float partialTick) {
		final var profiler = Minecraft.getInstance().getProfiler();
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);

		profiler.push("camera");
		camera.setupRenderMatrices();

		// system.rootNode.updatePositions(time);

		profiler.popPush("planet_context_setup");
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
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
		// drawCelestialObjects(camera, this.hdrSpaceTarget, partialTick);
		drawCelestialObjects(camera, mainTarget, partialTick);
		profiler.pop();

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

		// final var sceneTexture =
		// this.hdrSpaceTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		// HawkRendering.doPostProcessing(mainTarget, sceneTexture);

		return true;
	}

	@Override
	public void close() {
		if (this.galaxyCubemap != null)
			this.galaxyCubemap.close();
		if (this.hdrSpaceTarget != null)
			this.hdrSpaceTarget.close();
		if (this.postProcessTarget != null)
			this.postProcessTarget.close();
		if (this.starRenderer != null)
			this.starRenderer.close();
	}

}
