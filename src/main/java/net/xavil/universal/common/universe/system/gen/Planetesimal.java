package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.List;

import net.xavil.universal.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Interval;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;

public class Planetesimal {

	public final AccreteContext ctx;
	private int id;
	private double mass;
	private OrbitalShape orbitalShape;
	private Planetesimal moonOf = null;
	private final List<Planetesimal> moons = new ArrayList<>();

	private Planetesimal(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
	}

	private Planetesimal(AccreteContext ctx, Interval bounds) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
		var semiMajor = ctx.rng.uniformDouble(bounds.lower(), bounds.higher());
		var eccentricity = randomEccentricity(ctx);
		this.orbitalShape = new OrbitalShape(eccentricity, semiMajor);
		this.mass = ctx.params.initialPlanetesimalMass;
	}

	public double hillSphereRadius(double parentMass) {
		return this.orbitalShape.semiMajor() * (1 - this.orbitalShape.eccentricity())
				* Math.cbrt(this.mass / (3 * parentMass));
	}

	public static Planetesimal random(AccreteContext ctx, Interval bounds) {
		return new Planetesimal(ctx, bounds);
	}

	public static Planetesimal defaulted(AccreteContext ctx) {
		return new Planetesimal(ctx);
	}

	private void emitUpdateEvent() {
		this.ctx.debugConsumer.accept(new AccreteDebugEvent.UpdatePlanetesimal(this));
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
		this.mass = newMass;
		emitUpdateEvent();
	}

	public OrbitalShape getOrbitalShape() {
		return orbitalShape;
	}

	public void setOrbitalShape(OrbitalShape newOrbitalShape) {
		this.orbitalShape = newOrbitalShape;
		emitUpdateEvent();
	}

	public Iterable<Planetesimal> getMoons() {
		return this.moons;
	}

	public void addMoon(Planetesimal newMoon) {
		this.moons.add(newMoon);
		newMoon.moonOf = this;
		this.ctx.debugConsumer.accept(new AccreteDebugEvent.CaptureMoon(this, newMoon));
	}

	public static OrbitalShape calculateCombinedOrbitalShape(Planetesimal a, Planetesimal b) {
		// Assert.isReferentiallyEqual(a.moonOf, b.moonOf);
		var combinedMass = a.mass + b.mass;
		var newSemiMajor = combinedMass / (a.mass / a.orbitalShape.semiMajor() + b.mass / b.orbitalShape.semiMajor());
		var ta = a.mass * Math.sqrt(a.orbitalShape.semiMajor() * (1 - Math.pow(a.orbitalShape.eccentricity(), 2)));
		var tb = b.mass * Math.sqrt(b.orbitalShape.semiMajor() * (1 - Math.pow(b.orbitalShape.eccentricity(), 2)));
		var tCombined = (ta + tb) / (combinedMass * Math.sqrt(newSemiMajor));
		var newEccentricity = Math.sqrt(Math.abs(1 - (tCombined * tCombined)));
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

		var res = ((numerator / denominator) * Math.pow(mass, 1.0 / 3.0)) / (Units.KILO / Units.CENTI);

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
		var cur = this;
		for (var i = 0; i < 10; ++i) {
			if (cur.moonOf == null)
				return cur.orbitalShape.semiMajor();
			cur = cur.moonOf;
		}

		Mod.LOGGER.error("star distance lookup failed");
		return Double.NaN;
	}

	public void convertToPlanetNode(CelestialNode parent) {
		var type = canSweepGas() ? PlanetaryCelestialNode.Type.GAS_GIANT : PlanetaryCelestialNode.Type.ROCKY_WORLD;
		var radiusRearth = this.getRadius() * Units.KILO / Units.m_PER_Rearth;
		var node = new PlanetaryCelestialNode(type, this.mass * Units.Yg_PER_Msol, radiusRearth, 300);
		var shape = new OrbitalShape(this.orbitalShape.eccentricity(), this.orbitalShape.semiMajor() * Units.Tm_PER_au);
		var orbit = new CelestialNodeChild<>(parent, node, shape, OrbitalPlane.ZERO, 0);
		parent.insertChild(orbit);

		for (var moon : this.moons) {
			moon.convertToPlanetNode(node);
		}
	}

	public Interval effectLimits() {
		var m = reducedMass(mass);
		var inner = this.orbitalShape.periapsisDistance() * (1 - m);
		var outer = this.orbitalShape.apoapsisDistance() * (1 + m);
		return new Interval(inner, outer);
	}

	public Interval sweptDustLimits() {
		var effectLimits = effectLimits();
		var inner = effectLimits.lower() / (1 + ctx.params.cloudEccentricity);
		var outer = effectLimits.higher() / (1 - ctx.params.cloudEccentricity);
		return new Interval(Math.max(0, inner), outer);
	}

	public void accreteDust(DustBands dustBands) {
		double prevMass = 0;

		var iterationsRemaining = 100;
		while (this.mass - prevMass >= 0.0001 * this.mass) {
			prevMass = this.mass;
			if (iterationsRemaining-- <= 0)
				break;
			this.mass += dustBands.sweep(ctx, this);
			emitUpdateEvent();
		}

	}

}
