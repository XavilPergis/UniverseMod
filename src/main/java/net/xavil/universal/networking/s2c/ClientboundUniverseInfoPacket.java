package net.xavil.universal.networking.s2c;

import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.networking.ModPacket;

public class ClientboundUniverseInfoPacket extends ModPacket {

	public static final ResourceLocation CHANNEL = Mod.namespaced("universe_info");

	public long commonSeed;
	public long uniqueSeed;

	public UniverseId.SectorId startingGalaxyId;
	public Vec3i startingSystemVolumePos;
	public StarSystemNode startingSystem;
	public int startingNodeId;

	@Override
	public ResourceLocation getChannelName() {
		return CHANNEL;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.commonSeed = buf.readLong();
		this.uniqueSeed = buf.readLong();
		this.startingGalaxyId = readSectorId(buf);
		this.startingSystemVolumePos = readVector(buf);
		this.startingSystem = StarSystemNode.readNbt(buf.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(this.commonSeed);
		buf.writeLong(this.uniqueSeed);
		writeSectorId(buf, this.startingGalaxyId);
		writeVector(buf, this.startingSystemVolumePos);
		buf.writeNbt(StarSystemNode.writeNbt(this.startingSystem));
	}

}
