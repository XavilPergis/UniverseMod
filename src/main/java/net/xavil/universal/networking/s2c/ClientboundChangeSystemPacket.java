package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundChangeSystemPacket extends ModPacket {

	public static final ResourceLocation CHANNEL = Mod.namespaced("change_system");

	public SystemNodeId id;

	public static ClientboundChangeSystemPacket empty() {
		return new ClientboundChangeSystemPacket();
	};

	@Override
	public ResourceLocation getChannelName() {
		return CHANNEL;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.id = readSystemNodeId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeSystemNodeId(buf, this.id);
	}

}
