package net.xavil.ultraviolet.common.universe;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.ultraviolet.common.universe.system.BinaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.CelestialNodeChild;
import net.xavil.ultraviolet.common.universe.system.OtherCelestialNode;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;

public class AuthoredSystemResource extends SimpleJsonResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MutableMap<ResourceLocation, CompoundTag> nodes = MutableMap.hashMap();

    public AuthoredSystemResource() {
        super(GSON, "systems/authored");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> systemMap0,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {

        final var systemMap = ImmutableMap.proxy(systemMap0);
    }

    public Maybe<CelestialNode> getNode(ResourceLocation location) {
        return nodes.get(location).map(CelestialNode::readNbt);
    }

    private static CelestialNode createNode(String type) {
        if (type.equals("binary"))
            return new BinaryCelestialNode();
        if (type.equals("star"))
            return new StellarCelestialNode();
        if (type.equals("planet"))
            return new PlanetaryCelestialNode();
        if (type.equals("other"))
            return new OtherCelestialNode();
        return null;
    }

    private double getUnit(JsonElement elem, String dstUnit) {
        if (!elem.isJsonArray())
            return elem.getAsDouble();

        final var arr = elem.getAsJsonArray();
        final var value = arr.get(0).getAsDouble();
        final var srcUnit = arr.get(1).getAsString();

        return Units.convert(value, srcUnit, dstUnit);
    }

    // private OrbitalPlane parseOrbitalPlane(JsonObject obj) {
    //     // final var frame = ;
    //     // parent_equatorial, laplace, ecliptic
    //     final var inclination = getUnit(obj.get("inclination"), "rad");
    //     final var ascNode = getUnit(obj.get("ascending_node"), "rad");
    //     final var argPeri = getUnit(obj.get("arg_of_periapsis"), "rad");
    // }

    private CelestialNodeChild<?> parseChildNode(CelestialNode parent, JsonElement elem) {
        final var obj = elem.getAsJsonObject();
        final var childNode = parseNode(obj.get("node"));

        final var eccentricity = obj.get("eccentricity").getAsDouble();
        final var semiMajor = getUnit(obj.get("semi_major"), "Tm");
        final var inclination = getUnit(obj.get("inclination"), "rad");
        final var ascNode = getUnit(obj.get("ascending_node"), "rad");
        final var argPeri = getUnit(obj.get("arg_of_periapsis"), "rad");

        final var orbitalShape = new OrbitalShape(eccentricity, semiMajor);
        final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascNode, argPeri);

        return new CelestialNodeChild<>(parent, childNode, orbitalShape, orbitalPlane, 0);
    }

    private StellarCelestialNode.Type parseStarType(String type) {
        return switch (type) {
            case "star" -> StellarCelestialNode.Type.STAR;
            case "white_dwarf" -> StellarCelestialNode.Type.WHITE_DWARF;
            case "neutron_star" -> StellarCelestialNode.Type.NEUTRON_STAR;
            case "black_hole" -> StellarCelestialNode.Type.BLACK_HOLE;
            default -> null;
        };
    }

    private PlanetaryCelestialNode.Type parsePlanetType(String type) {
        return switch (type) {
            case "rocky" -> PlanetaryCelestialNode.Type.ROCKY_WORLD;
            case "rocky_icy" -> PlanetaryCelestialNode.Type.ROCKY_ICE_WORLD;
            case "icy" -> PlanetaryCelestialNode.Type.ICE_WORLD;
            case "earth_like" -> PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD;
            case "watery" -> PlanetaryCelestialNode.Type.WATER_WORLD;
            case "gas_giant" -> PlanetaryCelestialNode.Type.GAS_GIANT;
            case "ice_giant" -> PlanetaryCelestialNode.Type.GAS_GIANT;
            case "brown_dwarf" -> PlanetaryCelestialNode.Type.BROWN_DWARF;
            default -> null;
        };
    }

    private CelestialNode parseNode(JsonElement elem) {
        final var obj = elem.getAsJsonObject();
        final var node = createNode(obj.get("type").getAsString());

        if (obj.has("name"))
            node.explicitName = obj.get("name").getAsString();
        node.massYg = getUnit(obj.get("mass"), "Yg");

        if (node instanceof BinaryCelestialNode binaryNode) {
            final var nodeA = parseNode(obj.get("a"));
            final var nodeB = parseNode(obj.get("b"));
            binaryNode.setSiblings(nodeA, nodeB);

            final var eccentricity = obj.get("eccentricity").getAsDouble();
            final var semiMajor = getUnit(obj.get("semi_major_outer"), "Tm");
            final var inclination = getUnit(obj.get("inclination"), "rad");
            final var ascNode = getUnit(obj.get("ascending_node"), "rad");
            final var argPeri = getUnit(obj.get("arg_of_periapsis"), "rad");

            final var orbitalShape = new OrbitalShape(eccentricity, semiMajor);
            final var orbitalPlane = OrbitalPlane.fromOrbitalElements(inclination, ascNode, argPeri);

            binaryNode.setOrbitalShapes(orbitalShape);
            binaryNode.orbitalPlane = orbitalPlane;
        }
        if (node instanceof UnaryCelestialNode unaryNode) {
            unaryNode.obliquityAngle = getUnit(obj.get("obliquity"), "rad");
            unaryNode.rotationalRate = 2 * Math.PI / getUnit(obj.get("rotation_period"), "s");
            unaryNode.radius = getUnit(obj.get("radius"), "km");
            unaryNode.temperature = getUnit(obj.get("temperature"), "K");

            // final var rings = nbt.getList("rings", Tag.TAG_COMPOUND);
            // for (int i = 0; i < rings.size(); ++i) {
            // final var ringNbt = rings.getCompound(i);
            // final var plane = getNbt(OrbitalPlane.CODEC,
            // ringNbt.getCompound("orbital_plane"));
            // final var eccentricity = ringNbt.getDouble("eccentricity");
            // final var ringMass = ringNbt.getDouble("mass");
            // final var lower = ringNbt.getDouble("interval_lower");
            // final var higher = ringNbt.getDouble("interval_higher");
            // unaryNode.rings.push(new CelestialRing(plane, eccentricity, new
            // Interval(lower, higher), ringMass));
            // }
        }
        if (node instanceof StellarCelestialNode starNode) {
            starNode.type = parseStarType(obj.get("star_type").getAsString());
            starNode.luminosityLsol = getUnit(obj.get("luminosity"), "Lsol");
        }
        if (node instanceof PlanetaryCelestialNode planetNode) {
            planetNode.type = parsePlanetType(obj.get("planet_type").getAsString());
        }

        if (obj.has("children")) {
            final var childList = obj.getAsJsonArray("children");
            for (var i = 0; i < childList.size(); ++i) {
                node.insertChild(parseChildNode(node, childList.get(i).getAsJsonObject()));
            }
        }

        return node;
    }

}
