package net.xavil.universegen.galaxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xavil.util.Assert;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public final class GalaxySector {

	public static final Logger LOGGER = LoggerFactory.getLogger("galaxysector");
	public static final double MIN_SECTOR_SIZE_ly = 10;

	public final int level;
	public final Vec3i levelPos;

	public final SectorInfo info;
	public final Vec3 min, max;
	public Branch children;

	public record Branch(GalaxySector nnn, GalaxySector nnp,
			GalaxySector npn, GalaxySector npp,
			GalaxySector pnn, GalaxySector pnp,
			GalaxySector ppn, GalaxySector ppp) {
	}

	public GalaxySector(int level, Vec3i levelPos, SectorInfo info) {
		this.level = level;
		this.levelPos = levelPos;
		this.info = info;

		final var boundScale = MIN_SECTOR_SIZE_ly * Math.pow(2, level);
		this.min = levelPos.lowerCorner().mul(boundScale);
		this.max = levelPos.upperCorner().mul(boundScale);
	}

	public void split() {
		if (this.children != null || this.level == 0)
			return;
		final var bp = this.levelPos.mul(2);
		final var nl = this.level - 1;
		final var nnn = new GalaxySector(nl, bp.add(0, 0, 0), new SectorInfo());
		final var nnp = new GalaxySector(nl, bp.add(0, 0, 1), new SectorInfo());
		final var npn = new GalaxySector(nl, bp.add(0, 1, 0), new SectorInfo());
		final var npp = new GalaxySector(nl, bp.add(0, 1, 1), new SectorInfo());
		final var pnn = new GalaxySector(nl, bp.add(1, 0, 0), new SectorInfo());
		final var pnp = new GalaxySector(nl, bp.add(1, 0, 1), new SectorInfo());
		final var ppn = new GalaxySector(nl, bp.add(1, 1, 0), new SectorInfo());
		final var ppp = new GalaxySector(nl, bp.add(1, 1, 1), new SectorInfo());
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

	public void populate() {
	}

}
