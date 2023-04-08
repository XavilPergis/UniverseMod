package net.xavil.universal.mixin.accessor;

import java.util.OptionalDouble;

import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universegen.system.PlanetaryCelestialNode;

public interface EntityAccessor {
	
	Universe universal_getUniverse();
	void universal_setUniverse(Universe universe);
	SystemNodeId universal_getSystemNodeId();
	void universal_setSystemNodeId(SystemNodeId id);

	static Universe getUniverse(Entity entity) {
		return ((EntityAccessor) entity).universal_getUniverse();
	}

	static void setUniverse(Entity entity, Universe universe) {
		((EntityAccessor) entity).universal_setUniverse(universe);
	}

	static SystemNodeId getSystemNodeId(Entity entity) {
		return ((EntityAccessor) entity).universal_getSystemNodeId();
	}

	static void setSystemNodeId(Entity entity, SystemNodeId id) {
		((EntityAccessor) entity).universal_setSystemNodeId(id);
	}

	static OptionalDouble getEntityGravity(Entity entity) {
		final var nodeId = EntityAccessor.getSystemNodeId(entity);
		final var universe = EntityAccessor.getUniverse(entity);
		if (universe != null && nodeId != null) {
			final var node = universe.getSystemNode(nodeId).unwrap();
			if (node instanceof PlanetaryCelestialNode planetNode) {
				return OptionalDouble.of(planetNode.surfaceGravityEarthRelative());
			}
		}
		return OptionalDouble.empty();
	}

}
