package net.xavil.universal.common.universe.universe;

import net.minecraft.client.Minecraft;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public final class ClientUniverse extends Universe {

	protected final Minecraft client;

	// these are synced from the server upon joining.
	private long commonUniverseSeed = 0;
	private long uniqueUniverseSeed = 0;
	private StartingSystemGalaxyGenerationLayer startingGenerator;

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

	@Override
	public StartingSystemGalaxyGenerationLayer getStartingSystemGenerator() {
		return this.startingGenerator;
	}

	public void updateFromInfoPacket(ClientboundUniverseInfoPacket packet) {
		this.commonUniverseSeed = packet.commonSeed;
		this.uniqueUniverseSeed = packet.uniqueSeed;
		this.startingGenerator = new StartingSystemGalaxyGenerationLayer(
				packet.startingId.system().galaxySector(),
				packet.startingId.system().systemSector().sectorPos(),
				packet.startingSystem,
				packet.startingId.nodeId());
	}

}
