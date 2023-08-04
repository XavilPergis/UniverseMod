package net.xavil.ultraviolet.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.networking.s2c.ClientboundOpenStarmapPacket;

public class StarmapItem extends Item {

	public StarmapItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	private Maybe<SystemNodeId> getSystemToOpen(ServerLevel level) {
		final var location = LevelAccessor.getWorldType(level);
		final var universe = LevelAccessor.getUniverse(level);
		if (location instanceof WorldType.SystemNode world) {
			return Maybe.some(world.id);
		} else if (location instanceof WorldType.Station station) {
			return universe.getStation(station.id).flatMap(s -> {
				if (s.getLocation() instanceof StationLocation.OrbitingCelestialBody orbiting) {
					return Maybe.some(orbiting.id);
				}
				return Maybe.none();
			});
		}
		return Maybe.none();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
			final var universe = LevelAccessor.getUniverse(level);
			final var toOpen = getSystemToOpen(serverLevel).unwrapOr(universe.getStartingSystemGenerator().getStartingSystemId());
			serverPlayer.connection.send(new ClientboundOpenStarmapPacket(toOpen));
		}
		return InteractionResultHolder.success(player.getItemInHand(interactionHand));
	}

}
