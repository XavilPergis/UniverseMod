package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.item.ItemEntity;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.util.math.Vec3;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
	private void modifyDeltaMovement(ItemEntity entity, net.minecraft.world.phys.Vec3 motion) {
		EntityAccessor.applyGravity(entity, Vec3.from(motion));
	}

}
