package net.xavil.ultraviolet.common.universe.system.gen;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
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

public abstract sealed class SimulationNode implements IntoIterator<SimulationNode> {

	public int id;
	public final AccreteContext ctx;
	public final MutableList<SimulationNode> moons = new Vector<>();
	public SimulationNode parent = null;

	public double eccentricity;
	public double semiMajor;
	public double inclination;
	public double age;

	public SimulationNode(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = this.ctx.nextPlanetesimalId++;
	}

	public static final class Binary extends SimulationNode {
		public SimulationNode a, b;
		public double eccentricity;
		public double periapsisDistance;

		public Binary(AccreteContext ctx) {
			super(ctx);
		}

		@Override
		public CelestialNode convertToCelestialNode() {
			final var a = this.a.convertToCelestialNode();
			final var b = this.b.convertToCelestialNode();

			if (a == null)
				return b;
			if (b == null)
				return a;

			final var shapeA = new OrbitalShape(this.eccentricity,
					this.periapsisDistance * a.massYg / (a.massYg + b.massYg));
			final var shapeB = new OrbitalShape(this.eccentricity,
					this.periapsisDistance * b.massYg / (a.massYg + b.massYg));

			final var phase = this.ctx.rng.uniformDouble(0, 2.0 * Math.PI);
			return new BinaryCelestialNode(a, b, OrbitalPlane.ZERO, shapeA, shapeB, phase);
		}
	}

	public static final class Planet extends SimulationNode {
		public final MutableList<Ring> rings = new Vector<>();
		// the total mass of the planet, in Msol.
		public double mass;
		// the amount of gas this planet has accreted.
		public double gasMass;
		public double rotationalRate;

		private static final double Mearth_PER_Msol = Units.Yg_PER_Msol / Units.Yg_PER_Mearth;
		private static final double kg_PER_Msol = Units.Yg_PER_Msol * (Units.u_PER_Yu * Units.ku_PER_u);
		private static final double km_PER_Rearth = Units.m_PER_Rearth * Units.ku_PER_u;
		private static final double kg_PER_Yg = Units.u_PER_Yu * Units.ku_PER_u;

		public Planet(AccreteContext ctx) {
			super(ctx);
		}

		// https://web.archive.org/web/20211018070553/http://phl.upr.edu/library/notes/standardmass-radiusrelationforexoplanets
		// this is a crude approximation that doesnt take into account the composition
		// of the planet -- information which we can generate!
		/**
		 * @return Radius in km
		 */
		public double radius() {
			final var mass_Mearth = this.mass / Units.Yg_PER_Mearth;
			if (mass_Mearth < 1)
				return km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
			if (mass_Mearth < 200)
				return km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
			return km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
		}

		private PlanetaryCelestialNode.Type type() {
			final var massYg = this.mass * Units.Yg_PER_Msol;
			if (this.gasMass > 0.01 * this.mass) {
				if (massYg >= 0.08 * Units.Yg_PER_Msol) {
					return null;
				} else if (massYg >= 13 * Units.Yg_PER_Mjupiter) {
					return PlanetaryCelestialNode.Type.BROWN_DWARF;
				} else {
					return PlanetaryCelestialNode.Type.GAS_GIANT;
				}
			} else {
				return PlanetaryCelestialNode.Type.ROCKY_WORLD;
			}
		}

		private double rigidity() {
			return PlanetaryCelestialNode.Type.ROCKY_WORLD.rigidity;
		}

		private double timeUntilTidallyLockedMyr(double parentMass, double semiMajor) {
			return Formulas.timeUntilTidallyLocked(this.mass, this.radius(), this.rigidity(), parentMass, semiMajor);
		}

		private void applyTidalLocking() {
			if (this.parent instanceof Binary parent) {
			}
			if (this.parent instanceof Planet parent) {
				final var lockingTime = timeUntilTidallyLockedMyr(parent.mass, this.semiMajor);
				final var lockingPercent = Math.min(1.0, this.age / lockingTime);
				final var orbitalRate = 2.0 * Math.PI / Formulas.orbitalPeriod(this.semiMajor, this.mass);
				this.rotationalRate = Mth.lerp(lockingPercent, this.rotationalRate, orbitalRate);
			}
		}

		@Override
		public CelestialNode convertToCelestialNode() {
			return null;
			// if (this.mass < 3.0 * this.ctx.params.initialPlanetesimalMass)
			// 	return null;

			// final var radiusRearth = radius() / km_PER_Rearth;
			// final var massYg = this.mass * Units.Yg_PER_Msol;
			// final var shape = new OrbitalShape(this.eccentricity, this.semiMajor * Units.Tm_PER_au);
			// final var plane = OrbitalPlane.fromInclination(this.inclination, this.ctx.rng);

			// final CelestialNode node;
			// if (this.gasMass > 0.01 * this.mass) {
			// 	if (massYg >= 0.08 * Units.Yg_PER_Msol) {
			// 		// star track
			// 		node = StellarCelestialNode.fromMassAndAge(massYg, this.ctx.stellarAgeMyr);
			// 	} else if (massYg >= 13 * Units.Yg_PER_Mjupiter) {
			// 		// brown dwarf track
			// 		// TODO
			// 		node = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, massYg, radiusRearth, 300);
			// 		node.rotationalRate = 0; // TODO
			// 	} else {
			// 		// gas giant track
			// 		node = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, massYg, radiusRearth, 300);
			// 		node.rotationalRate = 0; // TODO
			// 	}
			// } else {
			// 	// terrestrial planet track
			// 	// TODO: different world types
			// 	final var pnode = new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, massYg,
			// 			radiusRearth, 300);
			// 	applyTidalLocking(pnode);
			// 	// TODO: try to apply tidal locking to the parent node too
			// 	node = pnode;
			// }

			// node.obliquityAngle = 0;

			// for (final var ring : this.rings.iterable()) {
			// 	// Mod.LOGGER.info("RING!!! {}, parent = {}", this, parent);
			// 	final var intervalTm = ring.interval.mul(Units.Tm_PER_au);
			// 	final var ringMassYg = ring.mass * Units.Yg_PER_Msol;
			// 	node.addRing(new CelestialRing(OrbitalPlane.ZERO, ring.eccentricity, intervalTm, ringMassYg));
			// }

			// // this random offset distributes the celestial bodies randomly around their
			// // orbits, so that they don't all form a straight line.
			// final var phase = this.ctx.rng.uniformDouble(0, 2.0 * Math.PI);
			// final var orbit = new CelestialNodeChild<>(parent, node, shape, plane, phase);
			// parent.insertChild(orbit);

			// for (final var moon : this.moons.iterable()) {
			// 	moon.convertToPlanetNode(node);
			// }
		}
	}

	public static final class Star extends SimulationNode {
		public double mass;
		public double rotationalRate;
		private StellarCelestialNode.Properties props = new StellarCelestialNode.Properties();

		public Star(AccreteContext ctx) {
			super(ctx);
		}

		public double luminosity() {
			this.props.load(this.mass * Units.Yg_PER_Msol, this.ctx.stellarAgeMyr);
			return this.props.luminosityLsol;
		}

		@Override
		public CelestialNode convertToCelestialNode() {
			final var massYg = this.mass * Units.Yg_PER_Msol;
			final var node = StellarCelestialNode.fromMassAndAge(massYg, this.ctx.stellarAgeMyr);
			node.rotationalRate = this.rotationalRate;
			return node;
		}
	}

	public abstract CelestialNode convertToCelestialNode();

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
				stack.extend(node.moons);
				if (node instanceof Binary bin) {
					stack.push(bin.a);
					stack.push(bin.b);
				}
				return node;
			}
		};
	}

}
