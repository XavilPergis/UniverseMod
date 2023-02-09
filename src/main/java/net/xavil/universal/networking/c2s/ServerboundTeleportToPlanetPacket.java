package net.xavil.universal.networking.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.ModPacket;

public class ServerboundTeleportToPlanetPacket extends ModPacket {

	public static final ResourceLocation CHANNEL = Mod.namespaced("teleport_to_planet");

	public UniverseId planetId;

	@Override
	public ResourceLocation getChannelName() {
		return CHANNEL;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.planetId = readUniverseId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeUniverseId(buf, this.planetId);
	}
	
}
