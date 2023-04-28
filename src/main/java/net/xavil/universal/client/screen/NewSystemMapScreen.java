package net.xavil.universal.client.screen;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.camera.OrbitCamera.Cached;
import net.xavil.universal.client.screen.layer.ScreenLayerBackground;
import net.xavil.universal.client.screen.layer.ScreenLayerGalaxy;
import net.xavil.universal.client.screen.layer.ScreenLayerGrid;
import net.xavil.universal.client.screen.layer.ScreenLayerSystem;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.util.Units;
import net.xavil.util.math.Color;

public class NewSystemMapScreen extends Universal3dScreen {

	public static final double TM_PER_UNIT = Units.Tm_PER_au;
	private final SystemTicket systemTicket;

	public NewSystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen,
				new OrbitCamera(1e12), 1e-6, 4e3);

		this.systemTicket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId.galaxySector());
		galaxy.sectorManager.forceLoad(this.systemTicket);

		final var pos = galaxy.sectorManager.getInitial(systemId.galaxySector()).unwrap().pos();

		this.layers.push(new ScreenLayerBackground(this, Color.BLACK));
		this.layers.push(new ScreenLayerGrid(this));
		this.layers.push(new ScreenLayerGalaxy(this, galaxy, pos));
		this.layers.push(new ScreenLayerSystem(this, galaxy, systemId.galaxySector()));
	}

	public NewSystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemNodeId id, StarSystem system) {
		this(previousScreen, galaxy, id.system(), system);
		// this.selectedId = this.followingId = id.nodeId();
	}

	@Override
	public Cached setupCamera(CameraConfig config, float partialTick) {
		return this.camera.cached(config, partialTick);
	}

}
