package net.xavil.universal.networking;

import net.minecraft.network.FriendlyByteBuf;

public abstract class ModPacket {

	public abstract void read(FriendlyByteBuf buf);

	public abstract void write(FriendlyByteBuf buf);

}
