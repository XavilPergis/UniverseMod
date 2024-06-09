package net.xavil.ultraviolet.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class GeyserParticle extends TextureSheetParticle {

	protected GeyserParticle(ClientLevel clientLevel, SpriteSet spriteSet,
			double x, double y, double z, double dx, double dy, double dz) {
		super(clientLevel, x, y, z, dx, dy, dz);

		setParticleSpeed(dx, dy, dz);
		this.gravity = 0.5f;
		// this.x = x;
		// this.y = y;
		// this.z = z;
		// this.
		this.lifetime = 120;
		this.friction = 1.0f;
		// this.friction = 1.1f;
		this.alpha = 0.4f;
		this.scale(4);
		this.setSpriteFromAge(spriteSet);
	}

	@Override
	public void tick() {
		super.tick();

		this.scale(1.01f);
		this.alpha = Mth.lerp(this.age / (float) this.lifetime, 0.3f, 0f);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double dx, double dy, double dz) {
			return new GeyserParticle(level, this.spriteSet, x, y, z, dx, dy, dz);
		}
	}

}
