package net.xavil.ultraviolet.client;

import com.mojang.blaze3d.systems.RenderSystem;

import static net.xavil.hawklib.client.HawkDrawStates.*;

import java.time.Instant;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarRenderManager implements Disposable {
	private GalaxyTicket galaxyTicket;
	private SectorTicket<SectorTicketInfo.Multi> sectorTicket;

	private Mesh starsMesh = new Mesh();

	/**
	 * The distance the camera needs to be from the current snapshot (in Tm) before
	 * the background stars are rebuilt.
	 */
	public double starSnapshotThreshold = 10000;
	/**
	 * The position at which the last snapshot was taken. This also acts as a sort
	 * of floating origin, so as to minimize floating point wobble.
	 */
	private Vec3 starSnapshotPosition = null;
	public double floatingOriginThreshold = 50000000;
	private Vec3 floatingOrigin = null;

	private boolean drawImmediate = true;
	/**
	 * The position relative to the galaxy at which the coordinate system's origin
	 * is placed.
	 */
	private Vec3 originOffset = Vec3.ZERO;

	// true if properties have changed that require a rebuild.
	private boolean isDirty = false;

	private Mode mode = Mode.REALISTIC;
	private BatchingHint batchingHint = BatchingHint.STATIC;

	private Instant builderTimeoutStart = Instant.now();
	private double prevPercentComplete = 0.0;

	public enum Mode {
		// rendered as accurately as i know how to -- used for rendering the background
		// stars in SkyRenderer
		REALISTIC,
		// // like Elite: Dangerous's "Realistic" galaxy map mode
		// PRETTY,
		// meant to look like nodes on a map rather than actual stars.
		MAP,
	}

	public enum BatchingHint {
		/**
		 * Use this when it is expected that the camera position will barely move.
		 */
		STATIC,
		/**
		 * Use this when it is expected that the camera position will be moving around a
		 * lot, so that lag spikes can be avoided in areas of high stellar density. The
		 * tradeoff for reduced lag spikes is that overall framerate might be lower.
		 */
		// TODO: lag spikes can maybe be fixed by building geometry in another thread
		DYNAMIC,
	}

	public StarRenderManager(Galaxy galaxy, SectorTicketInfo.Multi sectorTicketInfo) {
		this.galaxyTicket = galaxy.parentUniverse.sectorManager.createGalaxyTicketManual(galaxy.galaxyId);
		this.sectorTicket = galaxy.sectorManager.createSectorTicketManual(sectorTicketInfo);
	}

	public void setOriginOffset(Vec3 originOffset) {
		if (this.originOffset.equals(originOffset))
			return;
		this.originOffset = originOffset;
		this.isDirty = true;
	}

	public void setMode(Mode mode) {
		if (this.mode.equals(mode))
			return;
		this.mode = mode;
		this.isDirty = true;
	}

	public void setBatchingHint(BatchingHint batchingHint) {
		if (this.batchingHint.equals(batchingHint))
			return;
		this.batchingHint = batchingHint;
		this.isDirty = true;
	}

	public SectorTicket<SectorTicketInfo.Multi> getSectorTicket() {
		return this.sectorTicket;
	}

	public void draw(CachedCamera camera, Vec3 centerPos) {
		this.sectorTicket.info.centerPos = centerPos;
		this.sectorTicket.info.baseRadius = GalaxySector.BASE_SIZE_Tm;

		if (this.floatingOrigin == null
				|| camera.posTm.distanceTo(this.floatingOrigin) > this.floatingOriginThreshold) {
			// TODO: we don't have to rebuild if there arent actually any stars nearby the
			// camera, since you wouldn't be able to tell there's precision issues past a
			// certain threshold.
			this.floatingOrigin = camera.posTm.xyz();
			this.isDirty = true;
		}

		if (this.starSnapshotPosition == null
				|| centerPos.distanceTo(this.starSnapshotPosition) > this.starSnapshotThreshold) {
			this.starSnapshotPosition = centerPos;
			this.isDirty = true;
		}

		if (ClientConfig.get(ConfigKey.FORCE_STAR_RENDERER_IMMEDIATE_MODE)) {
			drawStarsImmediate(camera, centerPos);
			this.starSnapshotPosition = null;
		} else {
			buildStarsIfNeeded(camera, centerPos);
			if (this.drawImmediate) {
				drawStarsImmediate(camera, centerPos);
			} else {
				drawStarsFromBuffer(camera, centerPos);
			}
		}
	}

	private void buildStarsIfNeeded(CachedCamera camera, Vec3 centerPos) {
		if (!this.isDirty)
			return;

		// switch to "immediate-mode" rendering if we havent yet loaded in all the
		// sectors.
		this.drawImmediate = true;
		final var percentComplete = this.sectorTicket.attachedManager.percentComplete(this.sectorTicket);
		if (percentComplete < 1.0) {
			if (percentComplete != this.prevPercentComplete) {
				this.builderTimeoutStart = Instant.now();
				this.prevPercentComplete = percentComplete;
				return;
			} else if (Instant.now().isBefore(this.builderTimeoutStart.plusSeconds(5))) {
				return;
			}
		}

		this.starSnapshotPosition = centerPos;

		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.POINT_QUADS,
				UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD);
		final var ctx = new StarBuildingContext(builder, camera, centerPos, false);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			drawSectorStars(ctx, sector);
		});
		this.starsMesh.upload(builder.end());
		this.drawImmediate = false;
		this.isDirty = false;
	}

	private void drawStarsImmediate(CachedCamera camera, Vec3 centerPos) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.POINT_QUADS,
				UltravioletVertexFormats.VERTEX_FORMAT_BILLBOARD);
		final var ctx = new StarBuildingContext(builder, camera, centerPos, true);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			drawSectorStars(ctx, sector);
		});

		final var snapshot = RenderMatricesSnapshot.capture();
		camera.applyProjection();
		// CachedCamera.applyView(camera.orientation);

		final var origin = this.floatingOrigin;
		final var offset = camera.posTm.sub(origin).mul(1e12 / camera.metersPerUnit);

		// TODO: cleanup
		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			poseStack.mulPose(camera.orientation.asMinecraft());
			// poseStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
			poseStack.translate(-offset.x, -offset.y, -offset.z);
			// poseStack.translate(org.x, org.y, org.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		if (this.mode == Mode.REALISTIC) {
			final var shader = UltravioletShaders.SHADER_STAR_BILLBOARD_REALISTIC.get();
			StarRenderManager.setupStarShader(shader, camera);
			builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
		} else if (this.mode == Mode.MAP) {
			final var shader = UltravioletShaders.SHADER_STAR_BILLBOARD_UI.get();
			StarRenderManager.setupStarShader(shader, camera);
			builder.end().draw(shader, DRAW_STATE_OPAQUE);
		}

		snapshot.restore();

	}

	// TODO: would it be better to split up rendering into "chunks", so that we
	// don't have to render all the stars every frame? It seems to be performant
	// enough on my machine, so im not super worried...

	private static class StarBuildingContext {
		final Vec3.Mutable colorHolder = new Vec3.Mutable();
		final GalaxySector.ElementHolder elem = new GalaxySector.ElementHolder();
		final Vec3.Mutable toStar = new Vec3.Mutable();

		final FlexibleVertexConsumer builder;
		final CachedCamera camera;
		final Vec3 centerPos;
		final boolean isImmediateMode;

		public StarBuildingContext(FlexibleVertexConsumer builder, CachedCamera camera, Vec3 centerPos,
				boolean isImmediateMode) {
			this.builder = builder;
			this.camera = camera;
			this.centerPos = centerPos;
			this.isImmediateMode = isImmediateMode;
		}
	}

	private void drawSectorStars(StarBuildingContext ctx, GalaxySector sector) {
		if (ctx.isImmediateMode) {
			// FIXME: frustum culling doesnt work!
			// final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			// final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			// if (!camera.isAabbInFrustum(min, max))
			// return;
		}

		// if (sector.level == 0) return;
		// if (sector.level == 1) return;
		// if (sector.level == 2) return;
		// if (sector.level == 3) return;
		// if (sector.level == 4) return;
		// if (sector.level == 5) return;
		// if (sector.level == 6) return;
		// if (sector.level == 7) return;

		final var elem = ctx.elem;
		final var colorHolder = ctx.colorHolder;
		final var toStar = ctx.toStar;

		// final var actualOrigin = this.originOffset.add(this.floatingOrigin);
		final var actualOrigin = this.floatingOrigin;

		final var levelSize = this.sectorTicket.info.radiusForLevel(sector.level);
		for (int i = 0; i < sector.elements.size(); ++i) {
			sector.elements.load(elem, i);
			if (elem.systemPosTm.distanceTo(ctx.centerPos) > levelSize)
				continue;

			// don't render the stars that are behind the camera in immediate mode
			if (ctx.isImmediateMode) {
				Vec3.set(toStar, elem.systemPosTm);
				Vec3.sub(toStar, toStar, ctx.centerPos);
				if (toStar.dot(ctx.camera.forward) == 0)
					return;
			}

			Vec3.sub(elem.systemPosTm, elem.systemPosTm, actualOrigin);
			Vec3.add(elem.systemPosTm, elem.systemPosTm, this.originOffset);
			Vec3.mul(elem.systemPosTm, elem.systemPosTm, 1e12 / ctx.camera.metersPerUnit);

			StellarCelestialNode.BLACK_BODY_COLOR_TABLE.lookupColor(colorHolder, elem.temperatureK);
			final var brightnessMultiplier = StellarCelestialNode.BLACK_BODY_COLOR_TABLE
					.lookupBrightnessMultiplier(elem.temperatureK);

			if (this.mode == Mode.REALISTIC) {
				ctx.builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.uv0((float) elem.luminosityLsol, brightnessMultiplier)
						.endVertex();
			} else if (this.mode == Mode.MAP) {
				ctx.builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.endVertex();
			}
		}
	}

	// private void drawStars(FlexibleVertexConsumer builder, CachedCamera
	// camera, Vec3 centerPos,
	// boolean isImmediateMode) {

	// var to = this.originOffset;
	// if (isImmediateMode) {
	// to = to.sub(camera.posTm);
	// }

	// // java moment - can't capture a variable you mutate :(
	// final var totalOffset = to;

	// final var ctx = new StarBuildingContext(builder, camera, centerPos,
	// isImmediateMode);
	// this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
	// drawSectorStars(ctx, sector);
	// });
	// }

	private void drawStarsFromBuffer(CachedCamera camera, Vec3 centerPos) {
		final var snapshot = RenderMatricesSnapshot.capture();
		camera.applyProjection();

		final var origin = this.floatingOrigin;
		final var offset = camera.posTm.sub(origin).mul(1e12 / camera.metersPerUnit);

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			poseStack.mulPose(camera.orientation.asMinecraft());
			// poseStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
			poseStack.translate(-offset.x, -offset.y, -offset.z);
			// poseStack.translate(this.originOffset.x, this.originOffset.y,
			// this.originOffset.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		// FIXME: floating point precision issues
		// it would be better to build the geometry buffer relative to a point close to
		// the camera, instead of building it in world space and translating back to the
		// origin.

		if (this.mode == Mode.REALISTIC) {
			final var shader = UltravioletShaders.SHADER_STAR_BILLBOARD_REALISTIC.get();
			shader.setupDefaultShaderUniforms();
			setupStarShader(shader, camera);
			this.starsMesh.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
		} else if (this.mode == Mode.MAP) {
			final var shader = UltravioletShaders.SHADER_STAR_BILLBOARD_UI.get();
			shader.setupDefaultShaderUniforms();
			setupStarShader(shader, camera);
			this.starsMesh.draw(shader, DRAW_STATE_OPAQUE);
		}

		snapshot.restore();
	}

	public static void setupStarShader(ShaderProgram shader, CachedCamera camera) {
		final var universe = MinecraftClientAccessor.getUniverse();
		final var partialTick = Minecraft.getInstance().getFrameTime();
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.STAR_ICON_LOCATION));
		shader.setUniformf("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniformf("uTime", universe.getCelestialTime(partialTick));

		shader.setUniformf("uStarSize", ClientConfig.get(ConfigKey.STAR_SHADER_STAR_SIZE));
		shader.setUniformf("uStarLuminosityScale", ClientConfig.get(ConfigKey.STAR_SHADER_LUMINOSITY_SCALE));
		shader.setUniformf("uStarLuminosityMax", ClientConfig.get(ConfigKey.STAR_SHADER_LUMINOSITY_MAX));
		shader.setUniformf("uStarBrightnessScale", ClientConfig.get(ConfigKey.STAR_SHADER_BRIGHTNESS_SCALE));
		shader.setUniformf("uStarBrightnessMax", ClientConfig.get(ConfigKey.STAR_SHADER_BRIGHTNESS_MAX));
		shader.setUniformf("uReferenceMagnitude", ClientConfig.get(ConfigKey.STAR_SHADER_REFERENCE_MAGNITUDE));
		shader.setUniformf("uMagnitudeBase", ClientConfig.get(ConfigKey.STAR_SHADER_MAGNITUDE_BASE));
		shader.setUniformf("uMagnitudePower", ClientConfig.get(ConfigKey.STAR_SHADER_MAGNITUDE_POWER));
	}

	@Override
	public void close() {
		this.galaxyTicket.close();
		this.sectorTicket.close();
		this.starsMesh.close();
	}

}
