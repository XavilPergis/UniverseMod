package net.xavil.ultraviolet.networking.s2c;

import javax.annotation.Nullable;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundDebugPacket extends ModPacket<ClientGamePacketListener> {

    public String action;
    @Nullable
    public Tag payload;

    public ClientboundDebugPacket() {
    }

    public ClientboundDebugPacket(String action, @Nullable Tag payload) {
        this.action = action;
        this.payload = payload;
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.action = read(buf, NetworkSerializers.UTF);
        this.payload = read(buf, NetworkSerializers.NBT);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        write(buf, this.action, NetworkSerializers.UTF);
        write(buf, this.payload, NetworkSerializers.NBT);
    }

}
