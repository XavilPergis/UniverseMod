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
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarRenderManager implements Disposable {
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

	public SectorTicket<SectorTicketInfo.Multi> getSectorTicket() {
		return this.sectorTicket;
	}

	private double getRadius() {
		return 1.5;
	}

	public void draw(CachedCamera<?> camera, Vec3 centerPos) {
		if (this.sectorTicket.info == null)
			this.sectorTicket.info = SectorTicketInfo.visual(centerPos);
		this.sectorTicket.info.centerPos = centerPos;
		this.sectorTicket.info.baseRadius = getRadius() * GalaxySector.BASE_SIZE_Tm;

		if (ClientConfig.get(ConfigKey.FORCE_STAR_RENDERER_IMMEDIATE_MODE)) {
			drawStarsImmediate(camera, centerPos);
			this.starSnapshotOrigin = null;
		} else {
			buildStarsIfNeeded(camera, centerPos);
			if (this.immediateStarsTimerTicks != -1) {
				drawStarsImmediate(camera, centerPos);
			} else {
				drawStarsFromBuffer(camera, centerPos);
			}
		}
	}

	// TODO: build stars when all the sectors are finished generating instead of
	// having a fixed cooldown
	private void buildStarsIfNeeded(CachedCamera<?> camera, Vec3 centerPos) {
		if (this.starSnapshotOrigin != null
				&& centerPos.distanceTo(this.starSnapshotOrigin) < this.starSnapshotThreshold) {
			this.immediateStarsTimerTicks = -1;
			return;
		}
		if (this.immediateStarsTimerTicks == -1)
			this.immediateStarsTimerTicks = 10;
		if (this.immediateStarsTimerTicks > 0)
			return;

		this.starSnapshotOrigin = centerPos;
		this.immediateStarsTimerTicks = -1;

		final var colorHolder = new Vec3.Mutable();
		final var elem = new GalaxySector.SectorElementHolder();

		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		this.sectorTicket.attachedManager.forceLoad(this.sectorTicket);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			final var levelSize = this.sectorTicket.info.baseRadius * (1 << sector.level);
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				if (elem.systemPosTm.distanceTo(centerPos) > levelSize)
					continue;
				StellarCelestialNode.blackBodyColorFromTable(colorHolder, elem.temperatureK);
				elem.systemPosTm
						.addAssign(this.originOffset)
						.mulAssign(1e12 / camera.metersPerUnit);
				builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.uv0((float) elem.luminosityLsol, 0)
						.endVertex();
			}
		});
		this.starsBuffer.upload(builder.end());
	}

	private void drawStarsImmediate(CachedCamera<?> camera, Vec3 centerPos) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		final var batcher = new BillboardBatcher(builder, 10000);

		final var colorHolder = new Vec3.Mutable();
		final var toStar = new Vec3.Mutable();
		final var elem = new GalaxySector.SectorElementHolder();

		// final var camPos = camera.posTm.add(this.originOffset);
		batcher.begin(camera);
		this.sectorTicket.attachedManager.enumerate(this.sectorTicket, sector -> {
			// final var min = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			// final var max = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			// if (!camera.isAabbInFrustum(min, max))
			// return;
			final var levelSize = this.sectorTicket.info.baseRadius * (1 << sector.level);
			// if (sector.level != 0) return;

			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				if (elem.systemPosTm.distanceTo(centerPos) > levelSize)
					continue;
				toStar.load(elem.systemPosTm).subAssign(centerPos);
				if (toStar.dot(camera.forward) == 0)
					return;
				// RenderHelper.addStarPoint(builder, elem.info().primaryStar,
				// elem.pos().add(this.originOffset).mul(1e12 /
				// camera.metersPerUnit).sub(camera.pos));
				final var aaaaaa = new Vec3.Mutable(elem.systemPosTm);
				StellarCelestialNode.blackBodyColorFromTable(colorHolder, elem.temperatureK);
				elem.systemPosTm.addAssign(this.originOffset)
						.mulAssign(1e12 / camera.metersPerUnit)
						.subAssign(camera.pos);
				builder.vertex(elem.systemPosTm)
						.color((float) colorHolder.x, (float) colorHolder.y, (float) colorHolder.z, 1)
						.uv0((float) elem.luminosityLsol, 0)
						.endVertex();

				batcher.emitIfNeeded();
			}
			// sector.initialElements.forEach(elem -> {
			// if (elem.pos().distanceTo(centerPos) > levelSize)
			// return;
			// final var toStar = elem.pos().sub(centerPos);
			// if (toStar.dot(camera.forward) == 0)
			// return;
			// batcher.add(elem.info().primaryStar, elem.pos().add(this.originOffset));
			// });
		});
		batcher.end();
	}

	private void drawStarsFromBuffer(CachedCamera<?> camera, Vec3 centerPos) {
		final var snapshot = camera.setupRenderMatrices();

		{
			final var poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();

			poseStack.mulPose(camera.orientation.toMinecraft());
			poseStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
			// poseStack.translate(org.x, org.y, org.z);
			final var inverseViewRotationMatrix = poseStack.last().normal().copy();
			if (inverseViewRotationMatrix.invert()) {
				RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
			}

			RenderSystem.applyModelViewMatrix();
		}

		final var shader = getShader(SHADER_STAR_BILLBOARD);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		BufferRenderer.setupDefaultShaderUniforms(shader);
		setupStarShader(shader, camera);
		this.starsBuffer.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

		snapshot.restore();
	}

	public static void setupStarShader(ShaderProgram shader, CachedCamera<?> camera) {
		final var universe = MinecraftClientAccessor.getUniverse(Minecraft.getInstance());
		final var partialTick = Minecraft.getInstance().getFrameTime();
		// shader.setUniformSampler("uBillboardTexture",
		// GlTexture2d.importTexture(RenderHelper.STAR_ICON_LOCATION));
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
