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
import net.xavil.universegen.system.PlanetaryCelestialNode;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	abstract float getFrictionInfluencedSpeed(float friction);

	// TODO: make elytra not work in planets with no atmospheres (maybe make them
	// gradually less effective)

	@ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.08))
	private double modifyGravity(double value) {
		final var self = (LivingEntity) (Object) this;
		final var nodeId = EntityAccessor.getSystemNodeId(self);
		final var universe = EntityAccessor.getUniverse(self);
		if (universe != null && nodeId != null) {
			final var node = universe.getSystemNode(nodeId);
			if (node instanceof PlanetaryCelestialNode planetNode) {
				return planetNode.surfaceGravityEarthRelative() * value;
			}
		}
		return value;
	}

	@Redirect(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFrictionInfluencedSpeed(F)F"))
	private float speedFromLowGravity(LivingEntity self, float friction) {
		final var n = getFrictionInfluencedSpeed(friction);
		final var nodeId = EntityAccessor.getSystemNodeId(self);
		final var universe = EntityAccessor.getUniverse(self);
		if (universe != null && nodeId != null) {
			final var node = universe.getSystemNode(nodeId);
			if (node instanceof PlanetaryCelestialNode planetNode) {
				return n / Mth.clamp((float) planetNode.surfaceGravityEarthRelative(), 0.67f, 1.2f);
			}
		}
		return n;
	}

	@ModifyVariable(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private float modifyFallDamage(float damageMultiplier) {
		final var self = (LivingEntity) (Object) this;
		final var nodeId = EntityAccessor.getSystemNodeId(self);
		final var universe = EntityAccessor.getUniverse(self);
		if (universe != null && nodeId != null) {
			final var node = universe.getSystemNode(nodeId);
			if (node instanceof PlanetaryCelestialNode planetNode) {
				return damageMultiplier * (float) planetNode.surfaceGravityEarthRelative();
			}
		}
		return damageMultiplier;
	}

}
