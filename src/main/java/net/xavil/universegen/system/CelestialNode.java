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
import net.xavil.util.math.Formulas;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;
import net.xavil.util.math.Vec3;

public abstract sealed class CelestialNode permits
		StellarCelestialNode, BinaryCelestialNode, PlanetaryCelestialNode, OtherCelestialNode {

	public static final int UNASSINED_ID = -1;

	protected int id = UNASSINED_ID;
	protected @Nullable BinaryCelestialNode parentBinaryNode = null;
	protected @Nullable CelestialNodeChild<?> parentUnaryNode = null;
	protected final List<CelestialNodeChild<?>> childNodes = new ArrayList<>();

	public Vec3 position = Vec3.ZERO;
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
		if (id < 0) {
			Mod.LOGGER.error("Attempted to get celestial node with invalid id of " + id);
			return null;
		}
		return lookupSubtree(id);
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

	public List<CelestialNode> selfAndChildren() {
		var nodes = new ArrayList<CelestialNode>();
		visit(nodes::add);
		return nodes;
	}

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
			var combinedSemiMajor = binaryNode.orbitalShapeA.semiMajor() + binaryNode.orbitalShapeB.semiMajor();
			var combinedMass = binaryNode.getA().massYg + binaryNode.getB().massYg;
			var orbitalPeriod = Formulas.orbitalPeriod(combinedSemiMajor, combinedMass);

			var ellipseA = binaryNode.getEllipseA(referencePlane);
			var meanAnomalyA = (2 * Math.PI / orbitalPeriod) * time + binaryNode.offset;
			var trueAnomalyA = Formulas.calculateTrueAnomaly(meanAnomalyA, binaryNode.orbitalShapeA.eccentricity());
			binaryNode.getA().position = ellipseA.pointFromTrueAnomaly(-trueAnomalyA);

			var ellipseB = binaryNode.getEllipseB(referencePlane);
			var meanAnomalyB = (2 * Math.PI / orbitalPeriod) * time + binaryNode.offset;
			var trueAnomalyB = Formulas.calculateTrueAnomaly(meanAnomalyB, binaryNode.orbitalShapeB.eccentricity());
			binaryNode.getB().position = ellipseB.pointFromTrueAnomaly(trueAnomalyB);

			var newPlane = binaryNode.orbitalPlane.withReferencePlane(referencePlane);
			binaryNode.getA().updatePositions(newPlane, time);
			binaryNode.getB().updatePositions(newPlane, time);
		}

		for (var childOrbit : this.childOrbits()) {
			var ellipse = childOrbit.getEllipse(referencePlane);
			var orbitalPeriod = Formulas.orbitalPeriod(childOrbit.orbitalShape.semiMajor(), this.massYg);
			var meanAnomaly = (2 * Math.PI / orbitalPeriod) * time + childOrbit.offset;
			var trueAnomaly = Formulas.calculateTrueAnomaly(meanAnomaly, childOrbit.orbitalShape.eccentricity());
			childOrbit.node.position = ellipse.pointFromTrueAnomaly(trueAnomaly);

			var newPlane = childOrbit.orbitalPlane.withReferencePlane(referencePlane);
			childOrbit.node.updatePositions(newPlane, time);
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
			node.childNodes.add(child);
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

		var children = new ListTag();
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
