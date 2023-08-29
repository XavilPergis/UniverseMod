package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.function.BiConsumer;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.ultraviolet.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.CelestialRing;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;
import net.xavil.hawklib.math.Formulas;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;

public class Planetesimal {

	public final AccreteContext ctx;
	private int id;
	private double mass;
	private OrbitalShape orbitalShape;
	private Planetesimal moonOf = null;
	private double inclination;
	private double rotationalRate;
	private final MutableList<Planetesimal> moons = new Vector<>();
	private final MutableList<Ring> rings = new Vector<>();
	public boolean sweptGas = false;

	public record Ring(Interval interval, double mass, double eccentricity) {
	}

	private Planetesimal(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
	}

	private Planetesimal(AccreteContext ctx, double semiMajor, double inclination, Interval bounds) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
		// var semiMajor = ctx.rng.uniformDouble(bounds.lower(), bounds.higher());
		var eccentricity = randomEccentricity(ctx);
		Assert.isTrue(eccentricity >= 0 && eccentricity <= 1);
		this.orbitalShape = new OrbitalShape(eccentricity, semiMajor);
		this.mass = ctx.params.initialPlanetesimalMass;
		this.inclination = inclination;
		this.rotationalRate = ctx.rng.uniformDouble(0, 0.000872664626);
	}

	public double hillSphereRadius(double parentMass) {
		return this.orbitalShape.semiMajor() * (1 - this.orbitalShape.eccentricity())
				* Math.cbrt(this.mass / (3 * parentMass));
	}

	public static Planetesimal random(AccreteContext ctx, double semiMajor, double inclination, Interval bounds) {
		return new Planetesimal(ctx, semiMajor, inclination, bounds);
	}

	public static Planetesimal defaulted(AccreteContext ctx) {
		return new Planetesimal(ctx);
	}

	public int getId() {
		return id;
	}

	public double getMass() {
		return mass;
	}

	public Planetesimal getParentBody() {
		return this.moonOf;
	}

	public void setMass(double newMass) {
		Assert.isTrue(newMass >= 0.0);
		this.mass = newMass;
	}

	public OrbitalShape getOrbitalShape() {
		return orbitalShape;
	}

	public void setOrbitalShape(OrbitalShape newOrbitalShape) {
		this.orbitalShape = newOrbitalShape;
		Assert.isTrue(newOrbitalShape.eccentricity() >= 0 && newOrbitalShape.eccentricity() <= 1);
	}

	public Iterable<Planetesimal> getMoons() {
		return this.moons.iterable();
	}

	public void addMoon(Planetesimal newMoon) {
		this.moons.push(newMoon);
		newMoon.moonOf = this;
	}

	public void clearMoons() {
		this.moons.clear();
	}

	public void transformMoons(BiConsumer<MutableList<Planetesimal>, MutableList<Planetesimal>> consumer) {
		final var prev = MutableList.copyOf(this.moons);
		this.moons.clear();
		consumer.accept(prev, this.moons);

		for (var moon : this.moons.iterable()) {
			moon.moonOf = this;
		}
	}

	public Iterable<Ring> getRings() {
		return this.rings.iterable();
	}

	public void addRing(Ring newRing) {
		this.rings.push(newRing);
	}

	public static OrbitalShape calculateCombinedOrbitalShape(Planetesimal a, Planetesimal b) {
		// Assert.isReferentiallyEqual(a.moonOf, b.moonOf);
		var combinedMass = a.mass + b.mass;
		var newSemiMajor = combinedMass / (a.mass / a.orbitalShape.semiMajor() + b.mass / b.orbitalShape.semiMajor());
		var ta = a.mass * Math.sqrt(a.orbitalShape.semiMajor() * (1 - Math.pow(a.orbitalShape.eccentricity(), 2)));
		var tb = b.mass * Math.sqrt(b.orbitalShape.semiMajor() * (1 - Math.pow(b.orbitalShape.eccentricity(), 2)));
		var tCombined = (ta + tb) / (combinedMass * Math.sqrt(newSemiMajor));
		var newEccentricity = Math.sqrt(Math.abs(1 - (tCombined * tCombined)));
		newEccentricity = Mth.clamp(newEccentricity, 0, 0.8);
		Assert.isTrue(!Double.isNaN(newEccentricity));
		Assert.isTrue(newEccentricity >= 0 && newEccentricity <= 1);
		return new OrbitalShape(newEccentricity, newSemiMajor);
	}

	public double getRadius() {
		return calculateRadiusKothari(orbitalZone());
	}

	public int orbitalZone() {
		var distanceToStar = distanceToStar();
		if (distanceToStar < 4 * Math.sqrt(ctx.stellarLuminosityLsol))
			return 1;
		if (distanceToStar < 15 * Math.sqrt(ctx.stellarLuminosityLsol))
			return 2;
		return 3;
	}

	public double calculateRadiusKothari(int zone) {
		boolean isGasGiant = canSweepGas();

		double atomicWeight = 0;
		double atomicNum = 0;
		if (zone == 1) {
			atomicWeight = isGasGiant ? 9.5 : 15.0;
			atomicNum = isGasGiant ? 4.5 : 8.0;
		} else if (zone == 2) {
			atomicWeight = isGasGiant ? 2.47 : 10.0;
			atomicNum = isGasGiant ? 2.0 : 5.0;
		} else if (zone == 3) {
			atomicWeight = isGasGiant ? 7.0 : 10.0;
			atomicNum = isGasGiant ? 4.0 : 5.0;
		}

		var mass = this.mass;

		var numeratorP1 = (2.0 * ctx.params.BETA_20) / ctx.params.A1_20;
		var numeratorP2 = 1 / Math.pow(atomicWeight * atomicNum, 1.0 / 3.0);
		var numerator = numeratorP1 * numeratorP2 * Math.pow(ctx.params.SOLAR_MASS_IN_GRAMS, 1.0 / 3.0);

		var denominatorP1 = ctx.params.A2_20 / ctx.params.A1_20;
		var denominatorP2 = Math.pow(atomicWeight, 4.0 / 3.0) / Math.pow(atomicNum, 2.0);
		var denominator = 1.0 +
				denominatorP1 *
						denominatorP2 *
						Math.pow(ctx.params.SOLAR_MASS_IN_GRAMS, 2.0 / 3.0) *
						Math.pow(mass, 2.0 / 3.0);

		var res = Units.ku_PER_cu * (numerator / denominator) * Math.pow(mass, 1.0 / 3.0);

		return res;

		// A - atomicWeight
		// Z - atomicNum
		// double numerator = 2.0 * ctx.params.BETA_20 * ctx.params.A1_20 * atomicNum *
		// Math.cbrt(planetesimal.mass);
		// double denominator1 = ctx.params.A1_20 * ctx.params.A2_20 * atomicNum *
		// atomicNum *
		// Math.cbrt(atomicWeight);
		// double denominator2 = ctx.params.A2_20 * ctx.params.A2_20 *
		// Math.pow(atomicWeight, 5.0 / 3.0)
		// * Math.pow(planetesimal.mass, 2.0 / 3.0);

		// double radiusCm = numerator / (denominator1 + denominator2);
		// return radiusCm * (Units.CENTI / Units.KILO);
		// return radiusCm * Units.m_PER_Rsol / 1000;
	}

	public static double randomEccentricity(AccreteContext ctx) {
		return 1 - Math.pow(1 - ctx.rng.uniformDouble(), ctx.params.eccentricityCoefficient);
	}

	public static double reducedMass(double mass) {
		return Math.pow(mass / (1 + mass), 0.25);
	}

	public double criticalMass() {
		var temperature = this.orbitalShape.periapsisDistance() * Math.sqrt(this.ctx.stellarLuminosityLsol);
		return this.ctx.params.b * Math.pow(temperature, -0.75);
	}

	public boolean canSweepGas() {
		return this.mass > criticalMass();
	}

	public double distanceToStar() {
		Assert.isReferentiallyNotEqual(this.moonOf, this);

		var cur = this;
		for (var i = 0; i < 10; ++i) {
			if (cur.moonOf == null)
				return cur.orbitalShape.semiMajor();
			cur = cur.moonOf;
		}

		Mod.LOGGER.error("star distance lookup failed");
		return Double.NaN;
	}

	private double timeUntilTidallyLockedMyr(PlanetaryCelestialNode node, double parentMassYg, double semiMajorTm) {
		if (Double.isNaN(node.type.rigidity))
			return Double.NaN;

		final var meanRadiusM = Units.u_PER_ku * node.radius;
		final var denom = 1e9 * node.massYg * Math.pow(1e9 * parentMassYg, 2);
		final var lockingTime = 6 * Math.pow(semiMajorTm * 1e12, 6) * meanRadiusM * node.type.rigidity / denom;
		return lockingTime / 1e4;
	}

	private CelestialNode createCelestialNode() {
		final var massYg = this.mass * Units.Yg_PER_Msol;

		if (this.sweptGas) {
			if (massYg >= 0.08 * Units.Yg_PER_Msol) {
				return StellarCelestialNode.fromMassAndAge(massYg, this.ctx.systemAgeMya);
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

	public void convertToPlanetNode(CelestialNode parent) {
		if (this.mass < 3.0 * this.ctx.params.initialPlanetesimalMass)
			return;

		final var node = createCelestialNode();

		node.massYg = this.mass * Units.Yg_PER_Msol;
		final var shape = new OrbitalShape(this.orbitalShape.eccentricity(),
				this.orbitalShape.semiMajor() * Units.Tm_PER_au);
		final var plane = OrbitalPlane.fromInclination(this.inclination, this.ctx.rng);

		if (node instanceof UnaryCelestialNode unaryNode) {
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
			// TODO: try to apply tidal locking to the parent node too
			final var lockingTime = timeUntilTidallyLockedMyr(planetNode, parent.massYg, shape.semiMajor());
			final var lockingPercent = Math.min(1.0, this.ctx.systemAgeMya / lockingTime);
			final var orbitalRate = 2.0 * Math.PI / Formulas.orbitalPeriod(shape.semiMajor(), planetNode.massYg);
			planetNode.rotationalRate = Mth.lerp(lockingPercent, this.rotationalRate, orbitalRate);
		}

		// this random offset distributes the celestial bodies randomly around their
		// orbits, so that they don't all form a straight line.
		final var offset = this.ctx.rng.uniformDouble(0, 2.0 * Math.PI);
		final var orbit = new CelestialNodeChild<>(parent, node, shape, plane, offset);
		parent.insertChild(orbit);

		for (final var moon : this.moons.iterable()) {
			moon.convertToPlanetNode(node);
		}
	}

	public Interval effectLimits() {
		final var m = reducedMass(mass);
		final var inner = this.orbitalShape.periapsisDistance() * (1 - m);
		final var outer = this.orbitalShape.apoapsisDistance() * (1 + m);
		return new Interval(inner, outer);
	}

	public Interval sweptDustLimits() {
		final var effectLimits = effectLimits();
		final var inner = effectLimits.lower() / (1 + ctx.params.cloudEccentricity);
		final var outer = effectLimits.higher() / (1 - ctx.params.cloudEccentricity);
		return new Interval(Math.max(0, inner), outer);
	}

	public static void convertToRing(Planetesimal parent, Planetesimal child) {
		final var parentRadiusAu = parent.getRadius() / Units.km_PER_au;
		final var radiusAu = child.getRadius() / Units.km_PER_au;

		final var ringOuter = child.orbitalShape.semiMajor() + 4.0 * radiusAu;
		final var idealRingInner = child.orbitalShape.semiMajor() - 4.0 * radiusAu;
		final var ringInner = Math.max(idealRingInner, 1.5 * parentRadiusAu);

		if (ringInner >= ringOuter) {
			// i eated the whole ring
			parent.setMass(parent.mass + child.mass);
			return;
		}

		final var idealArea = Math.PI * (Mth.square(ringOuter) - Mth.square(idealRingInner));
		final var actualArea = Math.PI * (Mth.square(ringOuter) - Mth.square(ringInner));

		final var percentLoss = 1.0 - (actualArea / idealArea);
		
		parent.setMass(parent.mass + child.mass * percentLoss);
		final var ringMass = child.mass * (1.0 - percentLoss);
		parent.rings.push(new Ring(new Interval(ringInner, ringOuter), ringMass, 0.0));

		// FIXME: update rings when the radius changes
		// we could probably do all the ring handling stuff right at the end tbh
		
		// TODO: eccentricity should probably factor in somehow, but generally, rings
		// are extremely circular
	}

}
