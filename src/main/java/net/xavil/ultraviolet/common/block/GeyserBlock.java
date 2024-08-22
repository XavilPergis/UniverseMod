package net.xavil.ultraviolet.common.block;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.xavil.hawklib.Rng;
import net.xavil.ultraviolet.common.entity.GeyserEntity;
import net.xavil.ultraviolet.common.entity.ModEntities;

public final class GeyserBlock extends Block {

	public static final int MAX_PRESSURE = 7;
	public static final IntegerProperty PRESSURE = IntegerProperty.create("pressure", 0, MAX_PRESSURE);
	public static final BooleanProperty ERUPTING = BooleanProperty.create("erupting");
	public final float increaseChance = 0.25f;
	public final float[] decreaseChances = { 0.1f, 0.2f, 0.4f, 0.6f, 0.7f, 0.8f, 0.8f, 1.0f };

	public GeyserBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(PRESSURE, 0).setValue(ERUPTING, false));
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
		// TODO Auto-generated method stub
		super.animateTick(state, level, pos, random);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	private void spawnGeyserIfNeeded(BlockState state, Level level, BlockPos pos) {
		if (!state.getValue(ERUPTING))
			return;

		final var aboveBlock = new AABB(pos.above());
		final var geysers = level.getEntitiesOfClass(GeyserEntity.class, aboveBlock);

		for (final var geyser : geysers) {
			if (aboveBlock.contains(geyser.position()))
				return;
		}

		final var entity = new GeyserEntity(ModEntities.GEYSER, level, pos);
		level.addFreshEntity(entity);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
			boolean isMoving) {
		spawnGeyserIfNeeded(state, level, pos);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		spawnGeyserIfNeeded(state, level, pos);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
		final var rng = Rng.wrap(random);

		// TODO: temporary block
		// keeps the eruption status and pressure from changing if below
		if (level.getBlockState(pos.below()).getBlock() == Blocks.MAGMA_BLOCK)
			return;

		final var currentPressure = state.getValue(PRESSURE).intValue();
		final var isAlreadyErupting = state.getValue(ERUPTING).booleanValue();

		boolean toggleEruption = false;
		toggleEruption |= currentPressure == 0 && state.getValue(ERUPTING);
		toggleEruption |= currentPressure == 7 && !state.getValue(ERUPTING);

		if (toggleEruption) {
			final var newState = state.setValue(ERUPTING, !isAlreadyErupting);
			level.setBlock(pos, newState, UPDATE_CLIENTS | UPDATE_NEIGHBORS);
			if (!isAlreadyErupting) {
				spawnGeyserIfNeeded(state, level, pos);
			}
		} else {
			final var changeAmount = isAlreadyErupting ? -1 : 1;
			final var changeChance = isAlreadyErupting ? this.decreaseChances[currentPressure] : this.increaseChance;
			if (rng.chance(changeChance)) {
				final var newState = state.setValue(PRESSURE, currentPressure + changeAmount);
				level.setBlock(pos, newState, UPDATE_CLIENTS | UPDATE_NEIGHBORS);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(PRESSURE).add(ERUPTING);
	}

}
