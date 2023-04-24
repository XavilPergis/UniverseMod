package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.util.Assert;
import net.xavil.util.math.Vec3;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	private float getFrictionInfluencedSpeed(float friction) {
		throw Assert.isUnreachable();
	}

	// TODO: make elytra not work in planets with no atmospheres (maybe make them
	// gradually less effective)

	@Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 5))
	private void modifyDeltaMovement(LivingEntity entity, net.minecraft.world.phys.Vec3 motion) {
		EntityAccessor.applyGravity(entity, Vec3.fromMinecraft(motion));
	}

	private Vec3 getGravity() {
		final var gravity = EntityAccessor.getEntityGravity((LivingEntity) (Object) this);
		return gravity.unwrapOr(Vec3.YN);
	}

	@Redirect(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFrictionInfluencedSpeed(F)F"))
	private float speedFromLowGravity(LivingEntity self, float friction) {
		return getFrictionInfluencedSpeed(friction) / Mth.clamp((float) getGravity().length(), 0.67f, 1.2f);
	}

	// TODO: cause fall damage when slamming into a wall when gravity isnt directly downwards or smth

	@ModifyVariable(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private float modifyFallDamage(float damageMultiplier) {
		return (float) (damageMultiplier * getGravity().length());
	}

}
