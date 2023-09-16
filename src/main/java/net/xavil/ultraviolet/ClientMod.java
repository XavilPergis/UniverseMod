package net.xavil.ultraviolet;


import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.HawkTextureManager;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.shader.AttributeSet;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.GalaxyMapScreen;
import net.xavil.ultraviolet.client.screen.SystemMapScreen;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.ClientUniverse;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.ultraviolet.networking.ModNetworking;
import net.xavil.ultraviolet.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundDebugValueSetPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundStationJumpBeginPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseSyncPacket;

public class ClientMod implements ClientModInitializer {

	public static final Minecraft CLIENT = Minecraft.getInstance();

	@Override
	public void onInitializeClient() {
		ModNetworking.addClientboundHandler(ClientboundOpenStarmapPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundUniverseSyncPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundChangeSystemPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundSyncCelestialTimePacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundSpaceStationInfoPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundStationJumpBeginPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundDebugValueSetPacket.class, CLIENT, ClientConfig::applyPacket);

		HawkRendering.LOAD_SHADERS_EVENT.register(acceptor -> {
			// @formatter:off
			acceptor.accept(UltravioletShaders.SHADER_STAR_LOCATION,                     AttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_STAR_BILLBOARD_REALISTIC_LOCATION, AttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_STAR_BILLBOARD_UI_LOCATION,        AttributeSet.POSITION_COLOR, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_PLANET_LOCATION,                   AttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_RING_LOCATION,                     AttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_GALAXY_PARTICLE_LOCATION,          AttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_SKYBOX_LOCATION,                   AttributeSet.POSITION, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_BLIT_LOCATION,                     AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			
			acceptor.accept(UltravioletShaders.SHADER_BLOOM_DOWNSAMPLE_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_BLOOM_UPSAMPLE_LOCATION,   AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_MAIN_POSTPROCESS_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

			acceptor.accept(UltravioletShaders.SHADER_ATMOSPHERE_LOCATION,            AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_GRAVITATIONAL_LENSING_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			// @formatter:on
		});

		HawkTextureManager.REGISTER_ATLASES.register(ctx -> {
			// final var planetsAtlas =
			// ctx.registerAtlas(PlanetRenderingContext.PLANET_ATLAS_LOCATION);
			// planetsAtlas.registerSprite("path/to/sprite");
		});
	}

	public static void modifySkyColor(Vec3.Mutable skyColor, float partialTick) {
		if (CLIENT.level == null)
			return;
		final var universe = MinecraftClientAccessor.getUniverse();
		final var loc = LevelAccessor.getWorldType(CLIENT.level);
		if (loc instanceof WorldType.SystemNode world) {
			final var system = universe.getSystem(world.id.system()).unwrapOrNull();
			if (system != null)
				modifySkyColorInSystem(skyColor, system, partialTick);
		} else if (loc instanceof WorldType.Station stationLoc) {
			final var station = universe.getStation(stationLoc.id).unwrapOrNull();
			if (station == null)
				return;
			if (station.getLocation() instanceof StationLocation.OrbitingCelestialBody sloc) {
				final var system = universe.getSystem(sloc.id.system()).unwrapOrNull();
				if (system != null)
					modifySkyColorInSystem(skyColor, system, partialTick);
			} else if (station.getLocation() instanceof StationLocation.JumpingSystem sloc) {
				modifySkyColorJumpingSystem(skyColor);
			}
		}
	}

	private static void modifySkyColorInSystem(Vec3.Mutable skyColor, StarSystem system, float partialTick) {
		skyColor.x = 0;
		skyColor.y = 1;
		skyColor.z = 0;
		// final var tfm = new TransformStack();
		// SkyRenderer.INSTANCE.applyCelestialTransform(tfm, xz, loc, partialTick);
		// for (final var node : system.rootNode.selfAndChildren().iterable()) {
		// 	// TODO: calculate occlusion and stuff
		// }
	}

	private static void modifySkyColorJumpingSystem(Vec3.Mutable skyColor) {
	}

	public static void handlePacket(ClientboundOpenStarmapPacket packet) {
		final var universe = MinecraftClientAccessor.getUniverse();

		try (final var disposer = Disposable.scope()) {
			final var systemId = packet.toOpen.system();
			final var galaxyTicket = universe.sectorManager.createGalaxyTicket(disposer, systemId.universeSector());
			final var galaxy = universe.sectorManager.forceLoad(galaxyTicket).unwrap();
			final var systemTicket = galaxy.sectorManager.createSystemTicket(disposer, systemId.galaxySector());
			final var system = galaxy.sectorManager.forceLoad(systemTicket).unwrap();

			final var galaxyMap = new GalaxyMapScreen(CLIENT.screen, galaxy, systemId.galaxySector());
			final var systemMap = new SystemMapScreen(galaxyMap, galaxy, packet.toOpen, system);
			CLIENT.setScreen(systemMap);
		}
	}

	public static void handlePacket(ClientboundUniverseSyncPacket packet) {
		final var universe = new ClientUniverse();
		universe.updateFromInfoPacket(packet);
		MinecraftClientAccessor.setUniverse(universe);
		LevelAccessor.setUniverse(CLIENT.level, universe);
		LevelAccessor.setWorldType(CLIENT.level, packet.worldType);
	}

	public static void handlePacket(ClientboundChangeSystemPacket packet) {
		final var universe = MinecraftClientAccessor.getUniverse();
		LevelAccessor.setUniverse(CLIENT.level, universe);
		LevelAccessor.setWorldType(CLIENT.level, packet.location);
	}

	public static void handlePacket(ClientboundSyncCelestialTimePacket packet) {
		MinecraftClientAccessor.getUniverse().applyPacket(packet);
	}

	public static void handlePacket(ClientboundSpaceStationInfoPacket packet) {
		MinecraftClientAccessor.getUniverse().applyPacket(packet);
	}

	public static void handlePacket(ClientboundStationJumpBeginPacket packet) {
		MinecraftClientAccessor.getUniverse().applyPacket(packet);
	}

}
