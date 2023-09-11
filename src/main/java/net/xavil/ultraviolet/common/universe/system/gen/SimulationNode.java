package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Comparator;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.SortingStrategy;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.CelestialRing;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;

public abstract sealed class SimulationNode implements IntoIterator<SimulationNode> {

	public int id;
	public final AccreteContext ctx;
	public final MutableList<SimulationNode> satellites = new Vector<>();

	public double eccentricity;
	public double semiMajor;
	public double inclination;
	public double age;

	public enum NodeType {
		PLANET_TERRESTRIAL,
		PLANET_GAS_GIANT,
		BROWN_DWARF,
		STAR,
		BINARY,
	}

	public SimulationNode(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = this.ctx.nextPlanetesimalId++;
		this.age = this.ctx.currentSystemAgeMya;
	}

	public static final class Binary extends SimulationNode {
		public SimulationNode a, b;
		public double eccentricity;
		public double periapsisDistance;

		public Binary(AccreteContext ctx) {
			super(ctx);
		}

		@Override
		public CelestialNode convertToCelestialNode(StableRandom rng) {
			final var a = this.a.convertToCelestialNode(rng.split("a"));
			final var b = this.b.convertToCelestialNode(rng.split("b"));

			if (a == null)
				return b;
			if (b == null)
				return a;

			final var bnode = new BinaryCelestialNode();
			bnode.setSiblings(a, b);
			bnode.massYg = bnode.inner.massYg + bnode.outer.massYg;
			bnode.setOrbitalShapes(new OrbitalShape(this.eccentricity, this.periapsisDistance));
			bnode.orbitalPlane = OrbitalPlane.ZERO;
			bnode.phase = rng.uniformDouble("phase", 0, 2.0 * Math.PI);

			return null;
		}

		public SimulationNode larger() {
			return massOf(this.a) > massOf(this.b) ? this.a : this.b;
		}

		public SimulationNode smaller() {
			return massOf(this.a) > massOf(this.b) ? this.b : this.a;
		}

		@Override
		public NodeType getType() {
			return NodeType.BINARY;
		}

		@Override
		public void update(StableRandom rng, ProtoplanetaryDisc disc) {
			this.a.update(rng.split("a"), disc);
			this.b.update(rng.split("b"), disc);
			super.update(rng.split("self"), disc);
		}

		@Override
		public Interval gravitationalInteractionLimit() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double totalMass() {
			return super.totalMass() + this.a.totalMass() + this.b.totalMass();
		}
	}

	public static final class Unary extends SimulationNode {
		public final MutableList<Ring> rings = new Vector<>();
		public double mass; // Yg
		// the amount of gas this node has accreted. Used to determine what kind of
		// planet/star this node is.
		public double gasMass; // Yg
		public double rotationalRate; // rad/s

		private StellarCelestialNode.Properties stellarProperties = new StellarCelestialNode.Properties();

		public Unary(AccreteContext ctx) {
			super(ctx);
		}

		public static Unary star(AccreteContext ctx, double mass, double metallicity) {
			final var node = new Unary(ctx);
			node.mass = mass;
			node.gasMass = mass * (1.0 - metallicity);
			return node;
		}

		@Override
		public NodeType getType() {
			if (this.gasMass > 0.01 * this.mass) {
				if (this.mass >= 0.08 * Units.Yg_PER_Msol) {
					return NodeType.STAR;
				} else if (this.mass >= 13 * Units.Yg_PER_Mjupiter) {
					return NodeType.BROWN_DWARF;
				} else {
					return NodeType.PLANET_GAS_GIANT;
				}
			} else {
				return NodeType.PLANET_TERRESTRIAL;
			}
		}

		// https://web.archive.org/web/20211018070553/http://phl.upr.edu/library/notes/standardmass-radiusrelationforexoplanets
		// this is a crude approximation that doesnt take into account the composition
		// of the planet -- information which we can generate!
		/**
		 * @return Radius in km
		 */
		public double radius() {
			if (getType() == NodeType.STAR) {
				this.stellarProperties.load(this.mass, this.age);
				return Units.km_PER_Rsol * this.stellarProperties.radiusRsol;
			}
			final var mass_Mearth = Units.Mearth_PER_Yg * this.mass;
			if (mass_Mearth < 1)
				return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
			if (mass_Mearth < 200)
				return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
			return Units.km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
		}

		public double luminosity() {
			if (getType() != NodeType.STAR)
				return 0.0;
			this.stellarProperties.load(this.mass, this.age);
			return this.stellarProperties.luminosityLsol;
		}

		// private double rigidity() {
		// return PlanetaryCelestialNode.Type.ROCKY_WORLD.rigidity;
		// }

		// private double timeUntilTidallyLockedMyr(double parentMass, double semiMajor)
		// {
		// return Formulas.timeUntilTidallyLocked(this.mass, this.radius(),
		// this.rigidity(), parentMass, semiMajor);
		// }

		// private void applyTidalLocking() {
		// if (this.parent instanceof Binary parent) {
		// }
		// if (this.parent instanceof Planet parent) {
		// final var lockingTime = timeUntilTidallyLockedMyr(parent.mass,
		// this.semiMajor);
		// final var lockingPercent = Math.min(1.0, this.age / lockingTime);
		// final var orbitalRate = 2.0 * Math.PI /
		// Formulas.orbitalPeriod(this.semiMajor, this.mass);
		// this.rotationalRate = Mth.lerp(lockingPercent, this.rotationalRate,
		// orbitalRate);
		// }
		// }

		private CelestialNode createCelestialNode() {
			return switch (getType()) {
				case PLANET_TERRESTRIAL ->
					new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, this.mass);
				case PLANET_GAS_GIANT -> new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, this.mass);
				case BROWN_DWARF -> new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.BROWN_DWARF, this.mass);
				case STAR -> StellarCelestialNode.fromMassAndAge(this.mass, this.age);
				case BINARY -> null;
			};
		}

		@Override
		public CelestialNode convertToCelestialNode(StableRandom rng) {
			if (this.mass < 3.0 * this.ctx.params.initialPlanetesimalMass)
				return null;

			final var node = createCelestialNode();

			final var shape = new OrbitalShape(this.eccentricity, this.semiMajor);
			final var plane = OrbitalPlane.fromInclination(this.inclination, rng.split("orbital_plane"));

			if (node instanceof UnaryCelestialNode unaryNode) {
				unaryNode.rotationalRate = this.rotationalRate;
				unaryNode.obliquityAngle = 0;
				unaryNode.radius = this.radius();

				for (final var ring : this.rings.iterable()) {
					unaryNode.rings.push(new CelestialRing(
							OrbitalPlane.ZERO, ring.eccentricity, ring.interval, ring.mass));
				}
			}
			if (node instanceof StellarCelestialNode starNode) {
			}
			// if (node instanceof PlanetaryCelestialNode planetNode) {
			// // TODO: try to apply tidal locking to the parent node too
			// final var lockingTime = timeUntilTidallyLockedMyr(planetNode, parent.massYg,
			// shape.semiMajor());
			// final var lockingPercent = Math.min(1.0, this.ctx.systemAgeMya /
			// lockingTime);
			// final var orbitalRate = 2.0 * Math.PI /
			// Formulas.orbitalPeriod(shape.semiMajor(), planetNode.massYg);
			// planetNode.rotationalRate = Mth.lerp(lockingPercent, this.rotationalRate,
			// orbitalRate);
			// }

			// this random offset distributes the celestial bodies randomly around their
			// orbits, so that they don't all form a straight line.
			final var phase = rng.uniformDouble("orbital_phase", 0, 2.0 * Math.PI);

			for (final var satellite : this.satellites.iterable()) {
				final var childNode = satellite.convertToCelestialNode(rng.split(satellite.id));
				node.insertChild(new CelestialNodeChild<>(childNode, node, shape, plane, phase));
			}

			return node;
		}

		@Override
		public void update(StableRandom rng, ProtoplanetaryDisc disc) {
			super.update(rng, disc);
		}

		public static double reducedMass(double mass) {
			return Math.pow(mass / (1 + mass), 0.25);
		}

		public Interval effectLimits() {
			final var m = reducedMass(this.mass);
			final var inner = this.semiMajor * (1 - this.eccentricity) * (1 - m);
			final var outer = this.semiMajor * (1 + this.eccentricity) * (1 + m);
			return new Interval(inner, outer);
		}

		public double criticalMass() {
			// TODO: use node's temperature instead of a derived one...
			final var temperature = this.semiMajor * (1 - this.eccentricity)
					* Math.sqrt(this.ctx.stellarLuminosityLsol);
			return this.ctx.params.b * Math.pow(temperature, -0.75);
		}

		@Override
		public Interval gravitationalInteractionLimit() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@OverridingMethodsMustInvokeSuper
	public void update(StableRandom rng, ProtoplanetaryDisc disc) {
		this.satellites.sort(SortingStrategy.UNSTABLE, Comparator.comparingDouble(node -> node.semiMajor));
		final var prev = new Vector<>(this.satellites);
		this.satellites.clear();
		coalesceSatellites(rng.split("coalesce"), prev, satellites);

		for (final var child : this.satellites.iterable()) {
			child.update(rng.split(child.id), disc);
		}
	}

	public void coalesceSatellites(StableRandom rng, MutableList<SimulationNode> prevBodies,
			MutableList<SimulationNode> nextBodies) {
		for (int j = 0;; ++j) {
			if (j == 0) {
				prevBodies.clear();
				prevBodies.extend(nextBodies);
				nextBodies.clear();
			}

			boolean didCoalesce = false;
			int i = 0;
			while (i < prevBodies.size()) {
				var current = prevBodies.get(i++);
				while (i < prevBodies.size()) {
					final var next = prevBodies.get(i);

					// no intersection with the next planetesimal, just let the outer loop carry on
					// merging from its new position
					final var currentLimits = current.gravitationalInteractionLimit();
					final var nextLimits = next.gravitationalInteractionLimit();
					if (!currentLimits.intersects(nextLimits))
						break;

					i += 1;
					final var coalesced = applyInteraction(rng.split(i), current, next);
					current = coalesced != null ? coalesced : current;
					didCoalesce = true;
				}
				nextBodies.push(current);
			}

			if (!didCoalesce)
				break;

		}

	}

	private SimulationNode collide(StableRandom rng, Unary larger, Unary smaller) {

		larger.mass += smaller.mass;
		larger.gasMass += smaller.gasMass;
		larger.rings.clear();

		if (larger.getType() == NodeType.PLANET_TERRESTRIAL
				&& rng.chance("fragmentation_chance", 0.2)
				&& smaller.mass >= 0.05 * larger.mass) {
			final var moonMassFactor = (smaller.mass / larger.mass)
					* rng.uniformDouble("fragment_mass", 0.1, 0.8);
			final var moonMass = larger.mass * moonMassFactor;
			final var moonGasMass = larger.gasMass * moonMassFactor;

			larger.mass -= moonMass;
			larger.gasMass -= moonGasMass;

			final var maxDistance = 200.0 * Units.Tu_PER_ku * larger.radius();

			final var moon = new Unary(this.ctx);
			moon.semiMajor = maxDistance * rng.uniformDouble("fragment_semi_major");
			moon.mass = moonMass;
			moon.gasMass = moonGasMass;

			final var rocheLimitLarger = Formulas.rocheLimit(larger.mass, moonMass, moon.radius());
			if (moon.semiMajor >= rocheLimitLarger) {
				larger.satellites.push(moon);
			} else {
				convertToRing(larger, moon);
			}
		}

		return larger;
	}

	public SimulationNode capture(StableRandom rng, Unary larger, Unary smaller) {
		// by this point, we know the nodes haven't collided, nor have they ripped each
		// other apart via tidal forces, so we can create stable orbits!

		final var minDistance = Formulas.rocheLimit(larger.mass, smaller.mass, smaller.radius());
		final var maxDistance = Formulas.hillSphereRadius(massOf(this), larger.mass,
				larger.eccentricity, larger.semiMajor);

		final var totalDistance = rng.uniformDouble("distance", minDistance, maxDistance);
		final var largerRadius = larger.radius();

		final var centerOfMass = totalDistance * smaller.mass / (larger.mass + smaller.mass);

		if (Units.ku_PER_Tu * centerOfMass > 2.0 * largerRadius) {
			final var bn = new Binary(this.ctx);
			// bn.updateParent(larger.parent);
			bn.semiMajor = larger.semiMajor;
			bn.eccentricity = larger.eccentricity;
			bn.inclination = larger.inclination;

			bn.a = larger;
			bn.b = smaller;

			larger.semiMajor = centerOfMass;
			smaller.semiMajor = totalDistance - centerOfMass;

			// TODO
			larger.eccentricity = smaller.eccentricity = 0;
			larger.inclination = smaller.inclination = 0;

			return bn;
		} else {
			larger.satellites.push(smaller);
			return larger;
		}
	}

	public SimulationNode applyUnaryInteraction(StableRandom rng, Unary a, Unary b) {
		final var rocheLimitA = Formulas.rocheLimit(a.mass, b.mass, b.radius());
		final var rocheLimitB = Formulas.rocheLimit(b.mass, a.mass, a.radius());
		final var rocheLimit = Math.max(rocheLimitA, rocheLimitB);

		final var larger = a.mass >= b.mass ? a : b;
		final var smaller = a.mass >= b.mass ? b : a;

		// collision
		if (Math.abs(a.semiMajor - b.semiMajor) <= rocheLimit / 2.0) {
			return collide(rng.split("collision"), larger, smaller);
		}
		// ring
		else if (Math.abs(a.semiMajor - b.semiMajor) <= rocheLimit) {
			convertToRing(larger, smaller);
			return larger;
		}
		// capture
		else {
			return capture(rng.split("capture"), larger, smaller);
		}
	}

	public SimulationNode applyBinaryInteraction(Binary a, Binary b) {
		return null;
	}

	public SimulationNode applyThreewayInteraction(Binary bn, Unary un) {

		final var semiMajorBinaryRelative = Math.abs(bn.semiMajor - un.semiMajor);

		// inside stability margin, inside binary orbit: ejection, collsion, capture
		final SimulationNode bl = bn.larger(), bs = bn.smaller();
		// if (semiMajorBinaryRelative < 1.2 * bs.semiMajor) {
		// final var insideLargerOrbit = semiMajorBinaryRelative < bl.semiMajor;
		// final var node = insideLargerOrbit && this.ctx.rng.chance(0.67) ? bl : bs;

		// un.semiMajor = semiMajorBinaryRelative;

		// return applyInteraction(node, un);
		// }

		// outside stability margin: unary/binary orbit with barycenter
		// -> low mass in binary or high mass in unary means larger stability threshold
		// -> maybe check hill radius of all bodies to determine this distance
		// inside stability margin: ejection of anywhere from 1 to all bodies

		return null;
	}

	public SimulationNode applyInteraction(StableRandom rng, SimulationNode a, SimulationNode b) {

		// both are single nodes
		if (a instanceof Unary ua && b instanceof Unary ub) {
			return applyUnaryInteraction(rng, ua, ub);
		}
		// both are binaries
		else if (a instanceof Binary ba && b instanceof Binary bb) {
			return applyBinaryInteraction(ba, bb);
		}
		// else only one one the nodes is a binary
		else if (a instanceof Unary ua && b instanceof Binary bb) {
			return applyThreewayInteraction(bb, ua);
		} else if (a instanceof Binary ba && b instanceof Unary ub) {
			return applyThreewayInteraction(ba, ub);
		}

		return a;
	}

	private static void convertToRing(Unary parent, Unary child) {
		final var parentRadius = parent.radius();
		final var radius = child.radius();

		final var ringOuter = child.semiMajor + 4.0 * radius;
		final var idealRingInner = child.semiMajor - 4.0 * radius;
		final var ringInner = Math.max(idealRingInner, 1.5 * parentRadius);

		if (ringInner >= ringOuter) {
			// i eated the whole ring
			parent.mass += child.mass;
			return;
		}

		final var idealArea = Math.PI * (Mth.square(ringOuter) - Mth.square(idealRingInner));
		final var actualArea = Math.PI * (Mth.square(ringOuter) - Mth.square(ringInner));
		final var percentLoss = 1.0 - (actualArea / idealArea);

		parent.mass += child.mass * percentLoss;
		final var ringMass = child.mass * (1.0 - percentLoss);
		parent.rings.push(new Ring(new Interval(ringInner, ringOuter), ringMass, 0.0));

		// FIXME: update rings when the radius changes
		// we could probably do all the ring handling stuff right at the end tbh

		// TODO: eccentricity should probably factor in somehow, but generally, rings
		// are extremely circular [citation needed]

	}

	private static double massOf(SimulationNode node) {
		if (node instanceof Binary bn) {
			return massOf(bn.a) + massOf(bn.b);
		} else if (node instanceof Unary un) {
			return un.mass;
		}
		return 0.0;
	}

	@OverridingMethodsMustInvokeSuper
	public double totalMass() {
		double total = 0.0;
		for (final var child : this.satellites.iterable()) {
			total += child.totalMass();
		}
		return total;
	}

	public abstract Interval gravitationalInteractionLimit();

	public abstract CelestialNode convertToCelestialNode(StableRandom rng);

	public abstract NodeType getType();

	public record Ring(Interval interval, double mass, double eccentricity) {
	}

	@Override
	public Iterator<SimulationNode> iter() {
		final var stack = Vector.fromElements(this);
		return new Iterator<SimulationNode>() {
			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public SimulationNode next() {
				final var node = stack.get(stack.size() - 1);
				stack.extend(node.satellites);
				if (node instanceof Binary bin) {
					stack.push(bin.a);
					stack.push(bin.b);
				}
				return node;
			}
		};
	}

}
