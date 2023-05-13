package net.xavil.ultraviolet;

import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.common.universe.universe.AuthoredSystemResource;
import net.xavil.util.collections.Blackboard;

public class ModResources {

	public static final Blackboard<ResourceLocation> RELOADERS = new Blackboard<>();

	public record Key<T>(ResourceLocation location) {}
	
	public interface RegisterServerResourcesCallback {
		void register();
	}

	public static final Key<AuthoredSystemResource> AUTHORED_SYSTEM = new Key<>(Mod.namespaced("authored_system"));

	public static <T> T get(Blackboard.Key<ResourceLocation, T> key) {
		return RELOADERS.get(key).unwrapOrNull();
	}

}
