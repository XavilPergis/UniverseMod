package net.xavil.ultraviolet;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.HawkTextureManager;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.NewGalaxyMapScreen;
import net.xavil.ultraviolet.client.screen.NewSystemMapScreen;
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
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseInfoPacket;

public class ClientMod implements ClientModInitializer {

	public static final Minecraft CLIENT = Minecraft.getInstance();

	@Override
	public void onInitializeClient() {
		ModNetworking.addClientboundHandler(ClientboundOpenStarmapPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundUniverseInfoPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundChangeSystemPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundSyncCelestialTimePacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundSpaceStationInfoPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundStationJumpBeginPacket.class, CLIENT, ClientMod::handlePacket);
		ModNetworking.addClientboundHandler(ClientboundDebugValueSetPacket.class, CLIENT, ClientConfig::applyPacket);

		HawkRendering.LOAD_SHADERS_EVENT.register(acceptor -> {
			// @formatter:off
			acceptor.accept(UltravioletShaders.SHADER_STAR,             DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_STAR_BILLBOARD,   DefaultVertexFormat.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_PLANET,           DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_RING,             DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_GALAXY_PARTICLE,  DefaultVertexFormat.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_SKYBOX,           DefaultVertexFormat.POSITION, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_BLIT,             DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			
			acceptor.accept(UltravioletShaders.SHADER_BLOOM_DOWNSAMPLE, DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_BLOOM_UPSAMPLE,   DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(UltravioletShaders.SHADER_MAIN_POSTPROCESS, DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			// @formatter:off
		});

		HawkTextureManager.REGISTER_ATLASES.register(ctx -> {
			// final var planetsAtlas = ctx.registerAtlas(PlanetRenderingContext.PLANET_ATLAS_LOCATION);
			// planetsAtlas.registerSprite("path/to/sprite");
		});
	}

	public static void handlePacket(ClientboundOpenStarmapPacket packet) {
		final var universe = MinecraftClientAccessor.getUniverse(CLIENT);

		try (final var disposer = Disposable.scope()) {
			final var systemId = packet.toOpen.system();
			final var galaxyTicket = universe.sectorManager.createGalaxyTicket(disposer, systemId.universeSector());
			final var galaxy = universe.sectorManager.forceLoad(galaxyTicket).unwrap();
			final var systemTicket = galaxy.sectorManager.createSystemTicket(disposer, systemId.galaxySector());
			final var system = galaxy.sectorManager.forceLoad(systemTicket).unwrap();

			final var galaxyMap = new NewGalaxyMapScreen(CLIENT.screen, galaxy, systemId.galaxySector());
			final var systemMap = new NewSystemMapScreen(galaxyMap, galaxy, packet.toOpen, system);
			CLIENT.setScreen(systemMap);
		}
	}

	public static void handlePacket(ClientboundUniverseInfoPacket packet) {
		MinecraftClientAccessor.getUniverse(CLIENT).updateFromInfoPacket(packet);
	}

	public static void handlePacket(ClientboundChangeSystemPacket packet) {
		LevelAccessor.setLocation(CLIENT.level, packet.location);
	}

	public static void handlePacket(ClientboundSyncCelestialTimePacket packet) {
		MinecraftClientAccessor.getUniverse(CLIENT).applyPacket(packet);
	}

	public static void handlePacket(ClientboundSpaceStationInfoPacket packet) {
		MinecraftClientAccessor.getUniverse(CLIENT).applyPacket(packet);
	}

	public static void handlePacket(ClientboundStationJumpBeginPacket packet) {
		MinecraftClientAccessor.getUniverse(CLIENT).applyPacket(packet);
	}

}
