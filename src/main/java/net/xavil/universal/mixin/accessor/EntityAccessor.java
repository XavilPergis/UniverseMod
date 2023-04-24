package net.xavil.universal.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.Option;
import net.xavil.util.math.Vec3;

public interface EntityAccessor {

	static Universe getUniverse(Entity entity) {
		return ((LevelAccessor) entity.level).universal_getUniverse();
	}

	static Location getLocation(Entity entity) {
		return ((LevelAccessor) entity.level).universal_getLocation();
	}

	static Option<Vec3> getEntityGravity(Entity entity) {
		final var location = EntityAccessor.getLocation(entity);
		final var universe = EntityAccessor.getUniverse(entity);
		if (universe != null && location != null) {
			if (location instanceof Location.World loc) {
				final var node = universe.getSystemNode(loc.id).unwrapOrNull();
				if (node instanceof PlanetaryCelestialNode planetNode) {
					return Option.some(Vec3.from(0, -planetNode.surfaceGravityEarthRelative(), 0));
				}
			} else if (location instanceof Location.Station loc) {
				final var pos = Vec3.fromMinecraft(entity.position());
				return universe.getStation(loc.id).map(st -> st.getGavityAt(pos));
			}
		}
		return Option.none();
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
