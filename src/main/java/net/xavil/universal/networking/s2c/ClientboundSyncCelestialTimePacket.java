package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.networking.ModPacket;

public class ClientboundSyncCelestialTimePacket extends ModPacket<ClientGamePacketListener> {

	public double celestialTime;
	/**
	 * How many seconds pass in-game per one real-life second.
	 */
	public double celestialTimeRate = 1;
	/**
	 * Was there a jump in celestial time that the client should not smoothly interpolate between?
	 */
	public boolean isDiscontinuous = false;

	public ClientboundSyncCelestialTimePacket() {
	}

	public ClientboundSyncCelestialTimePacket(double celestialTime) {
		this.celestialTime = celestialTime;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.celestialTime = buf.readDouble();
		this.celestialTimeRate = buf.readDouble();
		this.isDiscontinuous = buf.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeDouble(this.celestialTime);
		buf.writeDouble(this.celestialTimeRate);
		buf.writeBoolean(this.isDiscontinuous);
	}

}
