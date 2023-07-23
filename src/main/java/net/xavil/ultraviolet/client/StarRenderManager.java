package net.xavil.ultraviolet.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexBuffer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.BillboardBatcher;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarRenderManager implements Disposable {
	private static final Minecraft CLIENT = Minecraft.getInstance();

	private GalaxyTicket galaxyTicket;
	private SectorTicket<SectorTicketInfo.Multi> sectorTicket;

	private FlexibleVertexBuffer starsBuffer = new FlexibleVertexBuffer();

	private Vec3 starSnapshotOrigin = null;
	/**
	 * The distance the camera needs to be from the current snapshot (in Tm) before
	 * the background stars are rebuilt.
	 */
	public double starSnapshotThreshold = 10000;
	private double immediateStarsTimerTicks = -1;
	/**
	 * The position relative to the galaxy at which the coordinate system's origin
	 * is placed.
	 */
	public Vec3 originOffset;

	public StarRenderManager(Galaxy galaxy, Vec3 originOffset) {
		this.galaxyTicket = galaxy.parentUniverse.sectorManager.createGalaxyTicketManual(galaxy.galaxyId);
		this.sectorTicket = galaxy.sectorManager.createSectorTicketManual(null);
		this.originOffset = originOffset;
	}

	public void tick() {
		if (this.immediateStarsTimerTicks > 0) {
			this.immediateStarsTimerTicks -= 1;
		}
	}

	public void draw(CachedCamera<?> camera) {
		final var camPos = camera.posTm.add(this.originOffset);

		if (this.sectorTicket.info == null)
			this.sectorTicket.info = SectorTicketInfo.visual(camPos);
		this.sectorTicket.info.centerPos = camPos;
		this.sectorTicket.info.multiplicitaveFactor = 2.0;

		if (ClientConfig.get(ConfigKey.FORCE_STAR_RENDERER_IMMEDIATE_MODE)) {
			drawStarsImmediate(camera);
		} else {
			buildStarsIfNeeded(camera);
			if (this.immediateStarsTimerTicks != -1) {
				drawStarsImmediate(camera);
			} else {
				drawStarsFromBuffer(camera);
			}
		}
	}

	private void buildStarsIfNeeded(CachedCamera<?> camera) {
		if (this.starSnapshotOrigin != null
				&& camera.posTm.distanceTo(this.starSnapshotOrigin) < this.starSnapshotThreshold) {
			this.immediateStarsTimerTicks = -1;
			return;
		}
		if (this.immediateStarsTimerTicks == -1)
			this.immediateStarsTimerTicks = 10;
		if (this.immediateStarsTimerTicks > 0)
			return;

		this.starSnapshotOrigin = camera.posTm;
		this.immediateStarsTimerTicks = -1;

		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		this.sectorTicket.attachedManager.forceLoad(this.sectorTicket);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(camera.posTm.add(this.originOffset)) > levelSize)
					return;
				RenderHelper.addBillboard(builder, camera, elem.info().primaryStar, elem.pos().sub(this.originOffset));
			});
		});
		this.starsBuffer.upload(builder.end());
	}

	private void drawStarsImmediate(CachedCamera<?> camera) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var batcher = new BillboardBatcher(builder, 10000);

		final var camPos = camera.posTm.add(this.originOffset);
		batcher.begin(camera);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			// if (!camera.isAabbInFrustum(min, max))
			// return;
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(camPos) > levelSize)
					return;
				final var toStar = elem.pos().sub(camPos);
				if (toStar.dot(camera.forward) == 0)
					return;
				batcher.add(elem.info().primaryStar, elem.pos().sub(this.originOffset));
			});
		});
		batcher.end();
	}

	private void drawStarsFromBuffer(CachedCamera<?> camera) {
		final var snapshot = camera.setupRenderMatrices();

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			final var offset = this.starSnapshotOrigin.mul(1e12 / camera.metersPerUnit).sub(camera.pos);
			poseStack.mulPose(camera.orientation.toMinecraft());
			poseStack.translate(offset.x, offset.y, offset.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		final var shader = getShader(SHADER_STAR_BILLBOARD);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		BufferRenderer.setupDefaultShaderUniforms(shader);
		this.starsBuffer.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

		snapshot.restore();
	}

	@Override
	public void close() {
		this.galaxyTicket.close();
		this.sectorTicket.close();
		this.starsBuffer.close();
	}

}
