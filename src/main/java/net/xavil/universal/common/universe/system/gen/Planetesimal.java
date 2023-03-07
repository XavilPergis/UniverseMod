package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.List;

import net.xavil.universal.Mod;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;

public class Planetesimal {

	public double mass;
	public OrbitalShape orbitalShape;
	public boolean isGasGiant = false;
	public Planetesimal moonOf = null;
	public final List<Planetesimal> moons = new ArrayList<>();
	public boolean isDwarf = false;
	public double radius;

	private Planetesimal() {
	}

	private Planetesimal(AccreteContext ctx, Interval bounds) {
		var semiMajor = ctx.random.nextDouble(bounds.inner(), bounds.outer());
		var eccentricity = randomEccentricity(ctx);
		this.orbitalShape = new OrbitalShape(eccentricity, semiMajor);
		this.mass = ctx.params.initialPlanetesimalMass;
	}

	// /// Hill sphere radius for planet / moon system in AU.
	// pub fn hill_sphere_au(
	// planet_axis: &f64,
	// planet_eccn: &f64,
	// planet_mass: &f64,
	// stellar_mass: &f64,
	// ) -> f64 {
	// let hill_sphere = planet_axis * (1.0 - planet_eccn) * (planet_mass / (3.0 *
	// stellar_mass)).powf(1.0 / 3.0);
	// float_to_precision(hill_sphere)
	// }

	public double hillSphereRadius(double parentMass) {
		return this.orbitalShape.semiMajor() * (1 - this.orbitalShape.eccentricity())
				* Math.cbrt(this.mass / (3 * parentMass));
	}

	public static Planetesimal random(AccreteContext ctx, Interval bounds) {
		return new Planetesimal(ctx, bounds);
	}

	public static Planetesimal defaulted() {
		return new Planetesimal();
	}

	public static double randomEccentricity(AccreteContext ctx) {
		return 1 - Math.pow(1 - ctx.random.nextDouble(), ctx.params.eccentricityCoefficient);
	}

	public static double reducedMass(double mass) {
		return Math.pow(mass / (1 + mass), 0.25);
	}

	public static double criticalMass(AccreteContext ctx, OrbitalShape shape) {
		var temperature = shape.periapsisDistance() * Math.sqrt(ctx.stellarLuminosityLsol);
		return ctx.params.b * Math.pow(temperature, -0.75);
	}

	public boolean canSweepGas(AccreteContext ctx) {
		return this.mass > criticalMass(ctx, this.orbitalShape);
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
		var type = this.isGasGiant ? PlanetaryCelestialNode.Type.GAS_GIANT : PlanetaryCelestialNode.Type.ROCKY_WORLD;
		var radiusRearth = this.radius * (Units.MEGA / Units.KILO) / Units.Mm_PER_Rearth;
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

	public Interval sweptDustLimits(AccreteContext ctx) {
		var effectLimits = effectLimits();
		var inner = effectLimits.inner() / (1 + ctx.params.cloudEccentricity);
		var outer = effectLimits.outer() / (1 - ctx.params.cloudEccentricity);
		return new Interval(Math.max(0, inner), outer);
	}

	public void accreteDust(AccreteContext ctx, DustBands dustBands) {
		double prevMass = 0;

		var iterationsRemaining = 100;
		while (this.mass - prevMass >= 0.0001 * this.mass) {
			prevMass = this.mass;
			if (iterationsRemaining-- <= 0)
				break;
			var swept = dustBands.sweep(ctx, this);
			Mod.LOGGER.info("swept {} mass", swept);
			this.mass += swept;
		}
	}

}
