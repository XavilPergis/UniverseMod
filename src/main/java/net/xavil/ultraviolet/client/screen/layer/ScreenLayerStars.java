package net.xavil.ultraviolet.client.screen.layer;

import org.lwjgl.glfw.GLFW;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.VecMath;
import net.xavil.ultraviolet.client.StarRenderManager;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.client.screen.StarStatisticsScreen;
import net.xavil.ultraviolet.client.screen.SystemExplorerScreen;
import net.xavil.ultraviolet.client.screen.SystemMapScreen;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorPos;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;

public class ScreenLayerStars extends HawkScreen3d.Layer3d {
	private final HawkScreen3d screen;
	public final Galaxy galaxy;
	private final StarRenderManager starRenderer;
	private final Vec3 originOffset;
	private boolean mapMode = true;

	public ScreenLayerStars(HawkScreen3d attachedScreen, Galaxy galaxy, Vec3 originOffset) {
		super(attachedScreen, new CameraConfig(1e2, false, 1e9, false));
		this.screen = attachedScreen;
		this.galaxy = galaxy;
		this.originOffset = originOffset;
		double[] scales = SectorTicketInfo.Multi.SCALES_EXP;
		// scales = SectorTicketInfo.Multi.SCALES_EXP_ADJUSTED;
		// scales = SectorTicketInfo.Multi.SCALES_UNIFORM;
		// scales = new double[] { 1, 2, 5, 12, 20, 40, 80, 512 };
		this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy,
				new SectorTicketInfo.Multi(Vec3.ZERO, GalaxySector.BASE_SIZE_Tm, scales)));
		this.starRenderer.setOriginOffset(this.originOffset);
	}

	private Vec3 getStarViewCenterPos(OrbitCamera.Cached camera) {
		// return camera.focus;
		if (this.mapMode)
			return camera.focus;
		return camera.pos.sub(this.originOffset).mul(camera.metersPerUnit / 1e12);
	}

	@Override
	public boolean handleClick(Vec2 mousePos, int button) {
		if (super.handleClick(mousePos, button))
			return true;

		if (button == 0) {
			final var selected = pickElement(this.camera, mousePos);
			insertBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM, selected.unwrapOrNull());
			return true;
		}

		return false;
	}

	@Override
	public boolean handleKeypress(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_TAB) {
			this.mapMode = !this.mapMode;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_K) {
			this.starRenderer.setMode(StarRenderManager.Mode.REALISTIC);
		} else if (keypress.keyCode == GLFW.GLFW_KEY_L) {
			this.starRenderer.setMode(StarRenderManager.Mode.MAP);
		} else if (keypress.keyCode == GLFW.GLFW_KEY_R) {
			final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
			if (selected != null) {
				final var disposer = new Disposable.Multi();
				final var ticket = this.galaxy.sectorManager.createSystemTicket(disposer, selected);
				this.galaxy.sectorManager.forceLoad(ticket);
				this.galaxy.sectorManager.getSystem(selected).ifSome(system -> {
					final var id = new SystemId(this.galaxy.galaxyId, selected);
					if (keypress.hasModifiers(GLFW.GLFW_MOD_SHIFT)) {
						final var screen = new SystemMapScreen(this.screen, this.galaxy, id, system);
						this.client.setScreen(screen);
					} else {
						final var screen = new SystemExplorerScreen(this.screen, this.galaxy, id, system);
						screen.camera.pitch.set(this.screen.camera.pitch.target);
						screen.camera.yaw.set(this.screen.camera.yaw.target);
						this.client.setScreen(screen);
					}
				});
				disposer.close();
				return true;
			}
		} else if (keypress.keyCode == GLFW.GLFW_KEY_H) {
			this.client.setScreen(new StarStatisticsScreen(this.screen, this.starRenderer.getSectorTicket(),
					this.camera.posTm.xyz()));

			return true;
		}

		return false;
	}

	private Maybe<GalaxySectorId> pickElement(OrbitCamera.Cached camera, Vec2 mousePos) {
		// @formatter:off
		final var closest = new Object() {
			double    depth       = Double.POSITIVE_INFINITY;
			int       sectorIndex = -1;
			SectorPos sectorPos   = null;
		};
		// @formatter:on

		final var window = this.client.getWindow();
		final var mouseX = 2.0 * mousePos.x / window.getGuiScaledWidth() - 1.0;
		final var mouseY = 1.0 - 2.0 * mousePos.y / window.getGuiScaledHeight();
		final var mousePosClip = new Vec2(mouseX, mouseY);

		final var viewCenter = getStarViewCenterPos(camera);

		final var mouseRay = camera.rayForPicking(window, mousePos);
		final var viewProjMatrix = Mat4.mul(camera.projectionMatrix, camera.viewMatrix);

		final var selectionRadius = 0.02;

		final var ticket = this.starRenderer.getSectorTicket();
		ticket.info.enumerateAffectedSectors(pos -> {
			final var sector = ticket.attachedManager.getSector(pos).unwrapOrNull();
			if (sector == null)
				return SectorTicketInfo.EnumerationAction.CONTINUE;

			// clip-space axis-aligned bounding rectangle containing the corners of the
			// sector's world space bounding box, with a little bit of padding to account
			// for the width of the selection column.

			final var sectorMinWorld = sector.pos().minBound().mul(1e12 / camera.metersPerUnit);
			final var sectorMaxWorld = sector.pos().maxBound().mul(1e12 / camera.metersPerUnit);
			if (!mouseRay.intersectAabb(sectorMinWorld, sectorMaxWorld)) {
				return SectorTicketInfo.EnumerationAction.SKIP_CHILDREN;
			}

			final var levelSize = ticket.info.radiusForLevel(sector.pos().level());

			final var elem = new GalaxySector.ElementHolder();
			final var point = new Vec3.Mutable(0, 0, 0);
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);

				// don't consider points that are in the current sector but outside of the
				// visual bubble around the 3d cursor position
				if (elem.systemPosTm.distanceTo(viewCenter) > levelSize)
					continue;

				// project point into clip space
				VecMath.transformPerspective(point, viewProjMatrix, elem.systemPosTm, 1);

				// select the closest star in a asmall column around the cursor position
				if (point.z > 1 || point.xy().distanceTo(mousePosClip) > selectionRadius)
					continue;

				if (point.z < closest.depth) {
					closest.depth = point.z;
					closest.sectorPos = sector.pos();
					closest.sectorIndex = i;
				}
			}

			return SectorTicketInfo.EnumerationAction.CONTINUE;
		});

		if (closest.sectorIndex == -1)
			return Maybe.none();
		return Maybe.some(GalaxySectorId.from(closest.sectorPos, closest.sectorIndex));
	}

	@Override
	public void render3d(Cached camera, RenderContext ctx) {
		final var cullingCamera = getCullingCamera();
		final var viewCenter = getStarViewCenterPos(cullingCamera);
		final var ticket = this.starRenderer.getSectorTicket();

		if (this.mapMode) {
			ticket.info.baseRadius = 3 * GalaxySector.BASE_SIZE_Tm;
			ticket.info.scales = SectorTicketInfo.Multi.SCALES_UNIFORM;
			this.starRenderer.setMode(StarRenderManager.Mode.MAP);
		} else {
			ticket.info.baseRadius = GalaxySector.BASE_SIZE_Tm;
			ticket.info.scales = SectorTicketInfo.Multi.SCALES_EXP;
			// this.starRenderer.setMode(StarRenderManager.Mode.MAP);
			this.starRenderer.setMode(StarRenderManager.Mode.REALISTIC);
		}
		this.starRenderer.draw(camera, viewCenter);

		final var selectedId = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
		final var selectedSystem = selectedId == null ? null
				: this.galaxy.sectorManager.getSystem(selectedId).unwrapOrNull();
		if (selectedSystem != null) {
			final var shader = UltravioletShaders.SHADER_UI_QUADS.get();
			shader.setupDefaultShaderUniforms();
			final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.QUAD_DUPLICATED, BufferLayout.POSITION_COLOR_TEX);

			final var distance = camera.pos.distanceTo(selectedSystem.pos);
			final Vec3 xo = camera.right.mul(0.05 * distance), yo = camera.up.mul(0.05 * distance);
			final var nn = selectedSystem.pos.sub(xo).sub(yo).sub(camera.pos);
			final var np = selectedSystem.pos.sub(xo).add(yo).sub(camera.pos);
			final var pn = selectedSystem.pos.add(xo).sub(yo).sub(camera.pos);
			final var pp = selectedSystem.pos.add(xo).add(yo).sub(camera.pos);

			final var color = ColorRgba.GREEN.withA(0.333f);
			builder.vertex(pn).color(color).uv0(1, 0).endVertex();
			builder.vertex(nn).color(color).uv0(0, 0).endVertex();
			builder.vertex(np).color(color).uv0(0, 1).endVertex();
			builder.vertex(pp).color(color).uv0(1, 1).endVertex();

			builder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(),
					HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

		}

		// TODO: render things that are currently jumping between systems

		if (ClientConfig.get(ConfigKey.SHOW_SECTOR_BOUNDARIES)) {
			final var builder = BufferRenderer.IMMEDIATE_BUILDER
					.beginGeneric(PrimitiveType.LINE_DUPLICATED, BufferLayout.POSITION_COLOR_NORMAL);
			ticket.attachedManager.enumerate(ticket, sector -> {
				if (!this.galaxy.sectorManager.isComplete(sector.pos()))
					;
				// if (!this.galaxy.sectorManager.isComplete(sector.pos()))
				// return;
				// if (sector.elements.size() == 0)
				// return;

				final Vec3 s = sector.pos().minBound(), e = sector.pos().maxBound();
				final var color = ClientConfig.getDebugColor(sector.pos().level()).withA(0.2f);

				final var nnn = new Vec3(s.x, s.y, s.z);
				final var nnp = new Vec3(s.x, s.y, e.z);
				final var npn = new Vec3(s.x, e.y, s.z);
				final var npp = new Vec3(s.x, e.y, e.z);
				final var pnn = new Vec3(e.x, s.y, s.z);
				final var pnp = new Vec3(e.x, s.y, e.z);
				final var ppn = new Vec3(e.x, e.y, s.z);
				final var ppp = new Vec3(e.x, e.y, e.z);

				// X
				RenderHelper.addLine(builder, camera, nnn, pnn, color);
				RenderHelper.addLine(builder, camera, nnp, pnp, color);
				RenderHelper.addLine(builder, camera, npn, ppn, color);
				RenderHelper.addLine(builder, camera, npp, ppp, color);
				// Y
				RenderHelper.addLine(builder, camera, nnn, npn, color);
				RenderHelper.addLine(builder, camera, nnp, npp, color);
				RenderHelper.addLine(builder, camera, pnn, ppn, color);
				RenderHelper.addLine(builder, camera, pnp, ppp, color);
				// Z
				RenderHelper.addLine(builder, camera, npn, npp, color);
				RenderHelper.addLine(builder, camera, nnn, nnp, color);
				RenderHelper.addLine(builder, camera, ppn, ppp, color);
				RenderHelper.addLine(builder, camera, pnn, pnp, color);
			});
			builder.end().draw(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_ADDITIVE_BLENDING);
		}
	}

}
