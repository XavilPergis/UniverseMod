package net.xavil.universal.mixin.accessor;

import java.util.OptionalDouble;

import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.math.Vec3;

public interface EntityAccessor {

	static Universe getUniverse(Entity entity) {
		return ((LevelAccessor) entity.level).universal_getUniverse();
	}

	static Location getLocation(Entity entity) {
		return ((LevelAccessor) entity.level).universal_getLocation();
	}

	static OptionalDouble getEntityGravity(Entity entity) {
		final var location = EntityAccessor.getLocation(entity);
		final var universe = EntityAccessor.getUniverse(entity);
		if (universe != null && location != null) {
			if (location instanceof Location.World world) {
				final var node = universe.getSystemNode(world.id).unwrapOrNull();
				if (node instanceof PlanetaryCelestialNode planetNode) {
					return OptionalDouble.of(planetNode.surfaceGravityEarthRelative());
				}
			} else if (location instanceof Location.Station station) {
				final var pos = Vec3.fromMinecraft(entity.position());
				final var opt = universe.getStation(station.id).map(st -> st.getGavityStrength(pos));
				return opt.isSome() ? OptionalDouble.of(opt.unwrap()) : OptionalDouble.empty();
			}
		}
		return OptionalDouble.empty();
	}

}
