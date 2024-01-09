package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.hawklib.collections.impl.VectorFloat;

public final class ScatterPlot {

	private VectorFloat data;

	private double minX, maxX;
	private double minY, maxY;

	public ScatterPlot(String xLabel, String xUnits, String yLabel, String yUnits) {
		reset();
	}

	public void reset() {
		this.minX = Double.POSITIVE_INFINITY;
		this.minY = Double.POSITIVE_INFINITY;
		this.maxX = Double.NEGATIVE_INFINITY;
		this.maxY = Double.NEGATIVE_INFINITY;
	}

	public void insert(double x, double y) {
		this.minX = Math.min(this.minX, x);
		this.minY = Math.min(this.minY, y);
		this.maxX = Math.max(this.maxX, x);
		this.maxY = Math.max(this.maxY, y);
		data.push((float) x);
		data.push((float) y);
	}

}
