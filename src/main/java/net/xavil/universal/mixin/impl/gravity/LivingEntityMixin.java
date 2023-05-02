package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.util.Assert;
import net.xavil.util.math.matrices.Vec3;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	private float getFrictionInfluencedSpeed(float friction) {
		throw Assert.isUnreachable();
	}

	private Vec3 cachedGravity = null;

	// TODO: make elytra not work in planets with no atmospheres (maybe make them
	// gradually less effective)

	@Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
	private void cacheGravity(net.minecraft.world.phys.Vec3 vec, CallbackInfo info) {
		cachedGravity = EntityAccessor.getEntityGravity((LivingEntity) (Object) this).unwrapOr(Vec3.YN);
	}

	@ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.08))
	private double modifyVanillaGravity(double d) {
		return -this.cachedGravity.y * d;
	}

	@ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.01))
	private double modifySlowfallGravity(double d) {
		return -this.cachedGravity.y * d;
	}

	@Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 2))
	private void modifyDeltaMovement1(LivingEntity entity, double x, double y, double z) {
		applyGravityHoriz(entity, x, y, z);
	}

	@Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 3))
	private void modifyDeltaMovement2(LivingEntity entity, double x, double y, double z) {
		applyGravityHoriz(entity, x, y, z);
	}

	private void applyGravityHoriz(LivingEntity entity, double x, double y, double z) {
		entity.setDeltaMovement(x + this.cachedGravity.x, y, z + this.cachedGravity.z);
	}

	private Vec3 getGravity() {
		final var gravity = EntityAccessor.getEntityGravity((LivingEntity) (Object) this);
		return gravity.unwrapOr(Vec3.YN);
	}

	@Redirect(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFrictionInfluencedSpeed(F)F"))
	private float speedFromLowGravity(LivingEntity self, float friction) {
		return getFrictionInfluencedSpeed(friction) / Mth.clamp((float) getGravity().length(), 0.67f, 1.2f);
	}

	// TODO: cause fall damage when slamming into a wall when gravity isnt directly
	// downwards or smth

	@ModifyVariable(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private float modifyFallDamage(float damageMultiplier) {
		return (float) (damageMultiplier * getGravity().length());
	}

}
