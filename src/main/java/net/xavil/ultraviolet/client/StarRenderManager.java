package net.xavil.ultraviolet.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import static net.xavil.ultraviolet.client.Shaders.*;
import static net.xavil.ultraviolet.client.DrawStates.*;
import net.xavil.ultraviolet.client.camera.CachedCamera;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexBuffer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.client.screen.BillboardBatcher;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.util.Disposable;
import net.xavil.util.math.matrices.Vec3;

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

	public StarRenderManager(Galaxy galaxy) {
		this.galaxyTicket = galaxy.parentUniverse.sectorManager.createGalaxyTicketManual(galaxy.galaxyId);
		this.sectorTicket = galaxy.sectorManager.createSectorTicketManual(null);
	}

	public void tick() {
		if (this.immediateStarsTimerTicks > 0) {
			this.immediateStarsTimerTicks -= 1;
		}
	}

	public void draw(CachedCamera<?> camera) {
		final var camPos = camera.pos.mul(camera.metersPerUnit / 1e12);

		if (this.sectorTicket.info == null)
			this.sectorTicket.info = SectorTicketInfo.visual(camPos);
		this.sectorTicket.info.centerPos = camPos;
		this.sectorTicket.info.multiplicitaveFactor = 2.0;

		buildStarsIfNeeded(camera);
		if (this.immediateStarsTimerTicks != -1) {
			drawStarsImmediate(camera);
		} else {
			drawStarsFromBuffer(camera);
		}
		// drawStarsImmediate(camera);
	}

	private void buildStarsIfNeeded(CachedCamera<?> camera) {
		if (this.starSnapshotOrigin != null
				&& camera.posTm.distanceTo(this.starSnapshotOrigin) < this.starSnapshotThreshold) {
			this.immediateStarsTimerTicks = -1;
			return;
		}
		if (this.immediateStarsTimerTicks == -1)
			this.immediateStarsTimerTicks = 1;
		if (this.immediateStarsTimerTicks > 0)
			return;

		this.immediateStarsTimerTicks = -1;
		this.starSnapshotOrigin = camera.posTm;

		final var builder = BufferRenderer.immediateBuilder();
		builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);
		this.sectorTicket.attachedManager.forceLoad(this.sectorTicket);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			final var levelSize = GalaxySector.sizeForLevel(sector.pos().level());
			sector.initialElements.forEach(elem -> {
				if (elem.pos().distanceTo(camera.posTm) > levelSize)
					return;
				RenderHelper.addBillboard(builder, camera, elem.info().primaryStar, elem.pos());
			});
		});
		builder.end();

		this.starsBuffer.upload(builder);
	}

	private void drawStarsImmediate(CachedCamera<?> camera) {
		final var builder = BufferRenderer.immediateBuilder();
		final var batcher = new BillboardBatcher(builder, 10000);

		final var camPos = camera.pos.mul(camera.metersPerUnit / 1e12);

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
				if (toStar.dot(camera.forward) <= 0)
					return;
				batcher.add(elem.info().primaryStar, elem.pos());
			});
		});
		batcher.end();
	}

	private void drawStarsFromBuffer(CachedCamera<?> camera) {
		final var shader = getShader(SHADER_STAR_BILLBOARD);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));

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

		BufferRenderer.setupDefaultShaderUniforms(shader);
		this.starsBuffer.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
	}

	@Override
	public void close() {
		this.galaxyTicket.close();
		this.sectorTicket.close();
		this.starsBuffer.close();
	}

}
