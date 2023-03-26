package net.xavil.universal.common.universe.universe;

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
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;

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
		}
		profiler.pop();
	}

	// private static CelestialNode parseSystemJson(JsonElement elem) {
	// 	return parseSystemNode(GsonHelper.getAsJsonObject(elem.getAsJsonObject(), "root"));
	// }

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

	// private static CelestialNode parseSystemNode(JsonObject elem) {
	// 	elem.getAsJsonObject();
	// }
	
}
