package net.xavil.universal.common.universe.system;

import java.util.OptionalLong;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.xavil.universal.common.dimension.DimensionCreationProperties;

public non-sealed class PlanetNode extends StarSystemNode {

	public enum Type {
		GAS_GIANT(false),
		ICE_WORLD(true),
		ROCKY_WORLD(true),
		ROCKY_ICE_WORLD(true),
		WATER_WORLD(true),
		EARTH_LIKE_WORLD(true);

		public final boolean isLandable;

		private Type(boolean isLandable) {
			this.isLandable = isLandable;
		}
	}

	public Type type;
	public double radiusRearth;
	public double temperatureK;

	public PlanetNode(Type type, double massYg, double radiusRearth, double temperatureK) {
		super(massYg);
		this.type = type;
		this.radiusRearth = radiusRearth;
		this.temperatureK = temperatureK;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("PlanetaryBodyNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("radiusRearth=" + this.radiusRearth + ", ");
		builder.append("temperatureK=" + this.temperatureK + ", ");
		builder.append("]");
		return builder.toString();
	}

	public Holder<DimensionType> dimensionType(MinecraftServer server) {
		if (this.type == Type.EARTH_LIKE_WORLD) {
			var registryAccess = server.registryAccess();
			var type = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
					.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION);
			return type;
		}

		var ultrawarm = this.temperatureK > 800;
		var natural = this.type == Type.EARTH_LIKE_WORLD;

		return Holder.direct(DimensionType.create(
				OptionalLong.of(1000), true, false, ultrawarm, natural, 1, false, false, true, true, false,
				-64, 384, 384,
				BlockTags.INFINIBURN_OVERWORLD, DimensionType.OVERWORLD_EFFECTS, 0.0f));
	}

	public Supplier<DimensionCreationProperties> dimensionProperties(MinecraftServer server) {
		if (!this.type.isLandable)
			return null;
		return () -> {
			var registryAccess = server.registryAccess();
			var generator = WorldGenSettings.makeDefaultOverworld(registryAccess, new Random().nextLong());
			var levelStem = new LevelStem(dimensionType(server), generator);
			return DimensionCreationProperties.basic(levelStem);
		};
	}

	// planet type (gas giant, icy world, rocky world, earth-like world, etc)
	// mass, surface gravity, atmosphere type, landable
	// cloud coverage, greenhouse effect, volcanism, plate tectonics
	// asteroid belt/rings? perhaps a single disc object?

}