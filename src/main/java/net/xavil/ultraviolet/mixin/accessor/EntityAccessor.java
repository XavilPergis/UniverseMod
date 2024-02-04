package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public interface EntityAccessor {

	static Universe getUniverse(Entity entity) {
		return ((LevelAccessor) entity.level).ultraviolet_getUniverse();
	}

	static WorldType getWorldType(Entity entity) {
		return LevelAccessor.getWorldType(entity.level);
	}

	static int getStation(Entity entity) {
		return LevelAccessor.getWorldType(entity.level) instanceof WorldType.Station type ? type.id : -1;
	}

	static Maybe<Vec3> getGravityAt(Level level, Vec3Access pos) {
		final var config = LevelAccessor.getConfigProvider(level);

		if (config.get(ConfigKey.USE_FIXED_GRAVITY)) {
			final var gravity = config.get(ConfigKey.FIXED_GRAVITY);
			return Maybe.some(new Vec3(0, -gravity, 0));
		}

		final var type = LevelAccessor.getWorldType(level);
		final var universe = LevelAccessor.getUniverse(level);
		if (universe != null && type != null) {
			if (type instanceof WorldType.SystemNode ty) {
				final var node = universe.getSystemNode(ty.id).unwrapOrNull();
				if (node instanceof PlanetaryCelestialNode planetNode) {
					var gravity = planetNode.surfaceGravityEarthRelative();
					final var minGravity = config.get(ConfigKey.MIN_GRAVITY);
					final var maxGravity = config.get(ConfigKey.MAX_GRAVITY);
					gravity = Mth.clamp(gravity, minGravity, maxGravity);
					return Maybe.some(new Vec3(0, -gravity, 0));
				}
			} else if (type instanceof WorldType.Station ty) {
				return universe.getStation(ty.id).map(st -> st.getGavityAt(pos));
			}
		}
		return Maybe.none();
	}

	static Maybe<Vec3> getEntityGravity(Entity entity) {
		return getGravityAt(entity.getLevel(), Vec3.from(entity.position()));
	}

	static void applyGravity(Entity entity, Vec3 vanillaAcceleration) {
		final var gravity = EntityAccessor.getEntityGravity(entity);
		if (gravity.isSome()) {
			final var gravityStrength = entity.getDeltaMovement().y - vanillaAcceleration.y;
			final var scaledGravity = gravity.unwrap().mul(gravityStrength);
			entity.setDeltaMovement(entity.getDeltaMovement().add(Vec3.toMinecraft(scaledGravity)));
		} else {
			entity.setDeltaMovement(Vec3.toMinecraft(vanillaAcceleration));
		}
	}

}
