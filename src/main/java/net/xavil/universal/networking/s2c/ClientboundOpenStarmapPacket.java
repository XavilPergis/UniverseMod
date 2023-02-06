package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundOpenStarmapPacket extends ModPacket {

	public UniverseId toOpen;

	public static ClientboundOpenStarmapPacket empty() {
		return new ClientboundOpenStarmapPacket();
	};

	private ClientboundOpenStarmapPacket() {
	}

	public ClientboundOpenStarmapPacket(UniverseId toOpen) {
		this.toOpen = toOpen;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.toOpen = readUniverseId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeUniverseId(buf, this.toOpen);
	}

}
