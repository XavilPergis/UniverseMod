package net.xavil.universal.common.universe.universe;

import net.minecraft.client.Minecraft;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public final class ClientUniverse extends Universe {
	
	protected final Minecraft client;

	// these are synced from the server upon joining.
	private long commonUniverseSeed = 0;
	private long uniqueUniverseSeed = 0;
	private UniverseId.SectorId startingGalaxyId;

	public ClientUniverse(Minecraft client) {
		this.client = client;
	}

	@Override
	public long getCommonUniverseSeed() {
		return this.commonUniverseSeed;
	}

	@Override
	public long getUniqueUniverseSeed() {
		return this.uniqueUniverseSeed;
	}

	public void updateFromInfoPacket(ClientboundUniverseInfoPacket packet) {
		this.commonUniverseSeed = packet.commonSeed;
		this.uniqueUniverseSeed = packet.uniqueSeed;
		this.startingGalaxyId = packet.startingGalaxyId;
	}

}
