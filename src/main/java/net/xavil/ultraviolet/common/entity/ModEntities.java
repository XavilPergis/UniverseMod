package net.xavil.ultraviolet.common.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.xavil.ultraviolet.Mod;

public final class ModEntities {

	public static final EntityType<GeyserEntity> GEYSER = FabricEntityTypeBuilder
			.<GeyserEntity>create(MobCategory.MISC, GeyserEntity::new)
			.dimensions(EntityDimensions.fixed(1, 1)).build();

	public static void registerEntityType(String name, EntityType<?> entityType) {
		Registry.register(Registry.ENTITY_TYPE, Mod.namespaced(name), entityType);
	}

	public static void register() {
		registerEntityType("geyser", GEYSER);
	}

	public static void registerClient() {
		EntityRendererRegistry.register(GEYSER, NoopRenderer::new);
	}
}
