package net.xavil.ultraviolet.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.util.math.matrices.Vec3;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setDeltaMovement(DDD)V", ordinal = 0))
	private void modifyDeltaMovement(AbstractArrow entity, double x, double y, double z) {
		EntityAccessor.applyGravity(entity, Vec3.from(x, y, z));
	}

}
