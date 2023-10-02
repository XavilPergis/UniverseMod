package net.xavil.universegen.system;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Rng;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.hawklib.math.matrices.Vec3;

public abstract sealed class CelestialNode implements IntoIterator<CelestialNode> permits
		BinaryCelestialNode, UnaryCelestialNode, OtherCelestialNode {

	public static final int UNASSINED_ID = -1;

	public int id = UNASSINED_ID;
	public long seed = 0;
	public BinaryCelestialNode parentBinaryNode = null;
	public CelestialNodeChild<?> parentUnaryNode = null;
	public final MutableList<CelestialNodeChild<?>> childNodes = new Vector<>();

	public String explicitName;
	public String suffix = "";

	public final Vec3.Mutable position = new Vec3.Mutable(Vec3.ZERO), lastPosition = new Vec3.Mutable(Vec3.ZERO);
	public OrbitalPlane referencePlane = OrbitalPlane.ZERO;
	// the rate of orbital precession
	public double apsidalRate; // rad/s

	public double massYg; // Yg

	public CelestialNode() {
	}

	public CelestialNode(double massYg) {
		this.massYg = massYg;
	}

	/**
	 * This method assumes it is being called on the root node.
	 */
	public final void build() {
		buildSubtree(0);
		assignSuffixes();
	}

	public final void assignSeeds(long seed) {
		final var rng = Rng.fromSeed(seed);
		this.seed = rng.uniformLong();
		if (this instanceof BinaryCelestialNode binaryNode) {
			binaryNode.getInner().assignSeeds(rng.uniformLong());
			binaryNode.getOuter().assignSeeds(rng.uniformLong());
		}
		for (var child : this.childNodes.iterable()) {
			child.node.parentUnaryNode = child;
			child.node.assignSeeds(rng.uniformLong());
		}
	}

	/**
	 * Assigns IDs to each celestial node such that each node's ID is greater than
	 * is descendants, but less than its siblings and ancestors. This allows
	 * {@link CelestialNode#lookup(int)} to not search the entire tree for a node
	 * on each lookup.
	 * 
	 * This will also sort the child nodes and set up backlinks.
	 * 
	 * @param startId The ID at which all descendant nodes should be greater than.
	 * @return The maximum ID contained within `this`, including itself.
	 */
	protected final int buildSubtree(int startId) {
		this.childNodes.sort(Comparator.comparingDouble(child -> child.orbitalShape.semiMajor()));

		if (this instanceof BinaryCelestialNode binaryNode) {
			binaryNode.getInner().parentBinaryNode = binaryNode;
			binaryNode.getOuter().parentBinaryNode = binaryNode;
			startId = binaryNode.getInner().buildSubtree(startId) + 1;
			startId = binaryNode.getOuter().buildSubtree(startId) + 1;
		}
		for (var child : this.childNodes.iterable()) {
			child.node.parentUnaryNode = child;
			startId = child.node.buildSubtree(startId) + 1;
		}

		this.id = startId;
		return startId;
	}

	/**
	 * Turns a number {@code n} into a unique sequence of letters, ordered in the
	 * following way:
	 * <p>
	 * {@code A, B, ..., Z, Aa, Ab, ..., Az, Ba, ..., Zz, Aaa, ...}
	 * </p>
	 * 
	 * @param n The number to convert to a letter sequence.
	 * @param m The "base" of the sequence. A value of 3 will only allow 3 letters
	 *          to be used ({@code A, B, C, Aa, ...}). Should be 26 for all letters
	 *          A-Z.
	 * @return The letter sequence.
	 */
	private static String numberToSeq(int n, int m) {
		var s = "";
		for (int i = n;; i = i / m - 1) {
			int ch = 'a' + i % m;
			ch = i < m ? Character.toUpperCase(ch) : ch;
			s = Character.toString(ch) + s;
			if (i < m)
				break;
		}
		return s;
	}

	protected void assignSuffixes() {
		final var stars = this.iter().filterCast(StellarCelestialNode.class).collectTo(Vector::new);
		stars.sort(Comparator.comparingDouble(snode -> snode.massYg));

		for (int i = 0; i < stars.size(); ++i) {
			final CelestialNode star = stars.get(i);
			final var seq = numberToSeq(i, 26);
			star.assignSuffix(seq, 0);
			CelestialNode cur = star.parentBinaryNode;
			while (cur != null) {
				cur.suffix += seq;
				cur = cur.parentBinaryNode;
			}
		}

		for (final var node : this.iterable()) {
			if (node instanceof BinaryCelestialNode bnode) {
				if (bnode.suffix.length() == 1) {
					bnode.assignSuffix("^" + bnode.suffix, 0);
				} else {
					bnode.assignSuffix(bnode.suffix, 0);
				}
			}
		}
	}

	protected void assignSuffix(String suffix, int depth) {
		this.suffix = suffix;
		for (int i = 0; i < this.childNodes.size(); ++i) {
			final var child = this.childNodes.get(i);
			if (child.node instanceof StellarCelestialNode) {
				child.node.assignSuffix(child.node.suffix, 0);
			} else {
				// alternate numbers and letters
				final var id = (depth & 1) == 0 ? String.valueOf(i + 1) : numberToSeq(i, 26).toLowerCase();
				child.node.assignSuffix(suffix + " " + id, depth + 1);
			}
		}
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
			var a = binaryNode.getInner().lookupSubtree(id);
			if (a != null)
				return a;
			var b = binaryNode.getOuter().lookupSubtree(id);
			if (b != null)
				return b;
		}

		for (var child : this.childNodes.iterable()) {
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
			binaryNode.getInner().visit(consumer);
			binaryNode.getOuter().visit(consumer);
		}
		this.childNodes.forEach(child -> child.node.visit(consumer));
	}

	@Override
	public Iterator<CelestialNode> iter() {
		final var stack = Vector.fromElements(this);
		return new Iterator<CelestialNode>() {
			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public CelestialNode next() {
				final var node = stack.popOrNull();
				for (final var child : node.childNodes.iterable()) {
					stack.push(child.node);
				}
				if (node instanceof BinaryCelestialNode bin) {
					stack.push(bin.getOuter());
					stack.push(bin.getInner());
				}
				return node;
			}
		};
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
			var a = binaryNode.getInner().find(predicate);
			if (a != UNASSINED_ID)
				return a;
			var b = binaryNode.getOuter().find(predicate);
			if (b != UNASSINED_ID)
				return b;
		}
		for (var child : this.childNodes.iterable()) {
			var id = child.node.find(predicate);
			if (id != UNASSINED_ID)
				return id;
		}
		return UNASSINED_ID;
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
		this.childNodes.push(child);
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

	// TODO: find center of mass of the current object and all its satellites and
	// orbit everything around that
	protected void updatePositions(OrbitalPlane referencePlane, double time) {
		this.referencePlane = referencePlane;

		if (this instanceof BinaryCelestialNode binaryNode) {
			final var combinedShape = binaryNode.getCombinedShape();
			final var ellipseA = binaryNode.getEllipseA(referencePlane, time);
			final var ellipseB = binaryNode.getEllipseB(referencePlane, time);
			getOrbitalPosition(binaryNode.getInner().position, ellipseA, combinedShape, false, time);
			getOrbitalPosition(binaryNode.getOuter().position, ellipseB, combinedShape, true, time);
			final var newPlane = binaryNode.orbitalPlane.withReferencePlane(referencePlane);
			binaryNode.getInner().updatePositions(newPlane, time);
			binaryNode.getOuter().updatePositions(newPlane, time);
		}

		for (var childOrbit : this.childNodes.iterable()) {
			final var newPlane = childOrbit.orbitalPlane.withReferencePlane(referencePlane);
			getOrbitalPosition(childOrbit.node.position, newPlane, childOrbit.orbitalShape, false, time);
			childOrbit.node.updatePositions(newPlane, time);
		}
	}

	public Vec3 getPosition(float partialTick) {
		return Vec3.lerp(partialTick, this.lastPosition, this.position);
	}

	public Vec3.Mutable getOrbitalPosition(Vec3.Mutable out, OrbitalPlane plane, OrbitalShape shape, boolean reverse,
			double time) {
		final var ellipse = Ellipse.fromOrbit(this.position, plane, shape, this.apsidalRate * time, reverse);
		return getOrbitalPosition(out, ellipse, shape, reverse, time);
	}

	public Vec3.Mutable getOrbitalPosition(Vec3.Mutable out, Ellipse ellipse, OrbitalShape shape, boolean reverse,
			double time) {
		final var orbitalPeriod = Formulas.orbitalPeriod(shape.semiMajor(), this.massYg);
		final var meanAnomaly = (2 * Math.PI / orbitalPeriod) * time;
		final var trueAnomaly = Formulas.calculateTrueAnomaly(meanAnomaly, shape.eccentricity());
		return ellipse.pointFromTrueAnomaly(out, reverse ? -trueAnomaly : trueAnomaly);
	}

	@Override
	public String toString() {
		var res = "CelestialNode " + this.id;
		if (this.explicitName != null)
			res += "(" + this.explicitName + ")";
		if (!this.suffix.isEmpty())
			res += "(" + this.suffix + ")";
		return res;
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

	private static <T> T getNbt(Codec<T> codec, Tag nbt) {
		return codec.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, Mod.LOGGER::error);
	}

	private static <T> Tag writeNbt(Codec<T> codec, T value) {
		return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, Mod.LOGGER::error);
	}

	public static CelestialNode readNbt(CompoundTag nbt) {
		final var node = createNode(nbt.getString("node_type"));
		if (node == null)
			return null;

		final var id = nbt.getInt("id");
		final var seed = nbt.getLong("seed");
		final var explicitName = nbt.contains("name") ? nbt.getString("name") : null;
		final var mass = nbt.getDouble("mass");
		final var position = new Vec3(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
		final var apsidalRate = nbt.getDouble("apsidal_rate");
		final var referencePlane = getNbt(OrbitalPlane.CODEC, nbt.get("reference_plane"));

		Vec3.set(node.position, position);
		node.referencePlane = referencePlane;
		node.id = id;
		node.seed = seed;
		node.explicitName = explicitName;
		node.massYg = mass;
		node.apsidalRate = apsidalRate;

		if (node instanceof BinaryCelestialNode binaryNode) {
			binaryNode.setInner(readNbt(nbt.getCompound("a")));
			binaryNode.setOuter(readNbt(nbt.getCompound("b")));
			binaryNode.orbitalPlane = getNbt(OrbitalPlane.CODEC, nbt.get("orbital_plane"));
			binaryNode.orbitalShapeInner = getNbt(OrbitalShape.CODEC, nbt.get("orbital_shape_a"));
			binaryNode.orbitalShapeOuter = getNbt(OrbitalShape.CODEC, nbt.get("orbital_shape_b"));
			binaryNode.phase = nbt.getDouble("phase");
		}
		if (node instanceof UnaryCelestialNode unaryNode) {
			unaryNode.obliquityAngle = nbt.getDouble("obliquity");
			unaryNode.rotationalRate = nbt.getDouble("rotational_rate");
			unaryNode.radius = nbt.getDouble("radius");
			unaryNode.temperature = nbt.getDouble("temperature");

			final var rings = nbt.getList("rings", Tag.TAG_COMPOUND);
			for (int i = 0; i < rings.size(); ++i) {
				final var ringNbt = rings.getCompound(i);
				final var plane = getNbt(OrbitalPlane.CODEC, ringNbt.getCompound("orbital_plane"));
				final var eccentricity = ringNbt.getDouble("eccentricity");
				final var ringMass = ringNbt.getDouble("mass");
				final var lower = ringNbt.getDouble("interval_lower");
				final var higher = ringNbt.getDouble("interval_higher");
				unaryNode.rings.push(new CelestialRing(plane, eccentricity, new Interval(lower, higher), ringMass));
			}
		}
		if (node instanceof StellarCelestialNode starNode) {
			starNode.type = StellarCelestialNode.Type.values()[nbt.getInt("type")];
			starNode.luminosityLsol = nbt.getDouble("luminosity");
		}
		if (node instanceof PlanetaryCelestialNode planetNode) {
			planetNode.type = PlanetaryCelestialNode.Type.values()[nbt.getInt("type")];
		}

		final var childList = nbt.getList("children", Tag.TAG_COMPOUND);
		for (var i = 0; i < childList.size(); ++i) {
			final var childNbt = childList.getCompound(i);

			final var childNode = readNbt(childNbt.getCompound("node"));
			final var phase = childNbt.getDouble("phase");
			final var orbitalPlane = getNbt(OrbitalPlane.CODEC, childNbt.get("orbital_plane"));
			final var orbitalShape = getNbt(OrbitalShape.CODEC, childNbt.get("orbital_shape"));

			node.insertChild(new CelestialNodeChild<>(node, childNode, orbitalShape, orbitalPlane, phase));
		}

		node.build();

		return node;
	}

	public static CompoundTag writeNbt(CelestialNode node) {
		final var nbt = new CompoundTag();

		nbt.putInt("id", node.id);
		nbt.putLong("seed", node.seed);
		if (node.explicitName != null)
			nbt.putString("name", node.explicitName);
		nbt.putDouble("mass", node.massYg);
		nbt.putDouble("x", node.position.x);
		nbt.putDouble("y", node.position.y);
		nbt.putDouble("z", node.position.z);
		nbt.putDouble("apsidal_rate", node.apsidalRate);
		nbt.put("reference_plane", writeNbt(OrbitalPlane.CODEC, node.referencePlane));

		if (node instanceof BinaryCelestialNode binaryNode) {
			nbt.putString("node_type", "binary");
			nbt.put("a", writeNbt(binaryNode.getInner()));
			nbt.put("b", writeNbt(binaryNode.getOuter()));
			nbt.put("orbital_plane", writeNbt(OrbitalPlane.CODEC, binaryNode.orbitalPlane));
			nbt.put("orbital_shape_a", writeNbt(OrbitalShape.CODEC, binaryNode.orbitalShapeInner));
			nbt.put("orbital_shape_b", writeNbt(OrbitalShape.CODEC, binaryNode.orbitalShapeOuter));
			nbt.putDouble("phase", binaryNode.phase);
		}
		if (node instanceof UnaryCelestialNode unaryNode) {
			nbt.putDouble("radius", unaryNode.radius);
			nbt.putDouble("obliquity", unaryNode.obliquityAngle);
			nbt.putDouble("rotational_rate", unaryNode.rotationalRate);
			nbt.putDouble("temperature", unaryNode.temperature);

			final var rings = new ListTag();
			for (final var ring : unaryNode.rings.iterable()) {
				final var ringNbt = new CompoundTag();
				ringNbt.put("orbital_plane", writeNbt(OrbitalPlane.CODEC, ring.orbitalPlane));
				ringNbt.putDouble("eccentricity", ring.eccentricity);
				ringNbt.putDouble("mass", ring.mass);
				ringNbt.putDouble("interval_lower", ring.interval.lower);
				ringNbt.putDouble("interval_higher", ring.interval.higher);
				rings.add(ringNbt);
			}
			nbt.put("rings", rings);
		}
		if (node instanceof StellarCelestialNode starNode) {
			nbt.putString("node_type", "star");
			nbt.putInt("type", starNode.type.ordinal());
			nbt.putDouble("luminosity", starNode.luminosityLsol);
		}
		if (node instanceof PlanetaryCelestialNode planetNode) {
			nbt.putString("node_type", "planet");
			nbt.putInt("type", planetNode.type.ordinal());
		}

		final var children = new ListTag();
		for (final var child : node.childNodes.iterable()) {
			final var childNbt = new CompoundTag();

			childNbt.put("node", writeNbt(child.node));
			childNbt.putDouble("phase", child.phase);
			childNbt.put("orbital_plane", writeNbt(OrbitalPlane.CODEC, child.orbitalPlane));
			childNbt.put("orbital_shape", writeNbt(OrbitalShape.CODEC, child.orbitalShape));

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
