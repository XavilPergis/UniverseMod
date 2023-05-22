package net.xavil.ultraviolet;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.client.ModRendering;
import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.ultraviolet.client.Shaders;
import net.xavil.ultraviolet.client.UltravioletTextureManager;
import net.xavil.ultraviolet.client.UltravioletTextureManager.SpriteRegistrationContext;
import net.xavil.ultraviolet.client.gl.GlFragmentWrites;
import net.xavil.ultraviolet.client.screen.NewGalaxyMapScreen;
import net.xavil.ultraviolet.client.screen.NewSystemMapScreen;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.ultraviolet.networking.ModNetworking;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundStationJumpBeginPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseInfoPacket;
import net.xavil.util.Disposable;

public class ClientMod implements ClientModInitializer {

	private void registerCelestialBody(SpriteRegistrationContext ctx, String name) {
		final var prefix = "ultraviolet:misc/celestialbodies/" + name;
		ctx.registerSprite(prefix + "/base_color/nx");
		ctx.registerSprite(prefix + "/base_color/px");
		ctx.registerSprite(prefix + "/base_color/ny");
		ctx.registerSprite(prefix + "/base_color/py");
		ctx.registerSprite(prefix + "/base_color/nz");
		ctx.registerSprite(prefix + "/base_color/pz");
		ctx.registerSprite(prefix + "/normal/nx");
		ctx.registerSprite(prefix + "/normal/px");
		ctx.registerSprite(prefix + "/normal/ny");
		ctx.registerSprite(prefix + "/normal/py");
		ctx.registerSprite(prefix + "/normal/nz");
		ctx.registerSprite(prefix + "/normal/pz");
	}

	@Override
	public void onInitializeClient() {
		final var client = Minecraft.getInstance();

		ModNetworking.CLIENTBOUND_PLAY_HANDLER = packet -> client.execute(() -> handlePacket(packet));

		ModRendering.LOAD_SHADERS_EVENT.register(acceptor -> {
			// @formatter:off
			acceptor.accept(Shaders.SHADER_STAR,             DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_STAR_BILLBOARD,   DefaultVertexFormat.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_PLANET,           DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_RING,             DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_GALAXY_PARTICLE,  DefaultVertexFormat.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_SKYBOX,           DefaultVertexFormat.POSITION, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_BLIT,             DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			
			acceptor.accept(Shaders.SHADER_BLOOM_DOWNSAMPLE, DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_BLOOM_UPSAMPLE,   DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			acceptor.accept(Shaders.SHADER_MAIN_POSTPROCESS, DefaultVertexFormat.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
			// @formatter:off
		});

		UltravioletTextureManager.REGISTER_ATLASES.register(ctx -> {
			// final var planetsAtlas = ctx.registerAtlas(PlanetRenderingContext.PLANET_ATLAS_LOCATION);
			// registerCelestialBody(planetsAtlas, "moon");
		});
	}

	public static void handlePacket(ModPacket<ClientGamePacketListener> packetUntyped) {
		final var client = Minecraft.getInstance();
		final var universe = MinecraftClientAccessor.getUniverse(client);

		if (packetUntyped instanceof ClientboundOpenStarmapPacket packet) {
			try (final var disposer = Disposable.scope()) {
				final var systemId = packet.toOpen.system();
				final var galaxyTicket = universe.sectorManager.createGalaxyTicket(disposer, systemId.universeSector());
				final var galaxy = universe.sectorManager.forceLoad(galaxyTicket).unwrap();
				final var systemTicket = galaxy.sectorManager.createSystemTicket(disposer, systemId.galaxySector());
				final var system = galaxy.sectorManager.forceLoad(systemTicket).unwrap();
	
				final var galaxyMap = new NewGalaxyMapScreen(client.screen, galaxy, systemId.galaxySector());
				final var systemMap = new NewSystemMapScreen(galaxyMap, galaxy, packet.toOpen, system);
				client.setScreen(systemMap);
			}
		} else if (packetUntyped instanceof ClientboundUniverseInfoPacket packet) {
			universe.updateFromInfoPacket(packet);
		} else if (packetUntyped instanceof ClientboundChangeSystemPacket packet) {
			LevelAccessor.setLocation(client.level, packet.location);
		} else if (packetUntyped instanceof ClientboundSyncCelestialTimePacket packet) {
			universe.applyPacket(packet);
		} else if (packetUntyped instanceof ClientboundSpaceStationInfoPacket packet) {
			universe.applyPacket(packet);
		} else if (packetUntyped instanceof ClientboundStationJumpBeginPacket packet) {
			universe.applyPacket(packet);
		}
	}

}
