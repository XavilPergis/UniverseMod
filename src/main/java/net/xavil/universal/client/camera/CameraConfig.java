package net.xavil.universal.client.camera;

// we need to have a single camera that can produce varied children cameras, with different near and far planes for rendering objects at different scales.
public final class CameraConfig {
	public final double nearPlane;
	public final double farPlane;
	public final double metersPerUnit;
	public final double renderScaleFactor;

	public CameraConfig(double nearPlane, double farPlane, double metersPerUnit, double renderScaleFactor) {
		this.nearPlane = nearPlane;
		this.farPlane = farPlane;
		this.metersPerUnit = metersPerUnit;
		this.renderScaleFactor = renderScaleFactor;
	}
}