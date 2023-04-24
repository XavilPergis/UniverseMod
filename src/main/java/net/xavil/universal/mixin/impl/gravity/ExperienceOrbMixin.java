package net.xavil.universal.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.ExperienceOrb;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.util.math.Vec3;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
	private void modifyDeltaMovement(ExperienceOrb entity, net.minecraft.world.phys.Vec3 motion) {
		EntityAccessor.applyGravity(entity, Vec3.fromMinecraft(motion));
	}

}
