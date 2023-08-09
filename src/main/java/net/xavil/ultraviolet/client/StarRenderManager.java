package net.xavil.ultraviolet.client;

import com.mojang.blaze3d.systems.RenderSystem;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexBuffer;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarRenderManager implements Disposable {
	private GalaxyTicket galaxyTicket;
	private SectorTicket<SectorTicketInfo.Multi> sectorTicket;

	private FlexibleVertexBuffer starsBuffer = new FlexibleVertexBuffer();

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

	public void draw(CachedCamera<?> camera, Vec3 centerPos) {
		this.sectorTicket.info.centerPos = centerPos;

		if (this.floatingOrigin == null
				|| camera.posTm.distanceTo(this.floatingOrigin) > this.floatingOriginThreshold) {
			// TODO: we don't have to rebuild if there arent actually any stars nearby the
			// camera, since you wouldn't be able to tell there's precision issues past a
			// certain threshold.
			this.floatingOrigin = camera.posTm;
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

	private void buildStarsIfNeeded(CachedCamera<?> camera, Vec3 centerPos) {
		if (!this.isDirty)
			return;

		// switch to "immediate-mode" rendering if we havent yet loaded in all the
		// sectors.
		this.drawImmediate = true;
		if (!this.sectorTicket.attachedManager.isComplete(this.sectorTicket))
			return;

		this.starSnapshotPosition = centerPos;

		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		final var ctx = new StarBuildingContext(builder, camera, centerPos, false);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			drawSectorStars(ctx, sector);
		});
		this.starsBuffer.upload(builder.end());
		this.drawImmediate = false;
		this.isDirty = false;
	}

	private void drawStarsImmediate(CachedCamera<?> camera, Vec3 centerPos) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		final var ctx = new StarBuildingContext(builder, camera, centerPos, true);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			drawSectorStars(ctx, sector);
		});

		final var snapshot = camera.setupRenderMatrices();

		//    0         2                                                     9          10
		//    *         S                                                     C          c
		//   -10       -8                                                                0
		// <- -                                                                          + ->

		final var origin = this.originOffset.add(this.floatingOrigin);
		final var offset = camera.posTm.sub(origin).mul(1e12 / camera.metersPerUnit);

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			poseStack.mulPose(camera.orientation.toMinecraft());
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
			final var shader = getShader(SHADER_STAR_BILLBOARD_REALISTIC);
			StarRenderManager.setupStarShader(shader, camera);
			builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
		} else if (this.mode == Mode.MAP) {
			final var shader = getShader(SHADER_STAR_BILLBOARD_UI);
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
		final GalaxySector.SectorElementHolder elem = new GalaxySector.SectorElementHolder();
		final Vec3.Mutable toStar = new Vec3.Mutable();

		final FlexibleVertexConsumer builder;
		final CachedCamera<?> camera;
		final Vec3 centerPos;
		final boolean isImmediateMode;

		public StarBuildingContext(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 centerPos,
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

		final var elem = ctx.elem;
		final var colorHolder = ctx.colorHolder;
		final var toStar = ctx.toStar;

		final var actualOrigin = this.originOffset.add(this.floatingOrigin);

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
			Vec3.mul(elem.systemPosTm, elem.systemPosTm, 1e12 / ctx.camera.metersPerUnit);

			StellarCelestialNode.blackBodyColorFromTable(colorHolder, elem.temperatureK);

			if (this.mode == Mode.REALISTIC) {
				ctx.builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.uv0((float) elem.luminosityLsol, 0)
						.endVertex();
			} else if (this.mode == Mode.MAP) {
				ctx.builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.endVertex();
			}
		}
	}

	// private void drawStars(FlexibleVertexConsumer builder, CachedCamera<?>
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

	private void drawStarsFromBuffer(CachedCamera<?> camera, Vec3 centerPos) {
		final var snapshot = camera.setupRenderMatrices();

		//    0         2                                                     9          10
		//    *         S                                                     C          c
		//   -10       -8                                                                0
		// <- -                                                                          + ->

		final var origin = this.originOffset.add(this.floatingOrigin);
		final var offset = camera.posTm.sub(origin).mul(1e12 / camera.metersPerUnit);

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			poseStack.mulPose(camera.orientation.toMinecraft());
			// poseStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
			poseStack.translate(-offset.x, -offset.y, -offset.z);
			// poseStack.translate(org.x, org.y, org.z);
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
			final var shader = getShader(SHADER_STAR_BILLBOARD_REALISTIC);
			BufferRenderer.setupDefaultShaderUniforms(shader);
			setupStarShader(shader, camera);
			this.starsBuffer.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
		} else if (this.mode == Mode.MAP) {
			final var shader = getShader(SHADER_STAR_BILLBOARD_UI);
			BufferRenderer.setupDefaultShaderUniforms(shader);
			setupStarShader(shader, camera);
			this.starsBuffer.draw(shader, DRAW_STATE_OPAQUE);
		}

		snapshot.restore();
	}

	public static void setupStarShader(ShaderProgram shader, CachedCamera<?> camera) {
		final var universe = MinecraftClientAccessor.getUniverse();
		final var partialTick = Minecraft.getInstance().getFrameTime();
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		shader.setUniform("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniform("uTime", universe.getCelestialTime(partialTick));
		shader.setUniform("uStarMinSize", ClientConfig.get(ConfigKey.STAR_SHADER_STAR_MIN_SIZE));
		shader.setUniform("uStarMaxSize", ClientConfig.get(ConfigKey.STAR_SHADER_STAR_MAX_SIZE));
		shader.setUniform("uStarSizeSquashFactor", ClientConfig.get(ConfigKey.STAR_SHADER_STAR_SIZE_SQUASH_FACTOR));
		shader.setUniform("uStarBrightnessFactor", ClientConfig.get(ConfigKey.STAR_SHADER_STAR_BRIGHTNESS_FACTOR));
		shader.setUniform("uDimStarMinAlpha", ClientConfig.get(ConfigKey.STAR_SHADER_DIM_STAR_MIN_ALPHA));
		shader.setUniform("uDimStarExponent", ClientConfig.get(ConfigKey.STAR_SHADER_DIM_STAR_EXPONENT));
	}

	@Override
	public void close() {
		this.galaxyTicket.close();
		this.sectorTicket.close();
		this.starsBuffer.close();
	}

}
