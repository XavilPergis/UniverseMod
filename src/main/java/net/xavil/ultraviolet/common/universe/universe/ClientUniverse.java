package net.xavil.ultraviolet.common.universe.universe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.common.universe.galaxy.StartingSystemGalaxyGenerationLayer;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.station.SpaceStation;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundStationJumpBeginPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseSyncPacket;

public final class ClientUniverse extends Universe {

	protected final Minecraft client;

	// these are synced from the server upon joining.
	private long commonUniverseSeed = 0;
	private long uniqueUniverseSeed = 0;
	private StartingSystemGalaxyGenerationLayer startingGenerator;

	protected SystemTicket startingSystemTicket = null;

	public ClientUniverse() {
		super(Side.CLIENT);
		this.client = Minecraft.getInstance();
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

	@Override
	public Level createLevelForStation(String name, int id) {
		return null;
	}

	public void updateFromInfoPacket(ClientboundUniverseSyncPacket packet) {
		this.commonUniverseSeed = packet.commonSeed;
		this.uniqueUniverseSeed = packet.uniqueSeed;

		try (final var disposer = Disposable.scope()) {
			final var sectorPos = packet.startingId.system().universeSector();
			final var tempTicket = this.sectorManager.createGalaxyTicket(disposer, sectorPos);
			final var galaxy = this.sectorManager.forceLoad(tempTicket).unwrap();

			final var node = CelestialNode.readNbt(packet.startingSystemNbt);
			this.startingGenerator = new StartingSystemGalaxyGenerationLayer(galaxy,
					packet.startingSystemAge,
					packet.startingSystemName,
					node, packet.startingId.nodeId());
			addStartingGenerationLayers(galaxy);
			final var startingId = this.startingGenerator.getStartingSystemId();

			this.startingSystemTicket = galaxy.sectorManager.createSystemTicket(this.disposer,
					startingId.system().galaxySector());
			galaxy.sectorManager.forceLoad(this.startingSystemTicket);
		}
	}

	public void applyPacket(ClientboundSyncCelestialTimePacket packet) {
		this.celestialTime = packet.celestialTime;
		if (packet.isDiscontinuous) {
			this.lastCelestialTime = this.celestialTime;
			final var worldType = EntityAccessor.getWorldType(this.client.player);
			final var system = getSystem(worldType.getSystemId(this)).unwrapOrNull();
			if (system != null) {
				system.rootNode.updatePositions(this.celestialTime);
				system.rootNode.visit(node -> Vec3.set(node.lastPosition, node.position));
			}
		}
		this.celestialTimeRate = packet.celestialTimeRate;
	}

	public void applyPacket(ClientboundStationJumpBeginPacket packet) {
		// TODO: idk should figure this out at some point prolly
		final var entry = this.spaceStations.entry(packet.stationId);
		if (entry.exists()) {
			// final var station = entry.get().unwrap();
			// station.prepareForJump(packet.target, packet.isJumpInstant);
		}
	}

	public void applyPacket(ClientboundSpaceStationInfoPacket packet) {
		final var entry = this.spaceStations.entry(packet.id);
		if (entry.exists()) {
			final var station = entry.get().unwrap();
			station.name = packet.name;
			station.orientation = packet.orientation;
			station.position = packet.position;
		} else {
			final var name = packet.name;
			final var station = new SpaceStation(this, null, name, packet.position);
			station.orientation = packet.orientation;
			entry.insert(station);
		}
	}

}
