package net.xavil.ultraviolet.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGalaxy;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGrid;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerStars;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerSystemInfo;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec3;

public class GalaxyMapScreen extends HawkScreen3d {

	public GalaxyMapScreen(Screen previousScreen, Galaxy galaxy, GalaxySectorId systemToFocus) {
		super(new TranslatableComponent("narrator.screen.starmap"), previousScreen, new OrbitCamera(1e12),
				1e3, 1e9);

		BlackboardKeys.SELECTED_STAR_SYSTEM.insert(blackboard, systemToFocus);

		this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));
		this.layers.push(new ScreenLayerGrid(this));
		this.layers.push(new ScreenLayerGalaxy(this, galaxy, Vec3.ZERO));

		try (final var tempDisposer = Disposable.scope()) {
			final var tempTicket = galaxy.sectorManager.createSectorTicket(tempDisposer,
					SectorTicketInfo.single(systemToFocus.sectorPos()));
			galaxy.sectorManager.forceLoad(tempTicket);

			final var elem = new GalaxySector.SectorElementHolder();
			var pos = Vec3.ZERO;
			if (galaxy.sectorManager.loadElement(elem, systemToFocus)) {
				pos = elem.systemPosTm.xyz();
			} else {
				Mod.LOGGER.error("Tried to open starmap to nonexistent id {}", systemToFocus);
			}

			this.camera.focus.set(pos);
			this.layers.push(new ScreenLayerStars(this, galaxy, Vec3.ZERO));
		}

		this.layers.push(new ScreenLayerSystemInfo(this, galaxy));
	}

	@Override
	public Cached setupCamera(CameraConfig config, float partialTick) {
		return this.camera.cached(config, partialTick);
	}

}
