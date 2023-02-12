package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;

public abstract sealed class StarSystemNode permits StarNode, BinaryNode, PlanetNode, OtherNode {

	public static final int UNASSINED_ID = -1;

	public static class UnaryOrbit {
		public StarSystemNode node;
		public boolean isPrograde; // FIXME: retrograde orbits should be expressed by flipping the orbital plane.
		public OrbitalShape orbitalShape;
		public OrbitalPlane orbitalPlane;

		public UnaryOrbit(StarSystemNode node, boolean isPrograde, OrbitalShape orbitalShape,
				OrbitalPlane orbitalPlane) {
			this.node = node;
			this.isPrograde = isPrograde;
			this.orbitalShape = orbitalShape;
			this.orbitalPlane = orbitalPlane;
		}

	}

	protected int id = UNASSINED_ID;
	protected @Nullable StarSystemNode parentUnaryNode = null;
	protected @Nullable BinaryNode parentBinaryNode = null;
	protected final List<UnaryOrbit> childNodes = new ArrayList<>();

	public double massYg; // Yg
	// TODO: these quantities are meaningless for binary orbits!
	public double obliquityAngle; // rad
	public double rotationalSpeed; // rad/s

	public StarSystemNode(double massYg) {
		this.massYg = massYg;
	}

	/**
	 * This method assumes it is being called on the root node.
	 */
	public final void assignIds() {
		assignSubtreeIds(0);
	}

	/**
	 * This method may be called on any subtree.
	 */
	public final @Nullable StarSystemNode lookup(int id) {
		if (id < 0) {
			Mod.LOGGER.error("Attempted to get celestial node with invalid id of " + id);
			return null;
		}
		return lookupSubtree(id);
	}

	public final int getId() {
		return this.id;
	}

	/**
	 * Assigns IDs to each celestial node such that each node's ID is greater than
	 * is descendants, but less than its siblings and ancestors. This allows
	 * {@link StarSystemNode#lookup(int)} to not search the entire tree for a node
	 * on each lookup.
	 * 
	 * @param startId The ID at which all descendant nodes should be greater than.
	 * @return The maximum ID contained within `this`, including itself.
	 */
	private final int assignSubtreeIds(int startId) {
		if (this instanceof BinaryNode binaryNode) {
			startId = binaryNode.getA().assignSubtreeIds(startId) + 1;
			startId = binaryNode.getB().assignSubtreeIds(startId) + 1;
		}
		for (var child : this.childNodes) {
			startId = child.node.assignSubtreeIds(startId) + 1;
		}

		this.id = startId;
		return startId;
	}

	private final StarSystemNode lookupSubtree(int id) {
		assert this.id != UNASSINED_ID;

		if (this.id == id)
			return this;

		// each node's ID is strictly higher than each child node's ID, meaning we can
		// cut out searching a branch entirely if the ID we're looking for is higher
		// than the maximum ID in a branch.
		if (id > this.id)
			return null;

		if (this instanceof BinaryNode binaryNode) {
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

	public void visit(Consumer<StarSystemNode> consumer) {
		consumer.accept(this);
		if (this instanceof BinaryNode binaryNode) {
			binaryNode.getA().visit(consumer);
			binaryNode.getB().visit(consumer);
		}
		this.childNodes.forEach(child -> child.node.visit(consumer));
	}

	public int find(StarSystemNode node) {
		return find(other -> other == node);
	}

	public int find(Predicate<StarSystemNode> predicate) {
		if (predicate.test(this))
			return this.id;
		if (this instanceof BinaryNode binaryNode) {
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

	public void setBinaryParent(BinaryNode parent) {
		this.parentBinaryNode = parent;
	}

	public BinaryNode getBinaryParent() {
		return this.parentBinaryNode;
	}

	public void setUnaryParent(StarSystemNode parent) {
		this.parentUnaryNode = parent;
	}

	public StarSystemNode getUnaryParent() {
		return this.parentUnaryNode;
	}

	public void insertChild(UnaryOrbit child) {
		this.childNodes.add(child);
	}

	public Iterable<UnaryOrbit> childOrbits() {
		return this.childNodes;
	}


	public static final double G = 1e-11;

	public static double getUnaryAngle(StarSystemNode parent, StarSystemNode.UnaryOrbit orbit, double time) {
		var a = orbit.orbitalShape.semimajorAxisTm() * 1e9;
		// T = 2 * pi * sqrt(a^3 / (G * M))
		var period = 2 * Math.PI * Math.sqrt(a * a * a / (G * 1e21 * parent.massYg));
		return (orbit.isPrograde ? 1 : -1) * 2 * Math.PI * time / period + orbit.orbitalPlane.argumentOfPeriapsisRad();
	}

	public static double getBinaryAngle(BinaryNode node, double time) {
		var a = node.getSemiMajorAxisA() * 1e9 + node.getSemiMajorAxisB() * 1e9;
		// T = 2 * pi * sqrt(a^3 / (G * (M1 + M2)))
		var period = 2 * Math.PI * Math.sqrt(a * a * a / (G * (1e21 * node.getA().massYg + 1e21 * node.getB().massYg)));
		return 2 * Math.PI * time / period + node.orbitalPlane.argumentOfPeriapsisRad();
	}

	public static Vec3 getUnaryOffset(OrbitalPlane referencePlane, StarSystemNode.UnaryOrbit orbit, double angle) {
		var plane = referencePlane.transform(orbit.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		// FIXME: elliptical orbits
		return Vec3.ZERO
				.add(apoapsisDir.scale(Math.cos(angle) * orbit.orbitalShape.semimajorAxisTm()))
				.add(rightDir.scale(Math.sin(angle) * orbit.orbitalShape.semiminorAxisTm()));
	}

	public static Vec3 getBinaryOffsetA(OrbitalPlane referencePlane, BinaryNode node, double angle) {
		var plane = referencePlane.transform(node.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		return apoapsisDir.scale(node.getFocalDistanceA())
				.add(apoapsisDir.scale(Math.cos(angle) * node.getSemiMajorAxisA()))
				.add(rightDir.scale(Math.sin(angle) * node.getSemiMinorAxisA()));
	}

	public static Vec3 getBinaryOffsetB(OrbitalPlane referencePlane, BinaryNode node, double angle) {
		var plane = referencePlane.transform(node.orbitalPlane);
		var apoapsisDir = new Vec3(1, 0, 0).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		var rightDir = new Vec3(0, 0, 1).xRot((float) plane.inclinationRad())
				.yRot((float) plane.longitueOfAscendingNodeRad());
		return apoapsisDir.scale(-node.getFocalDistanceB())
				.add(apoapsisDir.scale(Math.cos(angle) * -node.getSemiMajorAxisB()))
				.add(rightDir.scale(Math.sin(angle) * -node.getSemiMinorAxisB()));
	}

	public static void positionNode(StarSystemNode node, OrbitalPlane referencePlane, double time, float partialTick,
			Vec3 centerPos, BiConsumer<StarSystemNode, Vec3> consumer) {
		if (node instanceof BinaryNode binaryNode) {
			var angle = getBinaryAngle(binaryNode, time);
			var aCenter = centerPos.add(getBinaryOffsetA(referencePlane, binaryNode, angle));
			var bCenter = centerPos.add(getBinaryOffsetB(referencePlane, binaryNode, angle));
			positionNode(binaryNode.getA(), referencePlane, time, partialTick, aCenter, consumer);
			positionNode(binaryNode.getB(), referencePlane, time, partialTick, bCenter, consumer);
		}

		for (var childOrbit : node.childOrbits()) {
			var angle = getUnaryAngle(node, childOrbit, time);
			var center = centerPos.add(getUnaryOffset(referencePlane, childOrbit, angle));
			positionNode(childOrbit.node, referencePlane, time, partialTick, center, consumer);
		}

		consumer.accept(node, centerPos);
	}


	public static StarSystemNode readNbt(CompoundTag nbt) {
		StarSystemNode node = null;

		var id = nbt.getInt("id");
		var massYg = nbt.getDouble("mass");
		var nodeType = nbt.getString("node_type");

		Mod.LOGGER.warn(nbt.getAsString());

		if (nodeType.equals("binary")) {
			var a = readNbt(nbt.getCompound("a"));
			var b = readNbt(nbt.getCompound("b"));
			var orbitalPlane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, nbt.get("orbital_plane"))
					.getOrThrow(false, Mod.LOGGER::error);
			var maxOrbitalRadius = nbt.getDouble("max_orbital_radius");
			var squishFactor = nbt.getDouble("squish_factor");
			node = new BinaryNode(a, b, orbitalPlane, squishFactor, maxOrbitalRadius);
		} else if (nodeType.equals("star")) {
			var type = StarNode.Type.values()[nbt.getInt("type")];
			var luminosity = nbt.getDouble("luminosity");
			var radius = nbt.getDouble("radius");
			var temperature = nbt.getDouble("temperature");
			node = new StarNode(type, massYg, luminosity, radius, temperature);
		} else if (nodeType.equals("planet")) {
			var type = PlanetNode.Type.values()[nbt.getInt("type")];
			node = new PlanetNode(type, massYg);
		}

		node.id = id;

		final var childList = nbt.getList("children", Tag.TAG_COMPOUND);
		for (var i = 0; i < childList.size(); ++i) {
			var childNbt = childList.getCompound(i);

			var childNode = readNbt(childNbt.getCompound("node"));
			var prograde = childNbt.getBoolean("prograde");
			var orbitalPlane = OrbitalPlane.CODEC.parse(NbtOps.INSTANCE, childNbt.get("orbital_plane"))
					.getOrThrow(false, Mod.LOGGER::error);
			var orbitalShape = OrbitalShape.CODEC.parse(NbtOps.INSTANCE, childNbt.get("orbital_shape"))
					.getOrThrow(false, Mod.LOGGER::error);

			var child = new UnaryOrbit(childNode, prograde, orbitalShape, orbitalPlane);
			node.childNodes.add(child);
		}

		return node;
	}

	public static CompoundTag writeNbt(StarSystemNode node) {
		var nbt = new CompoundTag();

		nbt.putInt("id", node.id);
		nbt.putDouble("mass", node.massYg);

		if (node instanceof BinaryNode binaryNode) {
			nbt.putString("node_type", "binary");
			nbt.put("a", writeNbt(binaryNode.getA()));
			nbt.put("b", writeNbt(binaryNode.getB()));
			OrbitalPlane.CODEC.encodeStart(NbtOps.INSTANCE, binaryNode.orbitalPlane)
					.resultOrPartial(Mod.LOGGER::error)
					.ifPresent(n -> nbt.put("orbital_plane", n));
			nbt.putDouble("max_orbital_radius", binaryNode.maxOrbitalRadiusTm);
			nbt.putDouble("squish_factor", binaryNode.squishFactor);
		} else if (node instanceof StarNode starNode) {
			nbt.putString("node_type", "star");
			nbt.putInt("type", starNode.type.ordinal());
			nbt.putDouble("luminosity", starNode.luminosityLsol);
			nbt.putDouble("radius", starNode.radiusRsol);
			nbt.putDouble("temperature", starNode.temperatureK);
		} else if (node instanceof PlanetNode planetNode) {
			nbt.putString("node_type", "planet");
			nbt.putInt("type", planetNode.type.ordinal());
		}

		var children = new ListTag();
		for (var child : node.childNodes) {
			var childNbt = new CompoundTag();

			childNbt.put("node", writeNbt(child.node));
			childNbt.putBoolean("prograde", child.isPrograde);

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
