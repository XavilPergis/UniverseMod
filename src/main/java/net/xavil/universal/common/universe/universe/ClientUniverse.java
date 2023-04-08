package net.xavil.universal.common.universe.universe;

import net.minecraft.client.Minecraft;
import net.xavil.universal.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;
import net.xavil.util.Disposable;
import net.xavil.util.math.Vec3i;

public final class ClientUniverse extends Universe {

	protected final Minecraft client;

	// these are synced from the server upon joining.
	private long commonUniverseSeed = 0;
	private long uniqueUniverseSeed = 0;
	private StartingSystemGalaxyGenerationLayer startingGenerator;

	protected SystemTicket startingSystemTicket = null;

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

		Disposable.scope(disposer -> {
			final var sectorPos = packet.startingId.system().galaxySector();
			final var tempTicket = this.sectorManager.createGalaxyTicket(disposer, sectorPos);
			final var galaxy = this.sectorManager.forceLoad(tempTicket).unwrap();

			this.startingGenerator = new StartingSystemGalaxyGenerationLayer(galaxy, packet.startingSystem,
					packet.startingId.nodeId());
			galaxy.addGenerationLayer(this.startingGenerator);
			final var startingId = this.startingGenerator.getStartingSystemId();

			this.startingSystemTicket = galaxy.sectorManager.createSystemTicket(this.disposer,
					startingId.system().systemSector());
			galaxy.sectorManager.forceLoad(this.startingSystemTicket);
		});

	}

}
