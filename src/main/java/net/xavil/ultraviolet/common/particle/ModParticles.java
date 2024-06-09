package net.xavil.ultraviolet.common.particle;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.particle.GeyserParticle;

public final class ModParticles {

	public static final SimpleParticleType GEYSER = FabricParticleTypes.simple(true);

	public static void register() {
		Registry.register(Registry.PARTICLE_TYPE, Mod.namespaced("geyser"), GEYSER);
	}

	public static void registerClient() {
		ParticleFactoryRegistry.getInstance().register(GEYSER, GeyserParticle.Provider::new);
	}

}
