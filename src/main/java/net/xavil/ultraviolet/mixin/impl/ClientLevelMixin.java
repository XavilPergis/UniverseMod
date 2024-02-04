package net.xavil.ultraviolet.mixin.impl;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.config.ConfigProvider;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

	protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey,
			Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
		super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l);
		throw new IllegalStateException("unreachable: mixin consturctor");
	}

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void setConfigProvider(CallbackInfo info) {
		LevelAccessor.setConfigProvider(this, new ConfigProvider() {
			@Override
			public <T> T get(ConfigKey<T> key) {
				return ClientConfig.get(key);
			}
		});
	}

}
