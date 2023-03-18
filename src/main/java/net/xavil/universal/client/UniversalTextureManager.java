package net.xavil.universal.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class UniversalTextureManager
		extends SimplePreparableReloadListener<UniversalTextureManager.ManagerPreparations> {
	public static final UniversalTextureManager INSTANCE = new UniversalTextureManager();

	public static class ManagerPreparations {
		public final Map<ResourceLocation, TextureAtlas.Preparations> atlasPreparations = new HashMap<>();
	}

	private static final class AtlasInfo {
		public final TextureAtlas atlas;
		public final Set<ResourceLocation> spriteLocations;

		public AtlasInfo(TextureAtlas atlas, Set<ResourceLocation> spriteLocations) {
			this.atlas = atlas;
			this.spriteLocations = spriteLocations;
		}
	}

	private final Map<ResourceLocation, AtlasInfo> textureAtlases = new HashMap<>();

	public static final class SpriteRegistrationContext {
		public final AtlasRegistrationContext atlasContext;
		public final ResourceLocation atlasLocation;
		private final Set<ResourceLocation> spriteLocations = new HashSet<>();

		private SpriteRegistrationContext(AtlasRegistrationContext atlasContext, ResourceLocation atlasLocation) {
			this.atlasContext = atlasContext;
			this.atlasLocation = atlasLocation;
		}

		public void registerSprite(ResourceLocation location) {
			this.spriteLocations.add(location);
		}

		public void registerSprite(String location) {
			registerSprite(new ResourceLocation(location));
		}
	}

	public static final class AtlasRegistrationContext {
		private final Map<ResourceLocation, SpriteRegistrationContext> spriteContexts = new HashMap<>();

		public SpriteRegistrationContext registerAtlas(ResourceLocation location) {
			if (!this.spriteContexts.containsKey(location)) {
				this.spriteContexts.put(location, new SpriteRegistrationContext(this, location));
			}
			return this.spriteContexts.get(location);
		}

		public SpriteRegistrationContext registerAtlas(String location) {
			return registerAtlas(new ResourceLocation(location));
		}
	}

	public interface RegisterTextureAtlasesCallback {
		void register(AtlasRegistrationContext consumer);
	}

	public static final Event<RegisterTextureAtlasesCallback> REGISTER_ATLASES = EventFactory
			.createArrayBacked(RegisterTextureAtlasesCallback.class, callbacks -> consumer -> {
				for (var callback : callbacks)
					callback.register(consumer);
			});

	public void registerAtlases() {
		this.textureAtlases.values().forEach(info -> info.atlas.close());
		this.textureAtlases.clear();

		final var ctx = new AtlasRegistrationContext();
		REGISTER_ATLASES.invoker().register(ctx);
		for (var entry : ctx.spriteContexts.entrySet()) {
			final var atlas = new TextureAtlas(entry.getKey());
			final var sprites = Set.copyOf(entry.getValue().spriteLocations);
			this.textureAtlases.put(entry.getKey(), new AtlasInfo(atlas, sprites));
		}
	}

	@Override
	protected ManagerPreparations prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		profiler.startTick();
		profiler.push("stitching");
		final var preparations = new ManagerPreparations();
		for (final var entry : this.textureAtlases.entrySet()) {
			profiler.push(entry.getKey().toString());
			final var info = entry.getValue();
			final var spriteNames = info.spriteLocations.stream();
			final var atlasPreparations = info.atlas.prepareToStitch(resourceManager, spriteNames, profiler, 0);
			preparations.atlasPreparations.put(entry.getKey(), atlasPreparations);
			profiler.pop();
		}
		profiler.pop();
		profiler.endTick();
		return preparations;
	}

	@Override
	protected void apply(ManagerPreparations preparations, ResourceManager resourceManager, ProfilerFiller profiler) {
		profiler.startTick();
		profiler.push("register");
		UniversalTextureManager.INSTANCE.registerAtlases();
		profiler.popPush("upload");
		for (final var entry : preparations.atlasPreparations.entrySet()) {
			profiler.push(entry.getKey().toString());
			this.textureAtlases.get(entry.getKey()).atlas.reload(entry.getValue());
			profiler.pop();
		}
		profiler.pop();
		profiler.endTick();
	}

	public @Nullable TextureAtlas getAtlas(ResourceLocation atlasLocation) {
		var info = this.textureAtlases.get(atlasLocation);
		return info == null ? null : info.atlas;
	}

}
