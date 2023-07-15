package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public interface EntityAccessor {

	static Universe getUniverse(Entity entity) {
		return ((LevelAccessor) entity.level).ultraviolet_getUniverse();
	}

	static Location getLocation(Entity entity) {
		return ((LevelAccessor) entity.level).ultraviolet_getLocation();
	}

	static int getStation(Entity entity) {
		return ((LevelAccessor) entity.level).ultraviolet_getLocation() instanceof Location.Station loc ? loc.id : -1;
	}

	static Maybe<Vec3> getGravityAt(Level level, Vec3Access pos) {
		final var location = ((LevelAccessor) level).ultraviolet_getLocation();
		final var universe = ((LevelAccessor) level).ultraviolet_getUniverse();
		if (universe != null && location != null) {
			if (location instanceof Location.World loc) {
				final var node = universe.getSystemNode(loc.id).unwrapOrNull();
				if (node instanceof PlanetaryCelestialNode planetNode) {
					return Maybe.some(Vec3.from(0, -planetNode.surfaceGravityEarthRelative(), 0));
				}
			} else if (location instanceof Location.Station loc) {
				return universe.getStation(loc.id).map(st -> st.getGavityAt(pos));
			}
		}
		return Maybe.none();
	}

	static Maybe<Vec3> getEntityGravity(Entity entity) {
		final var location = EntityAccessor.getLocation(entity);
		final var universe = EntityAccessor.getUniverse(entity);
		if (universe != null && location != null) {
			if (location instanceof Location.World loc) {
				final var node = universe.getSystemNode(loc.id).unwrapOrNull();
				if (node instanceof PlanetaryCelestialNode planetNode) {
					return Maybe.some(Vec3.from(0, -planetNode.surfaceGravityEarthRelative(), 0));
				}
			} else if (location instanceof Location.Station loc) {
				final var pos = Vec3.from(entity.position());
				return universe.getStation(loc.id).map(st -> st.getGavityAt(pos));
			}
		}
		return Maybe.none();
	}

	static void applyGravity(Entity entity, Vec3 vanillaAcceleration) {
		final var gravity = EntityAccessor.getEntityGravity(entity);
		if (gravity.isSome()) {
			final var gravityStrength = entity.getDeltaMovement().y - vanillaAcceleration.y;
			final var scaledGravity = gravity.unwrap().mul(gravityStrength);
			entity.setDeltaMovement(entity.getDeltaMovement().add(scaledGravity.asMinecraft()));
		} else {
			entity.setDeltaMovement(vanillaAcceleration.asMinecraft());
		}
	}

}
