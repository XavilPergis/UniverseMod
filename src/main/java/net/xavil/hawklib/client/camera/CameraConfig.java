package net.xavil.hawklib.client.camera;

// we need to have a single camera that can produce varied children cameras, with different near and far planes for rendering objects at different scales.
public final class CameraConfig {
	public final double nearPlane;
	public final boolean nearPlaneSlidingScale;
	public final double farPlane;
	public final boolean farPlaneSlidingScale;

	public CameraConfig(double nearPlane, boolean nearPlaneSlidingScale,
			double farPlane, boolean farPlaneSlidingScale) {
		this.nearPlane = nearPlane;
		this.nearPlaneSlidingScale = nearPlaneSlidingScale;
		this.farPlane = farPlane;
		this.farPlaneSlidingScale = farPlaneSlidingScale;
	}

	public double getNear(double scale) {
		return this.nearPlaneSlidingScale ? scale * this.nearPlane : this.nearPlane;
	}

	public double getFar(double scale) {
		return this.farPlaneSlidingScale ? scale * this.farPlane : this.farPlane;
	}
}