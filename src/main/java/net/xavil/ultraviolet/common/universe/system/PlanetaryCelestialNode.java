package net.xavil.ultraviolet.common.universe.system;

import java.util.OptionalLong;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.common.level.ModChunkGenerator;

public non-sealed class PlanetaryCelestialNode extends UnaryCelestialNode {

	public static enum Modifier {
		NONE,
		ANOMALOUS,
		QUIET,
		VERDANT,
		ERODED,
	}

	// i dont really like this
	public enum Type {
		// idk what to use for rigidities here....... idk what rigidity is even measuring. (N m^-2)
		BROWN_DWARF(false, 1e8),
		GAS_GIANT(false, 1e8),
		ICE_WORLD(true, 4e9),
		ROCKY_WORLD(true, 3e10),
		ROCKY_ICE_WORLD(true, 1e10),
		WATER_WORLD(true, 3e10),
		EARTH_LIKE_WORLD(true, 3e10);

		public final boolean isLandable;
		public final double rigidity;

		private Type(boolean isLandable, double rigidity) {
			this.isLandable = isLandable;
			this.rigidity = rigidity;
		}
	}

	public Type type;

	public PlanetaryCelestialNode() {
	}

	public PlanetaryCelestialNode(Type type, double massYg) {
		super(massYg);
		this.type = type;
	}

	public PlanetaryCelestialNode(Type type, double massYg, double radiusRearth, double temperatureK) {
		super(massYg);
		this.type = type;
		this.radius = Units.km_PER_Rearth * radiusRearth;
		this.temperature = temperatureK;
	}

	public Holder<DimensionType> dimensionType(MinecraftServer server) {
		if (this.type == Type.EARTH_LIKE_WORLD) {
			final var registryAccess = server.registryAccess();
			final var type = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
					.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION);
			return type;
		}

		final var ultrawarm = this.temperature > 800;

		return Holder.direct(DimensionType.create(
				OptionalLong.of(18000), true, false, ultrawarm, false, 1, false, false, true, true, false,
				-64, 384, 384,
				BlockTags.INFINIBURN_OVERWORLD, DimensionType.OVERWORLD_EFFECTS, 0.0f));
	}

	public Supplier<LevelStem> dimensionProperties(MinecraftServer server) {
		if (!this.type.isLandable)
			return null;
		return () -> {
			final var registryAccess = server.registryAccess();
			final ChunkGenerator generator;
			if (this.type == Type.EARTH_LIKE_WORLD) {
				generator = WorldGenSettings.makeDefaultOverworld(registryAccess, this.seed);
			} else {
				generator = new ModChunkGenerator(registryAccess, this);
			}
			return new LevelStem(dimensionType(server), generator);
		};
	}

	public boolean hasAtmosphere() {
		return this.type == Type.EARTH_LIKE_WORLD || this.type == Type.WATER_WORLD;
	}

	public double surfaceGravityEarthRelative() {
		final var radiusRearth = Units.Rearth_PER_km * this.radius;
		final var massMearth = Units.Mearth_PER_Yg * this.massYg;
		return massMearth / (radiusRearth * radiusRearth);
	}

}