package net.xavil.universegen.galaxy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Rng;
import net.xavil.util.math.Interval;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public final class GalaxySector {

	public static final Logger LOGGER = LoggerFactory.getLogger("galaxysector");
	public static final double MIN_SECTOR_SIZE_ly = 10;
	public static final int LEVEL_COUNT = 8;
	public static final int SECTOR_ELEMENT_LIMIT = 10000;

	private static final double TOTAL_LEVEL_WEIGHT;
	private static final Interval[] LEVEL_MASS_INTERVALS;

	static {
		double w = 0.0;
		for (var l = 0; l < LEVEL_COUNT; ++l)
			w += 1 << l;

		final var intervals = new ArrayList<Interval>();
		double cumulativePercentage = 0;
		for (var l = 0; l < LEVEL_COUNT; ++l) {
			var percentage = (1 << (LEVEL_COUNT - l)) / w;
			var interval = new Interval(cumulativePercentage, cumulativePercentage + percentage);
			cumulativePercentage += percentage;
			intervals.add(interval);
		}
		TOTAL_LEVEL_WEIGHT = w;
		LEVEL_MASS_INTERVALS = intervals.toArray(new Interval[0]);
	}

	public final int level;
	public final Vec3i levelPos;
	private final GalaxySector parent;
	private final Galaxy galaxy;

	public final List<GalaxySectorEntry> entries = new ArrayList<>();
	public double remainingMass;

	public final Vec3 min, max;
	public Branch children;

	public record Branch(GalaxySector nnn, GalaxySector nnp,
			GalaxySector npn, GalaxySector npp,
			GalaxySector pnn, GalaxySector pnp,
			GalaxySector ppn, GalaxySector ppp) {
	}

	public GalaxySector(Galaxy galaxy, GalaxySector parent, int level, Vec3i levelPos) {
		this.galaxy = galaxy;
		this.parent = parent;
		this.level = level;
		this.levelPos = levelPos;

		final var boundScale = MIN_SECTOR_SIZE_ly * Math.pow(2, level);
		this.min = levelPos.lowerCorner().mul(boundScale);
		this.max = levelPos.upperCorner().mul(boundScale);
	}

	public void split() {
		if (this.children != null || this.level == 0)
			return;
		final var bp = this.levelPos.mul(2);
		final var nl = this.level - 1;
		final var nnn = new GalaxySector(this.galaxy, this, nl, bp.add(0, 0, 0));
		final var nnp = new GalaxySector(this.galaxy, this, nl, bp.add(0, 0, 1));
		final var npn = new GalaxySector(this.galaxy, this, nl, bp.add(0, 1, 0));
		final var npp = new GalaxySector(this.galaxy, this, nl, bp.add(0, 1, 1));
		final var pnn = new GalaxySector(this.galaxy, this, nl, bp.add(1, 0, 0));
		final var pnp = new GalaxySector(this.galaxy, this, nl, bp.add(1, 0, 1));
		final var ppn = new GalaxySector(this.galaxy, this, nl, bp.add(1, 1, 0));
		final var ppp = new GalaxySector(this.galaxy, this, nl, bp.add(1, 1, 1));
		this.children = new Branch(nnn, nnp, npn, npp, pnn, pnp, ppn, ppp);
	}

	public GalaxySector lookup(int level, Vec3i levelPos) {
		if (!levelPos.div(1 << level).equals(this.levelPos))
			return null;
		if (level == this.level)
			return this;
		if (this.children == null)
			return null;

		// @formatter:off
		var nnn = this.children.nnn.lookup(level, levelPos);
		if (nnn != null) return nnn;
		var nnp = this.children.nnp.lookup(level, levelPos);
		if (nnp != null) return nnp;
		var npn = this.children.npn.lookup(level, levelPos);
		if (npn != null) return npn;
		var npp = this.children.npp.lookup(level, levelPos);
		if (npp != null) return npp;
		var pnn = this.children.pnn.lookup(level, levelPos);
		if (pnn != null) return pnn;
		var pnp = this.children.pnp.lookup(level, levelPos);
		if (pnp != null) return pnp;
		var ppn = this.children.ppn.lookup(level, levelPos);
		if (ppn != null) return ppn;
		var ppp = this.children.ppp.lookup(level, levelPos);
		if (ppp != null) return ppp;
		// @formatter:on

		throw Assert.isUnreachable();
	}

	public void populate(Rng rng) {
		for (var i = 0; i < SECTOR_ELEMENT_LIMIT; ++i) {
			final var massT = rng.uniformDouble(LEVEL_MASS_INTERVALS[this.level]);
			final var mass = this.galaxy.stellarMassDistribution.sample(massT);
			if (this.remainingMass < mass)
				break;

			final var age = rng.uniformDouble(1, this.galaxy.galaxyAgeMyr);
			final var node = StellarCelestialNode.fromMassAndAge(rng, mass, age);

			final var entry = new GalaxySectorEntry(node);
			this.entries.add(entry);

			this.remainingMass -= mass;
		}
	}

}
