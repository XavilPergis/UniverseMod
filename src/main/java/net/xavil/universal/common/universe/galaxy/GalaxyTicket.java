package net.xavil.universal.common.universe.galaxy;

import java.util.function.BooleanSupplier;

import net.minecraft.core.Vec3i;

public abstract class GalaxyTicket {

	public final Vec3i center;
	public final int radius;

	public GalaxyTicket(Vec3i center, int radius) {
		this.center = center;
		this.radius = radius;
	}

	public abstract boolean isExpired();

	public GalaxyTicket wrap(Vec3i center, int radius, BooleanSupplier supplier) {
		return new GalaxyTicket(center, radius) {
			@Override
			public boolean isExpired() {
				return supplier.getAsBoolean();
			}
		};
	}

	public static class PermanentTicket extends GalaxyTicket {
		public PermanentTicket(Vec3i center, int radius) {
			super(center, radius);
		}

		@Override
		public boolean isExpired() {
			return false;
		}
	}

}
