package net.xavil.ultraviolet.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.util.math.matrices.Vec3;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
	private void modifyDeltaMovement(FallingBlockEntity entity, net.minecraft.world.phys.Vec3 motion) {
		EntityAccessor.applyGravity(entity, Vec3.from(motion));
	}

}
