package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.networking.ModPacket;

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
		this.celestialTime = readDouble(buf);
		this.celestialTimeRate = readDouble(buf);
		this.isDiscontinuous = readBoolean(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeDouble(buf, this.celestialTime);
		writeDouble(buf, this.celestialTimeRate);
		writeBoolean(buf, this.isDiscontinuous);
	}

}
