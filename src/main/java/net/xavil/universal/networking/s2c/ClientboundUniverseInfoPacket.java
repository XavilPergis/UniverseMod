package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystemNode;
import net.xavil.universal.networking.ModPacket;

public class ClientboundUniverseInfoPacket extends ModPacket {

	public static final ResourceLocation CHANNEL = Mod.namespaced("universe_info");

	public long commonSeed;
	public long uniqueSeed;
	public SystemNodeId startingId;
	public StarSystemNode startingSystem;

	@Override
	public ResourceLocation getChannelName() {
		return CHANNEL;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.commonSeed = buf.readLong();
		this.uniqueSeed = buf.readLong();
		this.startingId = readSystemNodeId(buf);
		this.startingSystem = StarSystemNode.readNbt(buf.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(this.commonSeed);
		buf.writeLong(this.uniqueSeed);
		writeSystemNodeId(buf, this.startingId);
		buf.writeNbt(StarSystemNode.writeNbt(this.startingSystem));
	}

}
