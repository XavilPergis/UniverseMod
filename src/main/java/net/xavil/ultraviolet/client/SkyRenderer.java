package net.xavil.ultraviolet.client;

import java.util.Objects;
import java.util.Random;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import static net.xavil.ultraviolet.client.Shaders.*;
import static net.xavil.ultraviolet.client.DrawStates.*;

import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.camera.CachedCamera;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexConsumer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
import net.xavil.ultraviolet.client.gl.GlFragmentWrites;
import net.xavil.ultraviolet.client.gl.GlFramebuffer;
import net.xavil.ultraviolet.client.gl.texture.GlCubemapTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.matrices.Mat4;
import net.xavil.util.math.matrices.Vec2;
import net.xavil.util.math.matrices.Vec2i;
import net.xavil.util.math.matrices.Vec3;

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
		// n = ((System.nanoTime() / 100000) % 1000000) / 1e0;
		// offset = Vec3.from(0, 10000000, 0);
		tfm.push();
		tfm.appendTranslation(Vec3.from(n, 0.01 * n, 0));
		tfm.appendTranslation(offset);
		tfm.prependRotation(CachedCamera.orientationFromMinecraftCamera(camera).inverse());
		final var invView = tfm.get();
		tfm.pop();
		final var proj = GameRendererAccessor.makeProjectionMatrix(this.client.gameRenderer,
				0.000001f, 1e5f, false, partialTick);
		return new CachedCamera<>(camera, invView, proj, 1e12);
	}

	private CachedCamera<?> createCubemapCamera(Object camera, TransformStack tfm, float partialTick) {
		final var invView = tfm.get();
		final var proj = Mat4.perspectiveProjection(Math.PI, 1, 1e-5, 1e5);
		return new CachedCamera<>(camera, invView, proj, 1e12);
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

	private Option<CachedCamera<?>> createCubemapCamera(CachedCamera<?> srcCamera, Location location, Vec3 faceDir,
			float partialTick) {
		final var universe = MinecraftClientAccessor.getUniverse(this.client);
		final var time = universe.getCelestialTime(partialTick);
		final var tfm = new TransformStack();
		if (location instanceof Location.World loc) {
			return universe.getSystem(loc.id.system()).flatMap(sys -> {
				final var node = sys.rootNode.lookup(loc.id.nodeId());
				if (node == null)
					return Option.none();
				tfm.appendTranslation(node.getPosition(partialTick));
				tfm.appendTranslation(sys.pos);
				return Option.some(createCubemapCamera(srcCamera.camera, tfm, partialTick));
			});
		} else if (location instanceof Location.Station loc) {
			return universe.getStation(loc.id).flatMap(station -> {
				final var p = station.getPos(partialTick);
				tfm.appendTranslation(p);
				applyFaceDir(tfm, faceDir);
				return Option.some(createCubemapCamera(srcCamera.camera, tfm, partialTick));
			});
		}
		return Option.none();
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
				// nodePos = nodePos.add(getPlanetSurfaceOffset(planetNode, quat));
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
		if (this.starRenderer == null)
			this.starRenderer = new StarRenderManager(galaxy);
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
		target.clear();

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
		final var builder = BufferRenderer.immediateBuilder();

		final var tfm = new TransformStack();
		final var location = EntityAccessor.getLocation(this.client.player);
		applyGalaxyTranslation(tfm, location, partialTick);

		this.galaxyCubemap.renderToCubemap(faceDir -> {
			tfm.push();
			applyFaceDir(tfm, faceDir);
			final var camera = createCubemapCamera(null, tfm, partialTick);

			if (this.shouldRenderGalaxyToCubemap)
				return;

			builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);
			this.galaxyRenderingContext.build();
			this.galaxyRenderingContext.enumerate((pos, size) -> {
				RenderHelper.addBillboard(builder, camera, new TransformStack(),
						pos,
						0.4 * size * (1e12 / camera.metersPerUnit),
						Color.WHITE.withA(1.0));
			});
			builder.end();

			final var shader = getShader(SHADER_GALAXY_PARTICLE);
			shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
			builder.draw(shader, DRAW_STATE_DIRECT_ADDITIVE_BLENDING);
			tfm.pop();
		});

		this.shouldRenderGalaxyToCubemap = false;
	}

	private void drawGalaxyCubemap(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		if (this.shouldRenderGalaxyToCubemap)
			renderGalaxyToCubemap(galaxy, partialTick);
		final var builder = BufferRenderer.immediateBuilder();

		builder.begin(FlexibleVertexMode.QUADS, DefaultVertexFormat.POSITION);
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
		builder.end();

		final var shader = getShader(SHADER_SKYBOX);
		shader.setUniformSampler("SkyboxSampler", this.galaxyCubemap);
		builder.draw(shader, DRAW_STATE_DIRECT);
	}

	private void drawGalaxy(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		final var rng = Rng.wrap(new Random(1337));

		builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);

		this.galaxyRenderingContext.build();
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new TransformStack(),
					pos,
					0.4 * size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.1));
		});

		builder.end();

		final var shader = getShader(SHADER_GALAXY_PARTICLE);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		builder.draw(shader, DRAW_STATE_DIRECT_ADDITIVE_BLENDING);

	}

	private void drawSystem(CachedCamera<?> camera, Galaxy galaxy, float partialTick) {
		final var system = this.systemTicket.forceLoad();
		if (system.isSome() && system.isNone())
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

	public boolean renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta,
			Camera camera, boolean isSkyVisible) {

		if (this.hdrSpaceTarget == null) {
			Mod.LOGGER.info("creating SkyRenderer framebuffer");
			this.hdrSpaceTarget = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY);
			this.hdrSpaceTarget.createColorTarget(GlFragmentWrites.COLOR, GlTexture.Format.RGBA32_FLOAT);
			this.hdrSpaceTarget.createDepthTarget(false, GlTexture.Format.DEPTH_UNSPECIFIED);
			this.hdrSpaceTarget.enableAllColorAttachments();
			this.hdrSpaceTarget.checkStatus();
		}

		final var mainTarget = new GlFramebuffer(this.client.getMainRenderTarget());
		mainTarget.enableAllColorAttachments();

		final var partialTick = this.client.isPaused() ? 0 : this.client.getFrameTime();
		// drawCelestialObjects(camera, this.hdrSpaceTarget, partialTick);
		drawCelestialObjects(camera, mainTarget, partialTick);

		// TODO: draw atmosphere or something
		// TODO: figure out how the fuck we're gonna make vanilla fog not look like
		// total ass
		// could maybe replace the vanilla fog shader with one that takes in a
		// background image buffer and uses that as the fog color. idk.

		mainTarget.bind();
		mainTarget.clearDepthAttachment(1.0f);

		// final var sceneTexture = this.hdrSpaceTarget.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		// ModRendering.doPostProcessing(mainTarget, sceneTexture);

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
