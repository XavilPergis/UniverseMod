package net.xavil.ultraviolet;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer;
import net.xavil.ultraviolet.common.components.SystemNodeIdComponent;

public class ModComponents
		implements ScoreboardComponentInitializer, WorldComponentInitializer, EntityComponentInitializer {

	public static final ComponentKey<SystemNodeIdComponent> SYSTEM_NODE_ID = ComponentRegistry
			.getOrCreate(Mod.namespaced("system_node_id"), SystemNodeIdComponent.class);

	@Override
	public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
		// registry.registerScoreboardComponent(SYSTEM_NODE_ID_KEY, (scoreboard, server) -> new SystemNodeIdComponent());
	}

	@Override
	public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
		registry.register(SYSTEM_NODE_ID, level -> new SystemNodeIdComponent());
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
	}

}
