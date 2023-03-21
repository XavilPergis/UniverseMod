package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.xavil.universal.mixin.accessor.EntityAccessor;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	
	@ModifyConstant(method = "tick()V", constant = @Constant(floatValue = 0.05f))
	private float modifyGravity(float value) {
		final var self = (AbstractArrow) (Object) this;
		final var gravity = EntityAccessor.getEntityGravity(self);
		return value * (float) gravity.orElse(1.0);
	}

}
