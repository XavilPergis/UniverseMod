package net.xavil.ultraviolet.networking;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;

public interface NetworkSerializer<T> {
	
	void write(FriendlyByteBuf buf, T value);

	@Nullable
	T read(FriendlyByteBuf buf);

}
