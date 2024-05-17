package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.common.universe.ScalarField;

public class InterpolatedField implements ScalarField {

	public final Vec3 min, max;
	public final double invLerpFactorX, invLerpFactorY, invLerpFactorZ;
	public final Cube[] cubes;
	public final int subdivisions;

	private static final class Cube implements ScalarField {
		public final Vec3 min, max;
		public final double nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;
		public final double invLerpFactorX, invLerpFactorY, invLerpFactorZ;

		public Cube(ScalarField field, Vec3 min, Vec3 max) {
			this.min = min;
			this.max = max;
			this.nnn = field.sample(min.x, min.y, min.z);
			this.nnp = field.sample(min.x, min.y, max.z);
			this.npn = field.sample(min.x, max.y, min.z);
			this.npp = field.sample(min.x, max.y, max.z);
			this.pnn = field.sample(max.x, min.y, min.z);
			this.pnp = field.sample(max.x, min.y, max.z);
			this.ppn = field.sample(max.x, max.y, min.z);
			this.ppp = field.sample(max.x, max.y, max.z);
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
			this.invLerpFactorZ = 1.0 / (this.max.z - this.min.z);
		}

		public Cube(Vec3 min, Vec3 max,
				double nnn, double nnp, double npn, double npp,
				double pnn, double pnp, double ppn, double ppp) {
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

		public double interpolate(double tx, double ty, double tz) {
			// trilinear interpolation
			final var xnn = this.nnn + tx * (this.pnn - this.nnn);
			final var xpn = this.npn + tx * (this.ppn - this.npn);
			final var xnp = this.nnp + tx * (this.pnp - this.nnp);
			final var xpp = this.npp + tx * (this.ppp - this.npp);
			final var yn = xnn + ty * (xpn - xnn);
			final var yp = xnp + ty * (xpp - xnp);
			return yn + tz * (yp - yn);
		}

		@Override
		public double sample(double x, double y, double z) {
			final var tx = this.invLerpFactorX * (x - this.min.x);
			final var ty = this.invLerpFactorY * (y - this.min.y);
			final var tz = this.invLerpFactorZ * (z - this.min.z);
			return interpolate(tx, ty, tz);
		}

	}

	private static int idx(int size, int x, int y, int z) {
		return size * size * x + size * y + z;
	}

	public static ScalarField create(ScalarField field, Vec3 min, Vec3 max, int subdivisions) {
		return subdivisions == 0 ? new Cube(field, min, max) : new InterpolatedField(field, min, max, subdivisions);
	}

	private InterpolatedField(ScalarField field, Vec3 min, Vec3 max, int subdivisions) {
		final int sl = subdivisions + 1, gl = sl + 1;

		final var grid = new double[gl * gl * gl];
		final var pos = new Vec3.Mutable();
		for (int x = 0; x < gl; ++x) {
			for (int y = 0; y < gl; ++y) {
				for (int z = 0; z < gl; ++z) {
					pos.x = Mth.lerp(x / (double) gl, min.x, max.x);
					pos.y = Mth.lerp(y / (double) gl, min.y, max.y);
					pos.z = Mth.lerp(z / (double) gl, min.z, max.z);
					grid[idx(gl, x, y, z)] = field.sample(pos);
				}
			}
		}

		this.cubes = new InterpolatedField.Cube[sl * sl * sl];
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
					this.cubes[idx(sl, x, y, z)] = new Cube(
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

	public double interpolate(double tx, double ty, double tz) {
		tx *= (this.subdivisions + 1);
		ty *= (this.subdivisions + 1);
		tz *= (this.subdivisions + 1);
		final int ix = Mth.floor(tx), iy = Mth.floor(ty), iz = Mth.floor(tz);
		return this.cubes[idx(this.subdivisions + 1, ix, iy, iz)]
				.sample(tx - ix, ty - iy, tz - iz);
	}

	@Override
	public double sample(double x, double y, double z) {
		final var tx = this.invLerpFactorX * (x - this.min.x);
		final var ty = this.invLerpFactorY * (y - this.min.y);
		final var tz = this.invLerpFactorZ * (z - this.min.z);
		return interpolate(tx, ty, tz);
	}

}