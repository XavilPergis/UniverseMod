package net.xavil.universal.client.camera;

// we need to have a single camera that can produce varied children cameras, with different near and far planes for rendering objects at different scales.
public final class CameraConfig {
	public final double nearPlane;
	public final double farPlane;

	public CameraConfig(double nearPlane, double farPlane) {
		this.nearPlane = nearPlane;
		this.farPlane = farPlane;
	}
}