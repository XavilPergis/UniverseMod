package net.xavil.universal;

import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.common.universe.universe.AuthoredSystemResource;

public class ModResources {

	public record Key<T>(ResourceLocation location) {}
	
	public interface RegisterServerResourcesCallback {
		void register();
	}

	public static final Key<AuthoredSystemResource> AUTHORED_SYSTEM = new Key<>(Mod.namespaced("authored_system"));

	// public static <T> T get(Key<T> key) {}

}
