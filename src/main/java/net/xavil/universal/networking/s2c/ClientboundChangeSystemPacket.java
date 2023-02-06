package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundChangeSystemPacket extends ModPacket {
	
	public UniverseId id;

	public static ClientboundChangeSystemPacket empty() {
		return new ClientboundChangeSystemPacket();
	};

	@Override
	public void read(FriendlyByteBuf buf) {
		this.id = readUniverseId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeUniverseId(buf, this.id);
	}

}
