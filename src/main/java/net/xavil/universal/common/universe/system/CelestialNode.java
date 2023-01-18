package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.Units;

public abstract sealed class CelestialNode {

	private static final int UNASSINED_ID = -1;

	public final List<UnaryOrbit> childNodes = new ArrayList<>();
	protected int id = UNASSINED_ID;
	public double massYg;

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
	 * This method may be called on any subtree.
	 */
	public final @Nullable CelestialNode lookup(int id) {
		if (id < 0) {
			Mod.LOGGER.error("Attempted to get celestial node with invalid id of " + id);
			return null;
		}
		return lookupSubtree(id);
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
	private final int assignSubtreeIds(int startId) {
		if (this instanceof BinaryNode binaryNode) {
			startId = binaryNode.a.assignSubtreeIds(startId) + 1;
			startId = binaryNode.b.assignSubtreeIds(startId) + 1;
		}
		for (var child : this.childNodes) {
			startId = child.node.assignSubtreeIds(startId) + 1;
		}

		this.id = startId;
		return startId;
	}

	private final CelestialNode lookupSubtree(int id) {
		assert this.id != UNASSINED_ID;

		if (this.id == id)
			return this;

		// each node's ID is strictly higher than each child node's ID, meaning we can
		// cut out searching a branch entirely if the ID we're looking for is higher
		// than the maximum ID in a branch.
		if (id > this.id)
			return null;

		if (this instanceof BinaryNode binaryNode) {
			var a = binaryNode.a.lookupSubtree(id);
			if (a != null)
				return a;
			var b = binaryNode.b.lookupSubtree(id);
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

	// reference plane is the XZ plane, reference direction is +X
	// all angles are in radians
	public record OrbitalPlane(
			// the "tilt" of the orbital plane around the ascending node
			double inclinationRad,
			// the "ascending node" is the axis where the tilted plane and the reference
			// plane intersect
			// the angle of the ascending node, measured from the reference direction
			double longitueOfAscendingNodeRad,
			// the "periapsis" is distance of the closest approach of the orbiting pair
			// the angle at which periapsis occurs, measured from the ascending node
			double argumentOfPeriapsisRad) {
	}

	// defines the shape of the orbital ellipse
	public record OrbitalShape(
			double eccentricity,
			double semimajorAxisTm) {
	}

	public record UnaryOrbit(
			boolean isPrograde,
			CelestialNode node,
			OrbitalShape orbitalShape,
			OrbitalPlane orbitalPlane) {
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

	// The position of this node represents the barycenter of the binary system
	public static final class BinaryNode extends CelestialNode {
		public CelestialNode a;
		public CelestialNode b;

		// binary orbits always share a common orbital plane
		public OrbitalPlane orbitalPlane;

		// each individual orbit in a binary can have its own ellipse shape
		public double eccentricity;
		public double semimajorAxisATm;
		public double semimajorAxisBTm;

		public BinaryNode(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane, double eccentricity,
				double maxDistanceTm) {
			super(a.massYg + b.massYg);

			this.a = a;
			this.b = b;
			this.orbitalPlane = orbitalPlane;
			this.eccentricity = eccentricity;

			this.semimajorAxisATm = maxDistanceTm;
			this.semimajorAxisBTm = maxDistanceTm;
			// this.shapeA = shapeA;
			// this.shapeB = shapeB;
		}

		@Override
		public String toString() {
			var builder = new StringBuilder("BinaryNode " + this.id);
			builder.append(" [");
			builder.append("massYg=" + this.massYg + ", ");
			builder.append("orbitalPlane=" + this.orbitalPlane + ", ");
			builder.append("eccentricity=" + this.eccentricity + ", ");
			builder.append("semimajorAxisATm=" + this.semimajorAxisATm + ", ");
			builder.append("semimajorAxisBTm=" + this.semimajorAxisBTm);
			builder.append("]");
			return builder.toString();
		}
	}

	public static non-sealed class StellarBodyNode extends CelestialNode {

		public static enum Type {
			MAIN_SEQUENCE,
			GIANT,
			WHITE_DWARF,
			NEUTRON_STAR,
			BLACK_HOLE,
		}

		public static enum StarClass {
			O("O"),
			B("B"),
			A("A"),
			F("F"),
			G("G"),
			K("K"),
			M("M");

			public final String name;

			private StarClass(String name) {
				this.name = name;
			}
		}

		public Type type;
		public double luminosityLsol;
		public double radiusRsol;

		public StellarBodyNode(Type type, double massYg, double luminosityLsol, double radiusRsol) {
			super(massYg);
			this.type = type;
			this.luminosityLsol = luminosityLsol;
			this.radiusRsol = radiusRsol;
		}

		public final StarClass starClass() {
			var massMsol = this.massYg / Units.YG_PER_MSOL;
			if (massMsol < 0.45)
				return StarClass.M;
			else if (massMsol < 0.8)
				return StarClass.K;
			else if (massMsol < 0.8)
				return StarClass.K;
			else if (massMsol < 1.04)
				return StarClass.G;
			else if (massMsol < 1.4)
				return StarClass.F;
			else if (massMsol < 2.1)
				return StarClass.A;
			else if (massMsol < 16)
				return StarClass.B;
			else
				return StarClass.O;
		}

		@Override
		public String toString() {
			var builder = new StringBuilder("StellarBodyNode " + this.id);
			builder.append(" [");
			builder.append("massYg=" + this.massYg + ", ");
			builder.append("type=" + this.type + ", ");
			builder.append("luminosityLsol=" + this.luminosityLsol + ", ");
			builder.append("radiusRsol=" + this.radiusRsol + ", ");
			builder.append("]");
			return builder.toString();
		}
	}

	public static non-sealed class PlanetaryBodyNode extends CelestialNode {
		public PlanetaryBodyNode(double massYg) {
			super(massYg);
		}

		@Override
		public String toString() {
			var builder = new StringBuilder("PlanetaryBodyNode " + this.id);
			builder.append(" [");
			builder.append("massYg=" + this.massYg + ", ");
			builder.append("]");
			return builder.toString();
		}
		// planet type (gas giant, icy world, rocky world, earth-like world, etc)
		// mass, surface gravity, atmosphere type, landable
		// asteroid belt/rings? perhaps a single disc object?
	}

	public static non-sealed class OtherNode extends CelestialNode {
		public OtherNode(double massYg) {
			super(massYg);
		}

		@Override
		public String toString() {
			var builder = new StringBuilder("OtherNode " + this.id);
			builder.append(" [");
			builder.append("massYg=" + this.massYg + ", ");
			builder.append("]");
			return builder.toString();
		}
	}

}
