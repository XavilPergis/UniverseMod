package net.xavil.ultraviolet.client.screen;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGalaxy;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGrid;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerStars;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerSystem;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.hawklib.math.ColorRgba;

public class SystemExplorerScreen extends HawkScreen3d {

	public SystemExplorerScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen,
				new OrbitCamera(1e12), 1e-6, 4e3);

		this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));
		this.layers.push(new ScreenLayerGrid(this));
		this.layers.push(new ScreenLayerGalaxy(this, galaxy, system.pos));
		this.layers.push(new ScreenLayerStars(this, galaxy, system.pos));
		this.layers.push(new ScreenLayerSystem(this, galaxy, systemId.galaxySector()));
	}

	public SystemExplorerScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemNodeId id, StarSystem system) {
		this(previousScreen, galaxy, id.system(), system);
	}

	@Override
	public Cached setupCamera(CameraConfig config, float partialTick) {
		return this.camera.cached(config, partialTick);
	}

}
