package net.xavil.universal.common;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.phys.BlockHitResult;
import net.xavil.universal.Mod;
import net.xavil.universal.common.dimension.DimensionCreationProperties;
import net.xavil.universal.common.dimension.DynamicDimensionManager;

public class TestBlock extends Block {

	public TestBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player,
			InteractionHand interactionHand, BlockHitResult blockHitResult) {

		if (player.isCrouching() && level instanceof ServerLevel serverLevel) {
			final var manager = DynamicDimensionManager.get(serverLevel.getServer());
			final var newLevel = manager.getOrCreateLevel(DynamicDimensionManager.getKey(Mod.namespaced("test")), () -> {
				var registryAccess = serverLevel.getServer().registryAccess();
				var type = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getOrCreateHolder(DimensionType.NETHER_LOCATION);
				var generator = WorldGenSettings.makeDefaultOverworld(registryAccess, new Random().nextLong());
				return DimensionCreationProperties.basic(new LevelStem(type, generator));
			});
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.teleportTo(newLevel, 0, 100, 0, 0, 0);
			}
		}

		return InteractionResult.SUCCESS;
	}
	
}
