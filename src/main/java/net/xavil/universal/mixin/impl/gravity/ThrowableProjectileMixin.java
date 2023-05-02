package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.util.math.matrices.Vec3;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setDeltaMovement(DDD)V", ordinal = 0))
	private void modifyDeltaMovement(ThrowableProjectile entity, double x, double y, double z) {
		EntityAccessor.applyGravity(entity, Vec3.from(x, y, z));
	}

}
