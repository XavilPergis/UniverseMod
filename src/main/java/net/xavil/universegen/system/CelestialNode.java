package net.xavil.universegen.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.xavil.universal.Mod;
import net.xavil.util.Assert;
import net.xavil.util.Units;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.math.Ellipse;
import net.xavil.util.math.Formulas;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;
import net.xavil.util.math.matrices.Vec3;

public abstract sealed class CelestialNode permits
		StellarCelestialNode, BinaryCelestialNode, PlanetaryCelestialNode, OtherCelestialNode {

	public static final int UNASSINED_ID = -1;

	protected int id = UNASSINED_ID;
	protected @Nullable BinaryCelestialNode parentBinaryNode = null;
	protected @Nullable CelestialNodeChild<?> parentUnaryNode = null;
	protected final List<CelestialNodeChild<?>> childNodes = new ArrayList<>();
	protected final List<CelestialRing> rings = new ArrayList<>();

	public Vec3 position = Vec3.ZERO, lastPosition = Vec3.ZERO;
	public OrbitalPlane referencePlane = OrbitalPlane.ZERO;

	public double massYg; // Yg
	// TODO: these quantities are meaningless for binary orbits!
	public double obliquityAngle; // rad
	public double rotationalPeriod; // s

	public CelestialNode(double massYg) {
		this.massYg = massYg;
	}

	/**
	 * This method assumes it is being called on the root node.
	 */
	public final void assignIds() {
		assignSubtreeIds(0);
	}

	/**
	 * Assigns IDs to each celestial node such that each node's ID is greater than
	 * is descendants, but less than its siblings and ancestors. This allows
	 * {@link CelestialNode#lookup(int)} to not search the entire tree for a node
	 * on each lookup.
	 * 
	 * @param startId The ID at which all descendant nodes should be greater than.
	 * @return The maximum ID contained within `this`, including itself.
	 */
	protected final int assignSubtreeIds(int startId) {
		if (this instanceof BinaryCelestialNode binaryNode) {
			startId = binaryNode.getA().assignSubtreeIds(startId) + 1;
			startId = binaryNode.getB().assignSubtreeIds(startId) + 1;
		}
		for (var child : this.childNodes) {
			startId = child.node.assignSubtreeIds(startId) + 1;
		}

		this.id = startId;
		return startId;
	}

	/**
	 * This method may be called on any subtree.
	 */
	public final @Nullable CelestialNode lookup(int id) {
		Assert.isNotEqual(this.id, UNASSINED_ID);
		return (id < 0 || id > this.id) ? null : lookupSubtree(id);
	}

	public final int getId() {
		return this.id;
	}

	protected final CelestialNode lookupSubtree(int id) {
		Assert.isNotEqual(this.id, UNASSINED_ID);

		if (this.id == id)
			return this;

		// each node's ID is strictly higher than each child node's ID, meaning we can
		// cut out searching a branch entirely if the ID we're looking for is higher
		// than the maximum ID in a branch.
		if (id > this.id)
			return null;

		if (this instanceof BinaryCelestialNode binaryNode) {
			var a = binaryNode.getA().lookupSubtree(id);
			if (a != null)
				return a;
			var b = binaryNode.getB().lookupSubtree(id);
			if (b != null)
				return b;
		}

		for (var child : this.childNodes) {
			var node = child.node.lookupSubtree(id);
			if (node != null)
				return node;
		}

		return null;
	}

	/**
	 * Visit each node in this tree in an arbitrary order, including the node that
	 * method was called on.
	 * 
	 * @param consumer The consumer that accepts each node.
	 */
	public void visit(Consumer<CelestialNode> consumer) {
		consumer.accept(this);
		if (this instanceof BinaryCelestialNode binaryNode) {
			binaryNode.getA().visit(consumer);
			binaryNode.getB().visit(consumer);
		}
		this.childNodes.forEach(child -> child.node.visit(consumer));
	}

	/**
	 * Visit each direct descendant, instead of recursively visiting like
	 * {@link #visit(Consumer)} does.
	 * 
	 * @param consumer The consumer that accepts each node.
	 */
	public void visitDirectDescendants(Consumer<CelestialNode> consumer) {
		if (this instanceof BinaryCelestialNode binaryNode) {
			consumer.accept(binaryNode.getA());
			consumer.accept(binaryNode.getB());
		}
		this.childNodes.forEach(child -> consumer.accept(child.node));
	}

	public ImmutableList<CelestialNode> selfAndChildren() {
		final var nodes = new Vector<CelestialNode>();
		visit(nodes::push);
		return nodes;
	}

	// public Iterator<CelestialNode> iter() {
	// // return selfAndChildren();
	// }

	public int find(CelestialNode node) {
		return find(other -> other == node);
	}

	/**
	 * Find the node ID of the first node according to a depth-first search that
	 * matches the provided predicate.
	 * 
	 * @param predicate The search predicate.
	 * @return The node ID.
	 */
	public int find(Predicate<CelestialNode> predicate) {
		if (predicate.test(this))
			return this.id;
		if (this instanceof BinaryCelestialNode binaryNode) {
			var a = binaryNode.getA().find(predicate);
			if (a != -1)
				return a;
			var b = binaryNode.getB().find(predicate);
			if (b != -1)
				return b;
		}
		for (var child : this.childNodes) {
			var id = child.node.find(predicate);
			if (id != -1)
				return id;
		}
		return -1;
	}

	public void setBinaryParent(BinaryCelestialNode parent) {
		this.parentBinaryNode = parent;
	}

	public BinaryCelestialNode getBinaryParent() {
		return this.parentBinaryNode;
	}

	public CelestialNode getUnaryParent() {
		return this.parentUnaryNode == null ? null : this.parentUnaryNode.parentNode;
	}

	public CelestialNodeChild<?> getOrbitInfo() {
		return this.parentUnaryNode;
	}

	public Iterable<CelestialNodeChild<?>> childOrbits() {
		return this.childNodes;
	}

	public void addRing(CelestialRing newRing) {
		this.rings.add(newRing);
	}

	public Iterable<CelestialRing> rings() {
		return this.rings;
	}

	/**
	 * An alias for {@link #insertChild(CelestialNodeChild)} that takes the child
	 * node and its orbital elements directly, instead of an already-constructed
	 * {@link CelestialNodeChild}.
	 */
	public void insertChild(CelestialNode child, double eccentricity, double periapsisDistance, double inclination,
			double longitudeOfAscendingNode, double argumentOfPeriapsis, double offset) {
		final var shape = new OrbitalShape(eccentricity, periapsisDistance);
		final var plane = OrbitalPlane.fromOrbitalElements(inclination, longitudeOfAscendingNode, argumentOfPeriapsis);
		insertChild(new CelestialNodeChild<>(this, child, shape, plane, offset));
	}

	/**
	 * Inserts a child node into a unary orbit around this node.
	 * 
	 * @param child The child to insert.
	 */
	public void insertChild(CelestialNodeChild<?> child) {
		Assert.isReferentiallyEqual(this, child.parentNode);
		this.childNodes.add(child);
		child.node.parentUnaryNode = child;
	}

	/**
	 * Calculates and updates the positions and related details for each subnode of
	 * this tree, according to the passed time.
	 * 
	 * @param time The amount of elapsed time, in seconds. Note that this is the
	 *             total elapsed time, and *not* the delta time.
	 */
	public void updatePositions(double time) {
		updatePositions(OrbitalPlane.ZERO, time);
	}

	protected void updatePositions(OrbitalPlane referencePlane, double time) {
		this.referencePlane = referencePlane;

		if (this instanceof BinaryCelestialNode binaryNode) {
			final var combinedShape = binaryNode.getCombinedShape();
			final var ellipseA = binaryNode.getEllipseA(referencePlane);
			final var ellipseB = binaryNode.getEllipseB(referencePlane);
			binaryNode.getA().position = getOrbitalPosition(ellipseA, combinedShape, false, time);
			binaryNode.getB().position = getOrbitalPosition(ellipseB, combinedShape, true, time);
			final var newPlane = binaryNode.orbitalPlane.withReferencePlane(referencePlane);
			binaryNode.getA().updatePositions(newPlane, time);
			binaryNode.getB().updatePositions(newPlane, time);
		}

		for (var childOrbit : this.childOrbits()) {
			final var newPlane = childOrbit.orbitalPlane.withReferencePlane(referencePlane);
			childOrbit.node.position = getOrbitalPosition(newPlane, childOrbit.orbitalShape, false, time);
			childOrbit.node.updatePositions(newPlane, time);
		}
	}

	public Vec3 getPosition(float partialTick) {
		return Vec3.lerp(partialTick, this.lastPosition, this.position);
	}

	public Vec3 getOrbitalPosition(OrbitalPlane plane, OrbitalShape shape, boolean reverse, double time) {
		final var ellipse = Ellipse.fromOrbit(this.position, plane, shape, reverse);
		return getOrbitalPosition(ellipse, shape, reverse, time);
	}

	public Vec3 getOrbitalPosition(Ellipse ellipse, OrbitalShape shape, boolean reverse, double time) {
		final var orbitalPeriod = Formulas.orbitalPeriod(shape.semiMajor(), this.massYg);
		final var meanAnomaly = (2 * Math.PI / orbitalPeriod) * time;
		final var trueAnomaly = Formulas.calculateTrueAnomaly(meanAnomaly, shape.eccentricity());
		return ellipse.pointFromTrueAnomaly(reverse ? -trueAnomaly : trueAnomaly);
	}

	@FunctionalInterface
	public interface PropertyConsumer {
		void addProperty(String name, String info);
	}

	private String describeStar(StellarCelestialNode starNode) {
		String starKind = "";
		final var starClass = starNode.starClass();
		if (starClass != null)
			starKind += "Class " + starClass.name + " ";
		if (starNode.type == StellarCelestialNode.Type.BLACK_HOLE)
			starKind += "Black Hole ";
		else if (starNode.type == StellarCelestialNode.Type.NEUTRON_STAR)
			starKind += "Neutron Star ";
		else if (starNode.type == StellarCelestialNode.Type.WHITE_DWARF)
			starKind += "White Dwarf ";
		else if (starNode.type == StellarCelestialNode.Type.GIANT)
			starKind += "(Giant) ";
		return starKind;
	}

	public void describe(PropertyConsumer consumer) {
		if (this instanceof BinaryCelestialNode)
			return;

		// consumer.accept("Mass", String.format("%.2f Yg", this.massYg));
		consumer.addProperty("Obliquity", String.format("%.2f rad", this.obliquityAngle));
		consumer.addProperty("Rotational Period", String.format("%.2f s", this.rotationalPeriod));

		// TODO: inclination n stuff

		if (this instanceof StellarCelestialNode starNode) {
			consumer.addProperty("Mass",
					String.format("%.4e Yg (%.2f M☉)", this.massYg, this.massYg / Units.Yg_PER_Msol));
			consumer.addProperty("Luminosity", String.format("%.6f L☉", starNode.luminosityLsol));
			consumer.addProperty("Radius", String.format("%.2f R☉", starNode.radiusRsol));
			consumer.addProperty("Temperature", String.format("%.0f K", starNode.temperatureK));
			// var starClass = starNode.starClass();
			// if (starClass != null)
			// consumer.addProperty("Spectral Class", starClass.name);
			consumer.addProperty("Type", describeStar(starNode));
			// consumer.addProperty("Type", starNode.type.name());
		} else if (this instanceof PlanetaryCelestialNode planetNode) {
			if (planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
				consumer.addProperty("Mass",
						String.format("%.2f Yg (%.2f M♃)", this.massYg, this.massYg / Units.Yg_PER_Mjupiter));
			} else {
				consumer.addProperty("Mass",
						String.format("%.2f Yg (%.2f Mⴲ)", this.massYg, this.massYg / Units.Yg_PER_Mearth));
			}
			consumer.addProperty("Type", planetNode.type.name());
			consumer.addProperty("Temperature", String.format("%.0f K", planetNode.temperatureK));
			if (planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
				consumer.addProperty("Radius", String.format("%.2f R♃",
						planetNode.radiusRearth * (Units.m_PER_Rearth / Units.m_PER_Rjupiter)));
			} else {
				consumer.addProperty("Radius", String.format("%.2f Rⴲ", planetNode.radiusRearth));
			}
		}
	}

	public static CelestialNode readNbt(CompoundTag nbt) {
		CelestialNode node = null;

		var id = nbt.getInt("id");
		var massYg = nbt.getDouble("mass");
		var x = nbt.getDouble("x");
		var y = nbt.getDouble("y");
		var z = nbt.getDouble("z");
		var position = Vec3.from(x, y, z);
		var obliquityAngle = nbt.getDouble("obliquity");
		var rotationalPeriod = nbt.getDouble("rotational_period");
		var nodeType = nbt.getString("node_type");

		var referencePlane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, nbt.get("reference_plane"))
				.getOrThrow(false, Mod.LOGGER::error);

		if (nodeType.equals("binary")) {
			var a = readNbt(nbt.getCompound("a"));
			var b = readNbt(nbt.getCompound("b"));
			var orbitalPlane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, nbt.get("orbital_plane"))
					.getOrThrow(false, Mod.LOGGER::error);
			var orbitalShapeA = OrbitalShape.CODEC.parse(NbtOps.INSTANCE, nbt.get("orbital_shape_a"))
					.getOrThrow(false, Mod.LOGGER::error);
			var orbitalShapeB = OrbitalShape.CODEC.parse(NbtOps.INSTANCE, nbt.get("orbital_shape_b"))
					.getOrThrow(false, Mod.LOGGER::error);
			var offset = nbt.getDouble("offset");
			node = new BinaryCelestialNode(massYg, a, b, orbitalPlane, orbitalShapeA, orbitalShapeB, offset);
		} else if (nodeType.equals("star")) {
			var type = StellarCelestialNode.Type.values()[nbt.getInt("type")];
			var luminosity = nbt.getDouble("luminosity");
			var radius = nbt.getDouble("radius");
			var temperature = nbt.getDouble("temperature");
			// NOTE: we only use protoplanetary disc information on the server, and we only
			// serialize systems in the clientbound direction currently. Something to watch
			// out for!
			node = new StellarCelestialNode(type, massYg, luminosity, radius, temperature);
		} else if (nodeType.equals("planet")) {
			var type = PlanetaryCelestialNode.Type.values()[nbt.getInt("type")];
			var radius = nbt.getDouble("radius");
			var temperature = nbt.getDouble("temperature");
			node = new PlanetaryCelestialNode(type, massYg, radius, temperature);
		}

		node.position = position;
		node.referencePlane = referencePlane;
		node.id = id;
		node.obliquityAngle = obliquityAngle;
		node.rotationalPeriod = rotationalPeriod;

		final var rings = nbt.getList("rings", Tag.TAG_COMPOUND);
		for (int i = 0; i < rings.size(); ++i) {
			final var ringNbt = rings.getCompound(i);

			final var plane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, ringNbt.getCompound("orbital_plane"))
					.getOrThrow(false, Mod.LOGGER::error);
			final var eccentricity = ringNbt.getDouble("eccentricity");
			final var mass = ringNbt.getDouble("mass");
			final var lower = ringNbt.getDouble("interval_lower");
			final var higher = ringNbt.getDouble("interval_higher");
			node.rings.add(new CelestialRing(plane, eccentricity, new Interval(lower, higher), mass));
		}
		nbt.put("rings", rings);

		final var childList = nbt.getList("children", Tag.TAG_COMPOUND);
		for (var i = 0; i < childList.size(); ++i) {
			var childNbt = childList.getCompound(i);

			var childNode = readNbt(childNbt.getCompound("node"));
			var offset = childNbt.getDouble("offset");
			var orbitalPlane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, childNbt.get("orbital_plane"))
					.getOrThrow(false, Mod.LOGGER::error);
			var orbitalShape = OrbitalShape.CODEC.parse(NbtOps.INSTANCE, childNbt.get("orbital_shape"))
					.getOrThrow(false, Mod.LOGGER::error);

			var child = new CelestialNodeChild<>(node, childNode, orbitalShape, orbitalPlane, offset);
			node.insertChild(child);
		}

		return node;
	}

	public static CompoundTag writeNbt(CelestialNode node) {
		var nbt = new CompoundTag();

		nbt.putInt("id", node.id);
		nbt.putDouble("mass", node.massYg);
		nbt.putDouble("x", node.position.x);
		nbt.putDouble("y", node.position.y);
		nbt.putDouble("z", node.position.z);
		nbt.putDouble("obliquity", node.obliquityAngle);
		nbt.putDouble("rotational_period", node.rotationalPeriod);

		OrbitalPlane.CODEC.encodeStart(NbtOps.INSTANCE, node.referencePlane)
				.resultOrPartial(Mod.LOGGER::error)
				.ifPresent(n -> nbt.put("reference_plane", n));

		if (node instanceof BinaryCelestialNode binaryNode) {
			nbt.putString("node_type", "binary");
			nbt.put("a", writeNbt(binaryNode.getA()));
			nbt.put("b", writeNbt(binaryNode.getB()));
			OrbitalPlane.CODEC.encodeStart(NbtOps.INSTANCE, binaryNode.orbitalPlane)
					.resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> nbt.put("orbital_plane", n));
			OrbitalShape.CODEC.encodeStart(NbtOps.INSTANCE, binaryNode.orbitalShapeA)
					.resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> nbt.put("orbital_shape_a", n));
			OrbitalShape.CODEC.encodeStart(NbtOps.INSTANCE, binaryNode.orbitalShapeB)
					.resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> nbt.put("orbital_shape_b", n));
			nbt.putDouble("offset", binaryNode.offset);
		} else if (node instanceof StellarCelestialNode starNode) {
			nbt.putString("node_type", "star");
			nbt.putInt("type", starNode.type.ordinal());
			nbt.putDouble("luminosity", starNode.luminosityLsol);
			nbt.putDouble("radius", starNode.radiusRsol);
			nbt.putDouble("temperature", starNode.temperatureK);
		} else if (node instanceof PlanetaryCelestialNode planetNode) {
			nbt.putString("node_type", "planet");
			nbt.putInt("type", planetNode.type.ordinal());
			nbt.putDouble("radius", planetNode.radiusRearth);
			nbt.putDouble("temperature", planetNode.temperatureK);
		}

		final var rings = new ListTag();
		for (var ring : node.rings) {
			final var ringNbt = new CompoundTag();
			OrbitalPlane.CODEC.encodeStart(NbtOps.INSTANCE, ring.orbitalPlane)
					.resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> ringNbt.put("orbital_plane", n));
			ringNbt.putDouble("eccentricity", ring.eccentricity);
			ringNbt.putDouble("mass", ring.mass);
			ringNbt.putDouble("interval_lower", ring.interval.lower());
			ringNbt.putDouble("interval_higher", ring.interval.higher());
			rings.add(ringNbt);
		}
		nbt.put("rings", rings);

		final var children = new ListTag();
		for (var child : node.childNodes) {
			var childNbt = new CompoundTag();

			childNbt.put("node", writeNbt(child.node));
			childNbt.putDouble("offset", child.offset);

			OrbitalPlane.CODEC.encodeStart(NbtOps.INSTANCE, child.orbitalPlane).resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> childNbt.put("orbital_plane", n));
			OrbitalShape.CODEC.encodeStart(NbtOps.INSTANCE, child.orbitalShape).resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> childNbt.put("orbital_shape", n));

			children.add(childNbt);
		}
		nbt.put("children", children);

		return nbt;
	}

	// we only consider two types of orbits:
	// 1. When bodies have masses of similar orders of magnitude, their
	// gravitational influence is strong enough on each other that they can be
	// thought of as co-orbiting each other, with one focus of each body pinned at
	// the barycenter of the two bodies, which is determined by the masses of each
	// body. The eccentricity of each orbital shape is shared.
	// 2. When bodies have masses of different orders of magnitude, one body's
	// influence is large enough that we can treat the bodies orbiting it as giving
	// negligible gravitational influence, and have one of their orbit foci directly
	// at the position of the parent body.

}
