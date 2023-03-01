package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.networking.ModPacket;

public class ClientboundSyncCelestialTimePacket extends ModPacket<ClientGamePacketListener> {

	public long celestialTimeTicks;

	public ClientboundSyncCelestialTimePacket() {
	}

	public ClientboundSyncCelestialTimePacket(long celestialTimeTicks) {
		this.celestialTimeTicks = celestialTimeTicks;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.celestialTimeTicks = buf.readLong();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(this.celestialTimeTicks);
	}

}
