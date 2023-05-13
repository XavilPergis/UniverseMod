package net.xavil.ultraviolet.mixin.impl.gravity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.util.math.matrices.Vec3;

@Mixin(Particle.class)
public abstract class ParticleMixin {

	@Shadow
	@Final
	protected ClientLevel level;

	@Shadow
	protected float gravity;

	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;
	@Shadow
	protected double xd;
	@Shadow
	protected double yd;
	@Shadow
	protected double zd;

	@ModifyConstant(method = "tick()V", constant = @Constant(doubleValue = 0.04))
	private double cancelGravity(double gravity) {
		return 0;
	}

	@Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;move(DDD)V"))
	private void applyCustomGravity(CallbackInfo info) {
		final var pos = Vec3.from(x, y, z);
		final var gravity = EntityAccessor.getGravityAt(this.level, pos).unwrapOr(Vec3.YN);
		this.xd += 0.04 * ((double) this.gravity) * gravity.x;
		this.yd += 0.04 * ((double) this.gravity) * gravity.y;
		this.zd += 0.04 * ((double) this.gravity) * gravity.z;
	}

}
