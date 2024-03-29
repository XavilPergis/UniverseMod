package net.xavil.ultraviolet.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.hawklib.math.matrices.Vec3;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setDeltaMovement(DDD)V", ordinal = 0))
	private void modifyDeltaMovement(ThrowableProjectile entity, double x, double y, double z) {
		EntityAccessor.applyGravity(entity, new Vec3(x, y, z));
	}

}
