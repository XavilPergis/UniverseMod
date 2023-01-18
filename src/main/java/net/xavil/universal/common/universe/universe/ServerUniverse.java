package net.xavil.universal.common.universe.universe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

public final class ServerUniverse extends Universe {

	protected final MinecraftServer server;

	public ServerUniverse(MinecraftServer server) {
		this.server = server;
	}

	// TODO: allow changing the universe seed via configs
	// the default seed is shared between all instances of the mod, because i think
	// it would be cool to be able to go "hey check out this world i found" and give
	// people the ability to just. go there.
	@Override
	public long getCommonUniverseSeed() {
		// floof :3
		return 0xf100f;
	}

	@Override
	public long getUniqueUniverseSeed() {
		var worldData = MinecraftServerAccessor.getWorldData(this.server);
		var worldSeed = worldData.worldGenSettings().seed();
		return Mth.murmurHash3Mixer(getCommonUniverseSeed()) ^ Mth.murmurHash3Mixer(worldSeed);
	}

}
