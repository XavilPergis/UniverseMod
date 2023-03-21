package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.xavil.universal.mixin.accessor.EntityAccessor;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
	
	@ModifyConstant(method = "tick()V", constant = @Constant(doubleValue = -0.04))
	private double modifyGravity(double value) {
		final var self = (FallingBlockEntity) (Object) this;
		final var gravity = EntityAccessor.getEntityGravity(self);
		return value * gravity.orElse(1.0);
	}

}
