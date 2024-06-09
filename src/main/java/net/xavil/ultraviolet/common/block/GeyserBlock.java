package net.xavil.ultraviolet.common.block;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
		final var rng = Rng.wrap(random);

		final var currentPressure = state.getValue(PRESSURE).intValue();

		if (currentPressure == 0 && state.getValue(ERUPTING)) {
			level.setBlock(pos, state.setValue(ERUPTING, false), 2);
		} else if (currentPressure == 7 && !state.getValue(ERUPTING)) {
			level.setBlock(pos, state.setValue(ERUPTING, true), 2);
			final var entity = new GeyserEntity(ModEntities.GEYSER, level, pos);
			level.addFreshEntity(entity);
		} else if (state.getValue(ERUPTING)) {
			if (rng.chance(this.decreaseChances[currentPressure])) {
				level.setBlock(pos, state.setValue(PRESSURE, currentPressure - 1), 2);
			}
		} else {
			if (rng.chance(this.increaseChance)) {
				level.setBlock(pos, state.setValue(PRESSURE, currentPressure + 1), 2);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(PRESSURE).add(ERUPTING);
	}

}
