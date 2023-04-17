package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xavil.universal.mixin.accessor.EntityAccessor;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	abstract float getFrictionInfluencedSpeed(float friction);

	// TODO: make elytra not work in planets with no atmospheres (maybe make them
	// gradually less effective)

	@ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.08))
	private double modifyGravity(double value) {
		return value * getGravity();
	}

	private double getGravity() {
		final var gravity = EntityAccessor.getEntityGravity((LivingEntity) (Object) this);
		return gravity.orElse(1.0);
	}

	@Redirect(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFrictionInfluencedSpeed(F)F"))
	private float speedFromLowGravity(LivingEntity self, float friction) {
		return getFrictionInfluencedSpeed(friction) / Mth.clamp((float) getGravity(), 0.67f, 1.2f);
	}

	@ModifyVariable(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private float modifyFallDamage(float damageMultiplier) {
		return (float) (damageMultiplier * getGravity());
	}

}
