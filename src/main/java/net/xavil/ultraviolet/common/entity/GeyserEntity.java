package net.xavil.ultraviolet.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.common.block.GeyserBlock;
import net.xavil.ultraviolet.common.particle.ModParticles;

public final class GeyserEntity extends Entity {

	public static final BlockPos INVALID_POS = BlockPos.ZERO.atY(Integer.MIN_VALUE);
	public static final Interval HEIGHT_BOUNDS = new Interval(2, 100);

	private static final EntityDataAccessor<BlockPos> DATA_TRACKED_POS = SynchedEntityData
			.defineId(GeyserEntity.class, EntityDataSerializers.BLOCK_POS);

	public GeyserEntity(EntityType<?> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true;
		this.noCulling = true;
	}

	public GeyserEntity(EntityType<?> entityType, Level level, BlockPos trackedGeyser) {
		super(entityType, level);
		this.noPhysics = true;
		this.noCulling = true;
		this.getEntityData().set(DATA_TRACKED_POS, trackedGeyser);
		this.setPos(Vec3.atBottomCenterOf(trackedGeyser.above()));
	}

	private static float getPushForce(float pressure) {
		return (float) Mth.lerp(Math.pow(pressure, 2.0), 0.01, 0.2);
	}

	@Override
	public void tick() {
		final var trackedLocation = this.getEntityData().get(DATA_TRACKED_POS);
		final var state = this.level.getBlockState(trackedLocation);
		if (!(state.getBlock() instanceof GeyserBlock) || !state.getValue(GeyserBlock.ERUPTING)) {
			this.remove(RemovalReason.DISCARDED);
			return;
		}

		super.tick();

		final var pressureT = state.getValue(GeyserBlock.PRESSURE) / (float) GeyserBlock.MAX_PRESSURE;
		final var height = HEIGHT_BOUNDS.lerp(Math.pow(pressureT, 2.0));
		// TODO: smoothing?
		final var aabb = new AABB(trackedLocation.above()).inflate(1, 0, 1).expandTowards(0, height, 0);
		this.setBoundingBox(aabb);

		final var pushForce = getPushForce(pressureT);

		// TODO: searching can be pretty expensive, perhaps we do this with a lower
		// frequency, caching affected entities between queries.
		final var affectedEntities = MutableList.proxy(this.level.getEntitiesOfClass(Entity.class, aabb));
		for (final var entity : affectedEntities.iterable()) {
			final var t = Mth.clamp(Mth.inverseLerp(entity.getY(), aabb.minY, aabb.maxY), 0, 1);
			final var upwardsPush = pushForce * (1 - t);
			if (entity instanceof Player player && player.getAbilities().flying)
				continue;
			entity.push(0, upwardsPush, 0);
		}

		spawnParticles(pressureT, pushForce, affectedEntities);
	}

	private void spawnParticles(double pressureT, double pushForce, ImmutableList<Entity> affectedEntities) {
		// particles!!
		if (!this.level.isClientSide)
			return;

		// TODO: spawn particles when pushing entities, like the fluid is being stopped
		// in its path by the entity.

		final var rng = Rng.wrap(this.random);
		final var particleCount = Mth.floor(Mth.lerp(pressureT, 1, 10));

		for (int i = 0; i < particleCount; ++i) {

			// random dir, mostly facing upwards
			final double dx = rng.uniformDouble(Interval.BIPOLAR),
					dy = rng.uniformDouble(Interval.UNIPOLAR),
					dz = rng.uniformDouble(Interval.BIPOLAR);

			final double vx = rng.normalDouble(0, 0.1), vz = rng.normalDouble(0, 0.1);
			final var velocity = new Vec3(vx, 1, vz).normalize().scale(rng.normalDouble(0.9, 1) * 4 * pushForce);

			this.level.addAlwaysVisibleParticle(ModParticles.GEYSER,
					this.position().x + dx, this.position().y + dy, this.position().z + dz,
					velocity.x, velocity.y, velocity.z);
		}

	}

	@Override
	public boolean isAlwaysTicking() {
		// fuck u, render me
		return true;
	}

	@Override
	protected void defineSynchedData() {
		this.getEntityData().define(DATA_TRACKED_POS, INVALID_POS);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		final var x = nbt.getInt("geyser_x");
		final var y = nbt.getInt("geyser_y");
		final var z = nbt.getInt("geyser_z");
		this.getEntityData().set(DATA_TRACKED_POS, new BlockPos(x, y, z));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		final var trackedLocation = this.getEntityData().get(DATA_TRACKED_POS);
		nbt.putInt("geyser_x", trackedLocation.getX());
		nbt.putInt("geyser_y", trackedLocation.getY());
		nbt.putInt("geyser_z", trackedLocation.getZ());
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

}
