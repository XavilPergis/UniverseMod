package net.xavil.universal.common.universe.system.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.CelestialNodeChild;
import net.xavil.universegen.system.CelestialRing;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.Assert;
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
	private final List<Ring> rings = new ArrayList<>();

	public record Ring(Interval interval, double mass, double eccentricity) {
	}

	private Planetesimal(AccreteContext ctx) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
	}

	private Planetesimal(AccreteContext ctx, double semiMajor, Interval bounds) {
		this.ctx = ctx;
		this.id = ctx.nextPlanetesimalId++;
		// var semiMajor = ctx.rng.uniformDouble(bounds.lower(), bounds.higher());
		var eccentricity = randomEccentricity(ctx);
		Assert.isTrue(eccentricity >= 0 && eccentricity <= 1);
		this.orbitalShape = new OrbitalShape(eccentricity, semiMajor);
		this.mass = ctx.params.initialPlanetesimalMass;
	}

	public double hillSphereRadius(double parentMass) {
		return this.orbitalShape.semiMajor() * (1 - this.orbitalShape.eccentricity())
				* Math.cbrt(this.mass / (3 * parentMass));
	}

	public static Planetesimal random(AccreteContext ctx, double semiMajor, Interval bounds) {
		return new Planetesimal(ctx, semiMajor, bounds);
	}

	public static Planetesimal defaulted(AccreteContext ctx) {
		return new Planetesimal(ctx);
	}

	private void emitUpdateEvent() {
		this.ctx.debugConsumer.accept(new AccreteDebugEvent.PlanetesimalUpdated(this));
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
		emitUpdateEvent();
	}

	public OrbitalShape getOrbitalShape() {
		return orbitalShape;
	}

	public void setOrbitalShape(OrbitalShape newOrbitalShape) {
		this.orbitalShape = newOrbitalShape;
		Assert.isTrue(newOrbitalShape.eccentricity() >= 0 && newOrbitalShape.eccentricity() <= 1);
		emitUpdateEvent();
	}

	public Iterable<Planetesimal> getMoons() {
		return this.moons;
	}

	public void addMoon(Planetesimal newMoon) {
		this.moons.add(newMoon);
		newMoon.moonOf = this;
		// this.ctx.debugConsumer.accept(new
		// AccreteDebugEvent.OrbitalParentChanged(this, newMoon));
	}

	public void clearMoons() {
		this.moons.clear();
	}

	public void transformMoons(BiConsumer<List<Planetesimal>, List<Planetesimal>> consumer) {
		var prev = List.copyOf(this.moons);
		this.moons.clear();
		consumer.accept(prev, this.moons);

		for (var moon : this.moons) {
			moon.moonOf = this;
		}

		if (this.ctx.debugConsumer.shouldEmitEvents()) {
			var prevSet = prev.stream().map(p -> p.id).collect(Collectors.toSet());
			var curSet = this.moons.stream().map(p -> p.id).collect(Collectors.toSet());
			var added = Sets.difference(curSet, prevSet);
			var removed = Sets.difference(prevSet, curSet);
			if (!added.isEmpty() || !removed.isEmpty()) {
				this.ctx.debugConsumer.accept(new AccreteDebugEvent.UpdateOrbits(this.id, added, removed));
			}
		}
	}

	public Iterable<Ring> getRings() {
		return this.rings;
	}

	public void addRing(Ring newRing) {
		this.rings.add(newRing);
		if (this.ctx.debugConsumer.shouldEmitEvents()) {
			this.ctx.debugConsumer.accept(new AccreteDebugEvent.RingAdded(this, newRing));
		}
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

	public void convertToPlanetNode(CelestialNode parent) {
		var type = canSweepGas() ? PlanetaryCelestialNode.Type.GAS_GIANT : PlanetaryCelestialNode.Type.ROCKY_WORLD;
		var radiusRearth = this.getRadius() * Units.KILO / Units.m_PER_Rearth;
		var node = new PlanetaryCelestialNode(type, this.mass * Units.Yg_PER_Msol, radiusRearth, 300);
		node.rotationalPeriod = Mth.lerp(this.ctx.rng.uniformDouble(), 0.2 * 86400, 4 * 86400);
		node.obliquityAngle = this.ctx.rng.uniformDouble(-2.0 * Math.PI, 2.0 * Math.PI);

		for (var ring : this.rings) {
			// Mod.LOGGER.info("RING!!!");
			var intervalTm = new Interval(ring.interval.lower() * Units.Tm_PER_au,
					ring.interval.higher() * Units.Tm_PER_au);
			node.addRing(new CelestialRing(OrbitalPlane.ZERO, ring.eccentricity, intervalTm, ring.mass * Units.Yg_PER_Msol));
		}

		var shape = new OrbitalShape(this.orbitalShape.eccentricity(), this.orbitalShape.semiMajor() * Units.Tm_PER_au);
		var plane = OrbitalPlane.fromOrbitalElements(0, this.ctx.rng.uniformDouble(0, 2.0 * Math.PI),
				this.ctx.rng.uniformDouble(0, 2.0 * Math.PI));
		var orbit = new CelestialNodeChild<>(parent, node, shape, plane, this.ctx.rng.uniformDouble(0, 2.0 * Math.PI));
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

	// public void accreteDust(DustBands dustBands) {
	// double prevMass = 0;

	// var iterationsRemaining = 100;
	// while (this.mass - prevMass >= 0.0001 * this.mass) {
	// prevMass = this.mass;
	// if (iterationsRemaining-- <= 0)
	// break;
	// this.mass += dustBands.sweep(ctx, this);
	// emitUpdateEvent();
	// }

	// }

	public Ring asRing() {
		final var radiusAu = this.getRadius() / Units.km_PER_au;
		final var ringInner = this.orbitalShape.semiMajor() - radiusAu;
		final var ringOuter = this.orbitalShape.semiMajor() + radiusAu;
		return new Ring(new Interval(ringInner, ringOuter), mass, this.orbitalShape.eccentricity());
	}

}
