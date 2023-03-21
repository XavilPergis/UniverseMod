package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.xavil.universal.mixin.accessor.EntityAccessor;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {

	@Shadow
	protected abstract float getGravity();
	
	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;getGravity()F"))
	private float modifyGravity(ThrowableProjectile self) {
		final var gravity = EntityAccessor.getEntityGravity(self);
		return getGravity() * (float) gravity.orElse(1.0);
	}

}
