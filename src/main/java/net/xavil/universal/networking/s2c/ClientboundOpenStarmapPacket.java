package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundOpenStarmapPacket extends ModPacket {

	public static final ResourceLocation CHANNEL = Mod.namespaced("open_starmap");

	public SystemNodeId toOpen;

	public static ClientboundOpenStarmapPacket empty() {
		return new ClientboundOpenStarmapPacket();
	};

	private ClientboundOpenStarmapPacket() {
	}

	@Override
	public ResourceLocation getChannelName() {
		return CHANNEL;
	}

	public ClientboundOpenStarmapPacket(SystemNodeId toOpen) {
		this.toOpen = toOpen;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.toOpen = readSystemNodeId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeSystemNodeId(buf, this.toOpen);
	}

}
