package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.xavil.universal.Mod;

public abstract sealed class StarSystemNode permits StarNode, BinaryNode, PlanetNode, OtherNode {

	public static final int UNASSINED_ID = -1;

	public record UnaryOrbit(
			boolean isPrograde,
			StarSystemNode node,
			OrbitalShape orbitalShape,
			OrbitalPlane orbitalPlane) {
	}

	protected int id = UNASSINED_ID;
	protected @Nullable BinaryNode parentBinaryNode = null;
	protected @Nullable StarSystemNode parentNode = null;
	protected final List<UnaryOrbit> childNodes = new ArrayList<>();

	public double massYg;

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

	public void setBinaryParent(BinaryNode parent) {
		this.parentBinaryNode = parent;
	}

	public BinaryNode getBinaryParent() {
		return this.parentBinaryNode;
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
