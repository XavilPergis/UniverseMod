package net.xavil.universal.common.item;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;

public class StarmapItem extends Item {

	public StarmapItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		if (player instanceof ServerPlayer serverPlayer) {
			var universe = MinecraftServerAccessor.getUniverse(serverPlayer.server);
			var packet = new ClientboundOpenStarmapPacket(universe.getStartingSystemGenerator().getStartingSystemId());
			var buf = PacketByteBufs.create();
			packet.write(buf);
			ServerPlayNetworking.send(serverPlayer, ModNetworking.CLIENTBOUND_OPEN_STARMAP, buf);
		}
		return InteractionResultHolder.success(player.getItemInHand(interactionHand));
	}
	
}
