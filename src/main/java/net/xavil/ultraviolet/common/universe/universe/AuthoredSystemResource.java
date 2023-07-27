package net.xavil.ultraviolet.common.universe.universe;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;

public class AuthoredSystemResource extends SimpleJsonResourceReloadListener {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Map<ResourceLocation, CelestialNode> authoredSystems = new HashMap<>();

	public AuthoredSystemResource() {
		super(GSON, "systems/authored");
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
		profiler.push("authored_system");
		for (final var entry : elements.entrySet()) {
			final var location = entry.getKey();
			final var element = entry.getValue();
			if (!element.isJsonObject()) {
				Mod.LOGGER.error("The root object of authored system '{}' was not an object", location);
				continue;
			}

			final var obj = element.getAsJsonObject();
			// entry.getValue().getAsJsonObject()
		}
		profiler.pop();
	}

	private static CelestialNode parseSystemJson(JsonObject obj) {
		final var root = GsonHelper.getAsJsonObject(obj, "root", null);
		return parseSystemNode(root);
	}

	// private static CelestialNodeChild<?> parseUnaryOrbit(JsonObject elem, CelestialNode parent) {
	// 	final var frame = GsonHelper.getAsString(elem, "frame");
	// 	final var eccentricity = GsonHelper.getAsDouble(elem, "eccentricity");
	// 	final var semiMajor = GsonHelper.getAsDouble(elem, "semi_major_axis");
	// 	final var inclination = GsonHelper.getAsDouble(elem, "inclination");
	// 	final var ascendingNode = GsonHelper.getAsDouble(elem, "ascending_node");
	// 	final var periapsisArg = GsonHelper.getAsDouble(elem, "arg_of_periapsis");
	// 	// "frame": "ecliptic",
	// 	// "eccentricity": 0.20563593,
	// 	// "semi_major_axis": 0.38709927,
	// 	// "inclination": 7.00497902,
	// 	// "ascending_node": 48.33076593,
	// 	// "arg_of_periapsis": 29.1270303
	// 	return new CelestialNodeChild<>(parent, cur, shape, plane, offset);
	// }

	private static StellarCelestialNode.Type parseStarType(String type) {
		return switch (type) {
			case "main_sequence" -> StellarCelestialNode.Type.MAIN_SEQUENCE;
			case "giant" -> StellarCelestialNode.Type.GIANT;
			case "white_dwarf" -> StellarCelestialNode.Type.WHITE_DWARF;
			case "neutron_star" -> StellarCelestialNode.Type.NEUTRON_STAR;
			case "black_hole" -> StellarCelestialNode.Type.BLACK_HOLE;
			default -> null;
		};
	}

	private static PlanetaryCelestialNode.Type parsePlanetType(String type) {
		return switch (type) {
			case "earth_like_world" -> PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD;
			case "gas_giant" -> PlanetaryCelestialNode.Type.GAS_GIANT;
			case "ice_world" -> PlanetaryCelestialNode.Type.ICE_WORLD;
			case "rocky_ice_world" -> PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD;
			case "rocky_world" -> PlanetaryCelestialNode.Type.ROCKY_WORLD;
			case "water_world" -> PlanetaryCelestialNode.Type.WATER_WORLD;
			default -> null;
		};
	}

	public static class StellarParameters {
		public double mass;
		public double luminosity;
		public double radius;
		public double temperature;
		public double obliquity;
		public double rotationPeriod;

		public static StellarParameters fromMass(double mass) {
			final var params = new StellarParameters();
			params.mass = mass;
			params.luminosity = StellarCelestialNode.mainSequenceLuminosityFromMass(mass);
			params.radius = StellarCelestialNode.mainSequenceRadiusFromMass(mass);
			params.temperature = StellarCelestialNode.temperature(params.radius, params.luminosity);
			return params;
		}

		public StellarCelestialNode makeStarNode(StellarCelestialNode.Type type) {
			final var node = new StellarCelestialNode(type, mass, luminosity, radius, temperature);
			node.obliquityAngle = obliquity;
			node.rotationalRate = 2.0 * Math.PI / rotationPeriod;
			return node;
		}
	}

	public static class PlanetaryParameters {
		public double mass;
		public double radius = 1;
		public double temperature = 288;
		public double obliquity;
		public double rotationPeriod;

		public static PlanetaryParameters fromMass(double mass) {
			final var params = new PlanetaryParameters();
			params.mass = mass;
			return params;
		}

		public PlanetaryCelestialNode makePlanetNode(PlanetaryCelestialNode.Type type) {
			final var node = new PlanetaryCelestialNode(type, mass, radius, temperature);
			node.obliquityAngle = obliquity;
			node.rotationalRate = 2.0 * Math.PI / rotationPeriod;
			return node;
		}
	}

	private static CelestialNode parseSystemNode(JsonObject obj) {
		return switch (GsonHelper.getAsString(obj, "type", null)) {
			case "star" -> {
				final var type = parseStarType(GsonHelper.getAsString(obj, "star_type", null));
				final var mass = Units.Yg_PER_Msol * GsonHelper.getAsDouble(obj, "mass", 0.0);

				final var params = StellarParameters.fromMass(mass);
				params.luminosity = GsonHelper.getAsDouble(obj, "luminosity", params.luminosity);
				params.radius = GsonHelper.getAsDouble(obj, "radius", params.radius);
				params.temperature = GsonHelper.getAsDouble(obj, "temperature", params.temperature);
				params.obliquity = GsonHelper.getAsDouble(obj, "obliquity", params.obliquity);
				params.rotationPeriod = GsonHelper.getAsDouble(obj, "rotation_period", params.rotationPeriod);

				yield params.makeStarNode(type);
			}
			case "planet" -> {
				final var type = parsePlanetType(GsonHelper.getAsString(obj, "planet_type", null));
				final var mass = Units.Yg_PER_Mearth * GsonHelper.getAsDouble(obj, "mass", 0.0);

				final var params = PlanetaryParameters.fromMass(mass);
				params.radius = GsonHelper.getAsDouble(obj, "radius", params.radius);
				params.temperature = GsonHelper.getAsDouble(obj, "temperature", params.temperature);
				params.obliquity = GsonHelper.getAsDouble(obj, "obliquity", params.obliquity);
				params.rotationPeriod = GsonHelper.getAsDouble(obj, "rotation_period", params.rotationPeriod);

				yield params.makePlanetNode(type);
			}
			// case "binary" -> {
			// 	final var radius = GsonHelper.getAsDouble(obj, "radius", 0.0);
			// 	final var temperature = GsonHelper.getAsDouble(obj, "temperature", 0.0);
			// 	final var node = new PlanetaryCelestialNode(type, mass, radius, temperature);
			// 	node.obliquityAngle = GsonHelper.getAsDouble(obj, "obliquity", 0.0);
			// 	node.rotationalRate = GsonHelper.getAsDouble(obj, "rotation_period", 0.0);
			// 	yield node;
			// }
			default -> null;
		};
	}
	
}
