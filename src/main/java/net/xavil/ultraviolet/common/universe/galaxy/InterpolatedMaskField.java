package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public class InterpolatedMaskField implements GalaxyRegionWeights.Field {

	public final Vec3 min, max;
	public final double invLerpFactorX, invLerpFactorY, invLerpFactorZ;
	public final Cell[] cells;
	public final int subdivisions;

	private static final class Cell implements GalaxyRegionWeights.Field {
		public final Vec3 min, max;
		public final GalaxyRegionWeights nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;
		public final double invLerpFactorX, invLerpFactorY, invLerpFactorZ;

		public Cell(GalaxyRegionWeights.Field field, Vec3 min, Vec3 max) {
			this.min = min;
			this.max = max;
			this.nnn = new GalaxyRegionWeights();
			this.nnp = new GalaxyRegionWeights();
			this.npn = new GalaxyRegionWeights();
			this.npp = new GalaxyRegionWeights();
			this.pnn = new GalaxyRegionWeights();
			this.pnp = new GalaxyRegionWeights();
			this.ppn = new GalaxyRegionWeights();
			this.ppp = new GalaxyRegionWeights();
			field.evaluate(new Vec3(min.x, min.y, min.z), this.nnn);
			field.evaluate(new Vec3(min.x, min.y, max.z), this.nnp);
			field.evaluate(new Vec3(min.x, max.y, min.z), this.npn);
			field.evaluate(new Vec3(min.x, max.y, max.z), this.npp);
			field.evaluate(new Vec3(max.x, min.y, min.z), this.pnn);
			field.evaluate(new Vec3(max.x, min.y, max.z), this.pnp);
			field.evaluate(new Vec3(max.x, max.y, min.z), this.ppn);
			field.evaluate(new Vec3(max.x, max.y, max.z), this.ppp);
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
			this.invLerpFactorZ = 1.0 / (this.max.z - this.min.z);
		}

		public Cell(Vec3 min, Vec3 max,
				GalaxyRegionWeights nnn, GalaxyRegionWeights nnp,
				GalaxyRegionWeights npn, GalaxyRegionWeights npp,
				GalaxyRegionWeights pnn, GalaxyRegionWeights pnp,
				GalaxyRegionWeights ppn, GalaxyRegionWeights ppp) {
			this.min = min;
			this.max = max;
			this.nnn = nnn;
			this.nnp = nnp;
			this.npn = npn;
			this.npp = npp;
			this.pnn = pnn;
			this.pnp = pnp;
			this.ppn = ppn;
			this.ppp = ppp;
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
			this.invLerpFactorZ = 1.0 / (this.max.z - this.min.z);
		}

		public void interpolate(double tx, double ty, double tz, GalaxyRegionWeights masks) {
			// trilinear interpolation
			final var xnnCore = this.nnn.core + tx * (this.pnn.core - this.nnn.core);
			final var xpnCore = this.npn.core + tx * (this.ppn.core - this.npn.core);
			final var xnpCore = this.nnp.core + tx * (this.pnp.core - this.nnp.core);
			final var xppCore = this.npp.core + tx * (this.ppp.core - this.npp.core);
			final var xnnArms = this.nnn.arms + tx * (this.pnn.arms - this.nnn.arms);
			final var xpnArms = this.npn.arms + tx * (this.ppn.arms - this.npn.arms);
			final var xnpArms = this.nnp.arms + tx * (this.pnp.arms - this.nnp.arms);
			final var xppArms = this.npp.arms + tx * (this.ppp.arms - this.npp.arms);
			final var xnnDisc = this.nnn.disc + tx * (this.pnn.disc - this.nnn.disc);
			final var xpnDisc = this.npn.disc + tx * (this.ppn.disc - this.npn.disc);
			final var xnpDisc = this.nnp.disc + tx * (this.pnp.disc - this.nnp.disc);
			final var xppDisc = this.npp.disc + tx * (this.ppp.disc - this.npp.disc);
			final var xnnHalo = this.nnn.halo + tx * (this.pnn.halo - this.nnn.halo);
			final var xpnHalo = this.npn.halo + tx * (this.ppn.halo - this.npn.halo);
			final var xnpHalo = this.nnp.halo + tx * (this.pnp.halo - this.nnp.halo);
			final var xppHalo = this.npp.halo + tx * (this.ppp.halo - this.npp.halo);

			final var ynCore = xnnCore + ty * (xpnCore - xnnCore);
			final var ypCore = xnpCore + ty * (xppCore - xnpCore);
			final var ynArms = xnnArms + ty * (xpnArms - xnnArms);
			final var ypArms = xnpArms + ty * (xppArms - xnpArms);
			final var ynDisc = xnnDisc + ty * (xpnDisc - xnnDisc);
			final var ypDisc = xnpDisc + ty * (xppDisc - xnpDisc);
			final var ynHalo = xnnHalo + ty * (xpnHalo - xnnHalo);
			final var ypHalo = xnpHalo + ty * (xppHalo - xnpHalo);

			masks.core = ynCore + tz * (ypCore - ynCore);
			masks.arms = ynArms + tz * (ypArms - ynArms);
			masks.disc = ynDisc + tz * (ypDisc - ynDisc);
			masks.halo = ynHalo + tz * (ypHalo - ynHalo);
		}

		@Override
		public void evaluate(Vec3Access pos, GalaxyRegionWeights masks) {
			final var tx = this.invLerpFactorX * (pos.x() - this.min.x);
			final var ty = this.invLerpFactorY * (pos.y() - this.min.y);
			final var tz = this.invLerpFactorZ * (pos.z() - this.min.z);
			interpolate(tx, ty, tz, masks);
		}

	}

	private static int idx(int size, int x, int y, int z) {
		return size * size * x + size * y + z;
	}

	public static GalaxyRegionWeights.Field create(GalaxyRegionWeights.Field field, Vec3 min, Vec3 max,
			int subdivisions) {
		return subdivisions == 1 ? new Cell(field, min, max) : new InterpolatedMaskField(field, min, max, subdivisions);
	}

	private InterpolatedMaskField(GalaxyRegionWeights.Field field, Vec3 min, Vec3 max, int subdivisions) {
		final int sl = subdivisions, gl = sl + 1;

		final var grid = new GalaxyRegionWeights[gl * gl * gl];
		final var pos = new Vec3.Mutable();
		for (int x = 0; x < gl; ++x) {
			for (int y = 0; y < gl; ++y) {
				for (int z = 0; z < gl; ++z) {
					pos.x = Mth.lerp(x / (gl - 1.0), min.x, max.x);
					pos.y = Mth.lerp(y / (gl - 1.0), min.y, max.y);
					pos.z = Mth.lerp(z / (gl - 1.0), min.z, max.z);
					final var masks = new GalaxyRegionWeights();
					field.evaluate(pos, masks);
					grid[idx(gl, x, y, z)] = masks;
				}
			}
		}

		this.cells = new Cell[sl * sl * sl];
		for (int x = 0; x < sl; ++x) {
			for (int y = 0; y < sl; ++y) {
				for (int z = 0; z < sl; ++z) {
					final int lx = x, ly = y, lz = z, hx = x + 1, hy = y + 1, hz = z + 1;
					final var nnn = grid[idx(gl, lx, ly, lz)];
					final var nnp = grid[idx(gl, lx, ly, hz)];
					final var npn = grid[idx(gl, lx, hy, lz)];
					final var npp = grid[idx(gl, lx, hy, hz)];
					final var pnn = grid[idx(gl, hx, ly, lz)];
					final var pnp = grid[idx(gl, hx, ly, hz)];
					final var ppn = grid[idx(gl, hx, hy, lz)];
					final var ppp = grid[idx(gl, hx, hy, hz)];
					final var xmin = Mth.lerp(x / (double) sl, min.x, max.x);
					final var ymin = Mth.lerp(y / (double) sl, min.y, max.y);
					final var zmin = Mth.lerp(z / (double) sl, min.z, max.z);
					final var xmax = Mth.lerp((x + 1) / (double) sl, min.x, max.x);
					final var ymax = Mth.lerp((y + 1) / (double) sl, min.y, max.y);
					final var zmax = Mth.lerp((z + 1) / (double) sl, min.z, max.z);
					this.cells[idx(sl, x, y, z)] = new Cell(
							new Vec3(xmin, ymin, zmin), new Vec3(xmax, ymax, zmax),
							nnn, nnp, npn, npp, pnn, pnp, ppn, ppp);
				}
			}
		}

		this.min = min;
		this.max = max;
		this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
		this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
		this.invLerpFactorZ = 1.0 / (this.max.z - this.min.z);
		this.subdivisions = subdivisions;
	}

	public void interpolate(double tx, double ty, double tz, GalaxyRegionWeights masks) {
		if (tx < 0 || tx > 1 || ty < 0 || ty > 1 || tz < 0 || tz > 1)
			masks.arms = masks.core = masks.disc = masks.halo = 0;
		tx *= this.subdivisions;
		ty *= this.subdivisions;
		tz *= this.subdivisions;
		int ix = Mth.floor(tx), iy = Mth.floor(ty), iz = Mth.floor(tz);
		if (ix == this.subdivisions)
			ix -= 1;
		if (iy == this.subdivisions)
			iy -= 1;
		if (iz == this.subdivisions)
			iz -= 1;
		this.cells[idx(this.subdivisions, ix, iy, iz)]
				.interpolate(tx - ix, ty - iy, tz - iz, masks);
	}

	@Override
	public void evaluate(Vec3Access pos, GalaxyRegionWeights masks) {
		final var tx = this.invLerpFactorX * (pos.x() - this.min.x);
		final var ty = this.invLerpFactorY * (pos.y() - this.min.y);
		final var tz = this.invLerpFactorZ * (pos.z() - this.min.z);
		interpolate(tx, ty, tz, masks);
	}

}