package net.xavil.ultraviolet.common.universe.system.realistic_generator;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.ultraviolet.common.universe.system.BinaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.CelestialNodeChild;
import net.xavil.ultraviolet.common.universe.system.CelestialRing;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;

public class Planetesimal implements IntoIterator<Planetesimal> {

	public final AccreteContext ctx;
	public final StableRandom rng;
	public final int id;
	public double age;

	public boolean isBinary = false;
	public Planetesimal binaryA, binaryB;
	public Planetesimal satteliteOf = null;
	public final MutableList<Planetesimal> sattelites = new Vector<>();

	public StellarCelestialNode.Properties stellarProperties = new StellarCelestialNode.Properties();

	public double mass;
	public OrbitalShape orbitalShape = OrbitalShape.ZERO;
	public double inclination;
	public double rotationalRate;
	public final MutableList<Ring> rings = new Vector<>();
	public boolean sweptGas = false;
	public boolean shouldContinueSweeping = true;

	public record Ring(Interval interval, double mass, double eccentricity) {
	}

	public Planetesimal(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
		this.rng = ctx.rng.split(this.id);
		this.age = ctx.systemAgeMya - ctx.currentSystemAgeMya;
	}

	private Planetesimal(AccreteContext ctx, double semiMajor, double inclination) {
		this(ctx);
		final var eccentricity = randomEccentricity(ctx, this.rng);
		Assert.isTrue(eccentricity >= 0 && eccentricity <= 1);
		this.orbitalShape = new OrbitalShape(eccentricity, semiMajor);
		this.mass = ctx.params.initialPlanetesimalMass;
		this.inclination = inclination;
		this.rotationalRate = ctx.rng.uniformDouble("rotational_rate", 0, 0.000872664626);
	}

	public double hillSphereRadius(double parentMass) {
		return this.orbitalShape.semiMajor() * (1 - this.orbitalShape.eccentricity())
				* Math.cbrt(this.mass / (3 * parentMass));
	}

	public static Planetesimal random(AccreteContext ctx, double semiMajor, double inclination) {
		return new Planetesimal(ctx, semiMajor, inclination);
	}

	public void transformMoons(BiConsumer<MutableList<Planetesimal>, MutableList<Planetesimal>> consumer) {
		final var prev = new Vector<>(this.sattelites);
		this.sattelites.clear();
		consumer.accept(prev, this.sattelites);

		for (final var sattelite : this.sattelites.iterable()) {
			sattelite.satteliteOf = this;
		}
	}

	public void removeSattelite(Planetesimal sattelite) {
		final var i = this.sattelites.indexOf(sattelite);
		Assert.isNotEqual(i, -1);
		this.sattelites.remove(i);
		sattelite.satteliteOf = null;
	}

	public void addSattelite(Planetesimal sattelite) {
		if (sattelite.satteliteOf == this)
			return;
		if (sattelite.satteliteOf != null)
			sattelite.satteliteOf.removeSattelite(sattelite);
		sattelite.satteliteOf = this;
		this.sattelites.push(sattelite);
	}

	public Iterable<Ring> getRings() {
		return this.rings.iterable();
	}

	public void addRing(Ring newRing) {
		this.rings.push(newRing);
	}

	public static OrbitalShape calculateCombinedOrbitalShape(Planetesimal a, Planetesimal b) {
		// Assert.isReferentiallyEqual(a.moonOf, b.moonOf);
		final var combinedMass = a.mass + b.mass;
		final var newSemiMajor = combinedMass
				/ (a.mass / a.orbitalShape.semiMajor() + b.mass / b.orbitalShape.semiMajor());
		final var ta = a.mass
				* Math.sqrt(a.orbitalShape.semiMajor() * (1 - Math.pow(a.orbitalShape.eccentricity(), 2)));
		final var tb = b.mass
				* Math.sqrt(b.orbitalShape.semiMajor() * (1 - Math.pow(b.orbitalShape.eccentricity(), 2)));
		final var tCombined = (ta + tb) / (combinedMass * Math.sqrt(newSemiMajor));
		var newEccentricity = Math.sqrt(Math.abs(1 - (tCombined * tCombined)));
		newEccentricity = Mth.clamp(newEccentricity, 0, 0.8);
		Assert.isTrue(!Double.isNaN(newEccentricity));
		Assert.isTrue(newEccentricity >= 0 && newEccentricity <= 1);
		return new OrbitalShape(newEccentricity, newSemiMajor);
	}

	public double getRadius() {
		if (this.isBinary) {
			final var ra = Units.km_PER_au * this.binaryA.orbitalShape.periapsisDistance() + this.binaryA.getRadius();
			final var rb = Units.km_PER_au * this.binaryB.orbitalShape.periapsisDistance() + this.binaryB.getRadius();
			return Math.max(ra, rb);
		}

		final var massYg = this.mass * Units.Yg_PER_Msol;
		if (this.mass >= 0.08) {
			// final var stellarProperties = new StellarCelestialNode.Properties();
			this.stellarProperties.load(new SplittableRng(this.rng.seed), massYg,
					this.ctx.systemAgeMya - this.ctx.currentSystemAgeMya);
			return Units.km_PER_Rsol * this.stellarProperties.radiusRsol;
		}
		final var mass_Mearth = Units.Mearth_PER_Yg * massYg;
		if (mass_Mearth < 1)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.3);
		if (mass_Mearth < 200)
			return Units.km_PER_Rearth * Math.pow(mass_Mearth, 0.5);
		return Units.km_PER_Rearth * 22.6 * Math.pow(mass_Mearth, -0.0886);
	}

	public static double randomEccentricity(AccreteContext ctx, StableRandom rng) {
		return 1 - Math.pow(1 - rng.uniformDouble("eccentricity"), ctx.params.eccentricityCoefficient);
	}

	public static double reducedMass(double mass) {
		return Math.pow(mass / (1 + mass), 0.25);
	}

	public double criticalMass() {
		var temperature = this.orbitalShape.periapsisDistance() * Math.sqrt(this.ctx.stellarLuminosityLsol);
		return this.ctx.params.b * Math.pow(temperature, -0.75);
	}

	public boolean canSweepGas() {
		return this.mass >= criticalMass();
	}

	private double timeUntilTidallyLockedMyr(PlanetaryCelestialNode node, double parentMassYg, double semiMajorTm) {
		// FIXME: this is bad lmao
		if (Double.isNaN(node.type.rigidity))
			return Double.POSITIVE_INFINITY;

		final var meanRadiusM = Units.u_PER_ku * node.radius;
		final var denom = 1e9 * node.massYg * Math.pow(1e9 * parentMassYg, 2);
		final var lockingTime = 6 * Math.pow(semiMajorTm * 1e12, 6) * meanRadiusM * node.type.rigidity / denom;
		return lockingTime / 1e4;
	}

	private CelestialNode createCelestialNode() {
		final var massYg = this.mass * Units.Yg_PER_Msol;

		if (this.isBinary) {
			final var bnode = new BinaryCelestialNode();
			bnode.massYg = 0;
			bnode.massYg += this.binaryA.mass * Units.Yg_PER_Msol;
			bnode.massYg += this.binaryB.mass * Units.Yg_PER_Msol;
			return bnode;
		}
		if (this.sweptGas) {
			if (massYg >= 0.08 * Units.Yg_PER_Msol) {
				return StellarCelestialNode.fromMassAndAge(new SplittableRng(this.rng.seed), massYg,
						this.ctx.systemAgeMya);
			} else if (massYg >= 13 * Units.Yg_PER_Mjupiter) {
				return new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.BROWN_DWARF, massYg);
			} else {
				return new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.GAS_GIANT, massYg);
			}
		} else {
			// TODO: different world types
			return new PlanetaryCelestialNode(PlanetaryCelestialNode.Type.ROCKY_WORLD, massYg);
		}
	}

	@Nullable
	public CelestialNode convertToCelestialNode() {
		if (this.mass < 3.0 * this.ctx.params.initialPlanetesimalMass)
			return null;

		CelestialNode node = createCelestialNode();

		if (node instanceof BinaryCelestialNode binaryNode) {
			final var a = this.binaryA.convertToCelestialNode();
			final var b = this.binaryB.convertToCelestialNode();
			if (a == null) {
				node = b;
			} else if (b == null) {
				node = a;
			} else {
				final var outer = this.binaryA.mass > this.binaryB.mass ? this.binaryB : this.binaryA;
				binaryNode.setSiblings(a, b);
				binaryNode.massYg = a.massYg + b.massYg;

				final var shape = new OrbitalShape(outer.orbitalShape.eccentricity(),
						outer.orbitalShape.semiMajor() * Units.Tm_PER_au);
				final var plane = OrbitalPlane.fromInclination(outer.inclination, outer.rng.split("orbital_plane"));

				binaryNode.setOrbitalShapes(shape);
				binaryNode.orbitalPlane = plane;
			}
		}
		if (node instanceof UnaryCelestialNode unaryNode) {
			node.massYg = this.mass * Units.Yg_PER_Msol;
			unaryNode.rotationalRate = this.rotationalRate;
			unaryNode.obliquityAngle = 0;
			unaryNode.radius = this.getRadius();

			for (final var ring : this.rings.iterable()) {
				final var intervalTm = ring.interval.mul(Units.Tm_PER_au);
				final var ringMassYg = ring.mass * Units.Yg_PER_Msol;
				unaryNode.rings.push(new CelestialRing(OrbitalPlane.ZERO, ring.eccentricity, intervalTm, ringMassYg));
			}
		}
		if (node instanceof StellarCelestialNode starNode) {
		}
		if (node instanceof PlanetaryCelestialNode planetNode) {
			// if (this.satteliteOf != null) {
			// final var parentMass = this.satteliteOf.mass * Units.Yg_PER_Msol;
			// // TODO: try to apply tidal locking to the parent node too
			// final var lockingTime = timeUntilTidallyLockedMyr(planetNode, parentMass,
			// shape.semiMajor());
			// final var lockingPercent = Math.min(1.0, this.ctx.systemAgeMya /
			// lockingTime);
			// final var orbitalRate = 2.0 * Math.PI /
			// Formulas.orbitalPeriod(shape.semiMajor(), planetNode.massYg);
			// planetNode.rotationalRate = Mth.lerp(lockingPercent, this.rotationalRate,
			// orbitalRate);
			// }
		}

		for (final var sattelite : this.sattelites.iterable()) {
			final var childNode = sattelite.convertToCelestialNode();
			if (childNode == null)
				continue;

			final var shape = new OrbitalShape(sattelite.orbitalShape.eccentricity(),
					sattelite.orbitalShape.semiMajor() * Units.Tm_PER_au);
			final var plane = OrbitalPlane.fromInclination(sattelite.inclination, sattelite.rng.split("orbital_plane"));

			// this random offset distributes the celestial bodies randomly around their
			// orbits, so that they don't all form a straight line.
			final var phase = this.rng.uniformDouble("orbital_phase", 0, 2.0 * Math.PI);
			final var orbit = new CelestialNodeChild<>(node, childNode, shape, plane, phase);
			node.insertChild(orbit);
		}

		return node;
	}

	// // W^1 m^-2
	// public static double energyFromStar(double starRadiusM, double
	// starTemperature, double objectDistanceM) {
	// final double totalSolarEmission = Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4
	// * Math.pow(starTemperature, 4.0)
	// * Math.pow(starRadiusM, 2.0);
	// return totalSolarEmission / Math.pow(objectDistanceM, 2.0);
	// }

	// public double calculateSurfaceTemperature() {
	// if (this.stellarProperties.type != null) {
	// return this.stellarProperties.temperatureK;
	// }

	// Planetesimal root = this;
	// while (root.satteliteOf != null) {
	// root = root.satteliteOf;
	// }

	// final var stars = root.iter().filter(node -> node.stellarProperties.type !=
	// null).collectTo(Vector::new);
	// for (final var star : stars.iterable()) {
	// energyFromStar(star.stellarProperties.radiusRsol,
	// star.stellarProperties.temperatureK, distance);
	// }
	// }

	public Interval effectLimits() {
		final var m = reducedMass(mass);
		final var inner = this.orbitalShape.periapsisDistance() * (1 - m);
		final var outer = this.orbitalShape.apoapsisDistance() * (1 + m);
		return new Interval(inner, outer);
	}

	public Interval sweptDustLimits() {
		return sweptDustLimits(this.orbitalShape.periapsisDistance(), this.orbitalShape.apoapsisDistance());
	}

	public Interval sweptDustLimits(double periapsis, double apoapsis) {
		final var m = reducedMass(mass);
		final var inner = periapsis * (1 - m) / (1 + ctx.params.cloudEccentricity);
		final var outer = apoapsis * (1 + m) / (1 - ctx.params.cloudEccentricity);
		return new Interval(Math.max(0, inner), outer);
	}

	public static void convertToRing(Planetesimal parent, Planetesimal child) {
		final var parentRadiusAu = parent.getRadius() / Units.km_PER_au;
		final var radiusAu = child.getRadius() / Units.km_PER_au;

		final var ringOuter = child.orbitalShape.periapsisDistance() + 4.0 * radiusAu;
		final var idealRingInner = child.orbitalShape.apoapsisDistance() - 4.0 * radiusAu;
		final var ringInner = Math.max(idealRingInner, 1.5 * parentRadiusAu);

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
		// are extremely circular
	}

	@Override
	public Iterator<Planetesimal> iter() {
		final var stack = Vector.fromElements(this);
		return new Iterator<Planetesimal>() {
			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public Planetesimal next() {
				final var node = stack.get(stack.size() - 1);
				stack.extend(node.sattelites);
				if (node.isBinary) {
					stack.push(node.binaryA);
					stack.push(node.binaryB);
				}
				return node;
			}
		};
	}

}
