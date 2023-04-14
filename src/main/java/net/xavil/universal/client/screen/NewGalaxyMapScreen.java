package net.xavil.universal.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.universal.Mod;
import net.xavil.universal.client.screen.OrbitCamera.Cached;
import net.xavil.universal.client.screen.layer.ScreenLayerBackground;
import net.xavil.universal.client.screen.layer.ScreenLayerGalaxy;
import net.xavil.universal.client.screen.layer.ScreenLayerGrid;
import net.xavil.universal.client.screen.layer.ScreenLayerStars;
import net.xavil.universal.client.screen.layer.ScreenLayerSystemInfo;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.util.Disposable;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec3;

public class NewGalaxyMapScreen extends Universal3dScreen {
	public static final double TM_PER_UNIT = 1000;

	private final Galaxy galaxy;

	public NewGalaxyMapScreen(Screen previousScreen, Galaxy galaxy, GalaxySectorId systemToFocus) {
		super(new TranslatableComponent("narrator.screen.starmap"), previousScreen, new OrbitCamera(1e12, TM_PER_UNIT),
				1e0, 1e5);

		BlackboardKeys.SELECTED_STAR_SYSTEM.insert(blackboard, systemToFocus);

		this.layers.push(new ScreenLayerBackground(this, Color.BLACK));
		this.layers.push(new ScreenLayerGrid(this));
		this.layers.push(new ScreenLayerGalaxy(this, galaxy, Vec3.ZERO));

		this.galaxy = galaxy;
		Disposable.scope(tempDisposer -> {
			final var tempTicket = galaxy.sectorManager.createSectorTicket(tempDisposer,
					SectorTicketInfo.single(systemToFocus.sectorPos()));
			galaxy.sectorManager.forceLoad(tempTicket);
			final var initial = galaxy.sectorManager.getInitial(systemToFocus);
			if (initial.isNone()) {
				Mod.LOGGER.error("Tried to open starmap to nonexistent id {}", systemToFocus);
			}

			final var initialPos = initial.map(i -> i.pos()).unwrapOr(Vec3.ZERO);
			this.camera.focus.set(initialPos);

			this.layers.push(new ScreenLayerStars(this, galaxy, SectorTicketInfo.visual(initialPos)));
		});

		this.layers.push(new ScreenLayerSystemInfo(this, galaxy));
	}

	@Override
	public Cached setupCamera(float partialTick) {
		return this.camera.cached(partialTick);
	}

}
