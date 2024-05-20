package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.ultraviolet.common.universe.ScalarField2;

public class InterpolatedField2 implements ScalarField2 {

	public final Vec2 min, max;
	public final double invLerpFactorX, invLerpFactorY;
	public final Cell[] cells;
	public final int subdivisions;

	private static final class Cell implements ScalarField2 {
		public final Vec2 min, max;
		public final double nn, np, pn, pp;
		public final double invLerpFactorX, invLerpFactorY;

		public Cell(ScalarField2 field, Vec2 min, Vec2 max) {
			this.min = min;
			this.max = max;
			this.nn = field.sample(min.x, min.y);
			this.np = field.sample(min.x, max.y);
			this.pn = field.sample(max.x, min.y);
			this.pp = field.sample(max.x, max.y);
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
		}

		public Cell(Vec2 min, Vec2 max,
				double nn, double np, double pn, double pp) {
			this.min = min;
			this.max = max;
			this.nn = nn;
			this.np = np;
			this.pn = pn;
			this.pp = pp;
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
		}

		public double interpolate(double tx, double ty) {
			// bilinear interpolation
			final var yn = this.nn + tx * (this.pn - this.nn);
			final var yp = this.np + tx * (this.pp - this.np);
			return yn + ty * (yp - yn);
		}

		@Override
		public double sample(double x, double y) {
			final var tx = this.invLerpFactorX * (x - this.min.x);
			final var ty = this.invLerpFactorY * (y - this.min.y);
			return interpolate(tx, ty);
		}

	}

	private static int idx(int size, int x, int y) {
		return size * x + y;
	}

	public static ScalarField2 create(ScalarField2 field, Vec2 min, Vec2 max, int subdivisions) {
		// return subdivisions == 1 ? new Cell(field, min, max) : new
		// InterpolatedField2(field, min, max, subdivisions);
		return new InterpolatedField2(field, min, max, subdivisions);
	}

	private InterpolatedField2(ScalarField2 field, Vec2 min, Vec2 max, int subdivisions) {
		final int sl = subdivisions, gl = sl + 1;

		final var grid = new double[gl * gl];
		for (int gx = 0; gx < gl; ++gx) {
			for (int gy = 0; gy < gl; ++gy) {
				final var px = Mth.lerp(gx / (gl - 1.0), min.x, max.x);
				final var py = Mth.lerp(gy / (gl - 1.0), min.y, max.y);
				grid[idx(gl, gx, gy)] = field.sample(px, py);
			}
		}

		this.cells = new InterpolatedField2.Cell[sl * sl];
		for (int x = 0; x < sl; ++x) {
			for (int y = 0; y < sl; ++y) {
				final int lx = x, ly = y, hx = x + 1, hy = y + 1;
				final var nn = grid[idx(gl, lx, ly)];
				final var np = grid[idx(gl, lx, hy)];
				final var pn = grid[idx(gl, hx, ly)];
				final var pp = grid[idx(gl, hx, hy)];
				final var xmin = Mth.lerp(lx / (double) sl, min.x, max.x);
				final var ymin = Mth.lerp(ly / (double) sl, min.y, max.y);
				final var xmax = Mth.lerp(hx / (double) sl, min.x, max.x);
				final var ymax = Mth.lerp(hy / (double) sl, min.y, max.y);
				this.cells[idx(sl, x, y)] = new Cell(
						new Vec2(xmin, ymin), new Vec2(xmax, ymax),
						nn, np, pn, pp);
			}
		}

		this.min = min;
		this.max = max;
		this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
		this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
		this.subdivisions = subdivisions;
	}

	public double interpolate(double tx, double ty) {
		if (tx < 0 || tx > 1 || ty < 0 || ty > 1)
			return 0;
		tx *= this.subdivisions;
		ty *= this.subdivisions;
		int ix = Mth.floor(tx), iy = Mth.floor(ty);
		if (ix == this.subdivisions)
			ix -= 1;
		if (iy == this.subdivisions)
			iy -= 1;
		return this.cells[idx(this.subdivisions, ix, iy)]
				.interpolate(tx - ix, ty - iy);
	}

	@Override
	public double sample(double x, double y) {
		final var tx = this.invLerpFactorX * (x - this.min.x);
		final var ty = this.invLerpFactorY * (y - this.min.y);
		return interpolate(tx, ty);
	}

}