package net.xavil.hawklib.client.screen;

import java.util.function.Consumer;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static net.xavil.hawklib.client.HawkDrawStates.*;

import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.FpCamera;
import net.xavil.hawklib.client.camera.HawkCamera;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.mixin.accessor.MouseHandlerAccessor;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public abstract class HawkScreen3d extends HawkScreen {

	protected boolean isForwardPressed = false, isBackwardPressed = false,
			isLeftPressed = false, isRightPressed = false,
			isUpPressed = false, isDownPressed = false,
			isRotateCWPressed = false, isRotateCCWPressed = false;
	public double scrollMultiplier = 1.2;
	public double scrollMin, scrollMax;

	public HawkCamera camera;

	public static abstract class Layer3d extends Layer2d {
		protected CameraConfig cameraConfig;
		protected CachedCamera lastCamera;
		protected CachedCamera camera;

		private CachedCamera.FrustumCorners frustumPoints = null;
		private CachedCamera.FrustumCorners cullingFrustumPoints = null;
		private CachedCamera cullingCamera = null;

		public Layer3d(HawkScreen3d attachedScreen, CameraConfig cameraConfig) {
			super(attachedScreen);
			this.cameraConfig = cameraConfig;
		}

		@Override
		@OverridingMethodsMustInvokeSuper
		public void render(RenderContext ctx) {
			final var snapshot = RenderMatricesSnapshot.capture();
			this.camera.applyProjection();
			CachedCamera.applyView(this.camera.orientation);
			render3d(this.camera, ctx);
			snapshot.restore();
		}

		public void setup3d(HawkCamera camera, float partialTick) {
		}

		public abstract void render3d(CachedCamera camera, RenderContext ctx);

		public void onMoved(Vec3 displacement) {
		}

		public CachedCamera getCullingCamera() {
			return this.cullingCamera != null ? this.cullingCamera : this.camera;
		}
	}

	protected HawkScreen3d(Component component, Screen previousScreen, HawkCamera camera,
			double scrollMin, double scrollMax) {
		super(component, previousScreen);
		this.camera = camera;
		this.scrollMin = scrollMin;
		this.scrollMax = scrollMax;

		// this.camera.pitch.set(Math.PI / 8);
		// this.camera.yaw.set(Math.PI / 8);
		// this.camera.scale.set((scrollMin + scrollMax) / 2.0);
		// this.camera.scale.target = (scrollMin + scrollMax) / 2.0;
	}

	private double prevMouseX, prevMouseY;
	private boolean hasPrevMousePos = false;

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (!hasPrevMousePos) {
			this.prevMouseX = mouseX;
			this.prevMouseY = mouseY;
			hasPrevMousePos = true;
			return;
		}
		
		final var deltaX = mouseX - this.prevMouseX;
		final var deltaY = mouseY - this.prevMouseY;
		this.prevMouseX = mouseX;
		this.prevMouseY = mouseY;
		
		if (this.camera instanceof FpCamera fpCamera) {
			MouseHandlerAccessor.grabMouse(false);
			fpCamera.orientation = Quat.axisAngle(Vec3.XP, 0.005 * deltaY).mul(fpCamera.orientation);
			fpCamera.orientation = Quat.axisAngle(Vec3.YP, 0.005 * deltaX).mul(fpCamera.orientation);
		} else {
			this.client.mouseHandler.releaseMouse();
		}
	}

	@Override
	public boolean mouseDragged(Vec2 mousePos, Vec2 delta, int button) {
		this.setDragging(true);

		if (button == 2) {
			moveCamera(delta, 0, true);
		} else if (button == 1) {
			moveCamera(Vec2.ZERO, delta.y, true);
		} else if (button == 0) {
			rotateCamera(delta);
		}

		return true;
	}

	public void rotateCamera(Vec2 horiz) {
		if (this.camera instanceof OrbitCamera orbitCam) {
			orbitCam.yaw.target = orbitCam.yaw.target + horiz.x * 0.005;
			var desiredPitch = orbitCam.pitch.target + horiz.y * 0.005;
			var actualPitch = Mth.clamp(desiredPitch, -Math.PI / 2, Math.PI / 2);
			orbitCam.pitch.target = actualPitch;
		} else if (this.camera instanceof FpCamera fpCamera) {
			fpCamera.orientation = Quat.axisAngle(Vec3.ZN, 0.01 * horiz.x).mul(fpCamera.orientation);
		}
	}

	public void moveCamera(Vec2 horiz, double vert, boolean invert) {
		if (this.camera instanceof OrbitCamera orbitCam) {
			final var dragScale = orbitCam.scale.current * (orbitCam.metersPerUnit / 1e12) * 0.0035;

			horiz = invert && orbitCam.pitch.current < 0 ? horiz.withY(-horiz.y) : horiz;
			final var offset = new Vec3(horiz.x, 0, horiz.y)
					.rotateY(-orbitCam.yaw.current)
					.add(0, vert, 0).mul(dragScale);
			if (offset.length() > 0) {
				orbitCam.focus.target = orbitCam.focus.target.add(offset);
				onMoved(offset);
			}
		} else if (this.camera instanceof FpCamera fpCamera) {
			Vec3 offset = new Vec3(-horiz.x, -vert, -horiz.y);
			offset = fpCamera.orientation.inverse().transform(offset);
			offset = offset.mul(fpCamera.speed.current);
			// final var rightDir = fpCamera.orientation.transform(Vec3.XP);
			// final var upDir = fpCamera.orientation.transform(Vec3.YP);
			// final var forwardDir = fpCamera.orientation.transform(Vec3.ZN);
			// final var offset =
			// rightDir.mul(horiz.x).add(upDir.mul(vert)).add(forwardDir.mul(horiz.y)).mul(100);
			if (offset.length() > 0) {
				fpCamera.pos.target = fpCamera.pos.target.add(offset);
				// fpCamera.pos.set(fpCamera.pos.current.add(offset));
				onMoved(offset);
			}
		}
	}

	@Override
	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		if (this.camera instanceof OrbitCamera orbitCam) {
			final var prevTarget = orbitCam.scale.target;
			final var currentZoomPercentage = Mth.inverseLerp(prevTarget, this.scrollMin, this.scrollMax);
			final var scrollSpeed = Mth.lerp(currentZoomPercentage, 1.05, 1.2);
			if (scrollDelta > 0) {
				// zoom out
				orbitCam.scale.target = Math.max(prevTarget / scrollSpeed, this.scrollMin);
				return true;
			} else if (scrollDelta < 0) {
				// zoom in
				orbitCam.scale.target = Math.min(prevTarget * scrollSpeed, this.scrollMax);
				return true;
			}
		} else if (this.camera instanceof FpCamera fpCamera) {
			// final var prevTarget = fpCamera.speed.target;
			// final var currentZoomPercentage = Mth.inverseLerp(prevTarget, this.scrollMin,
			// this.scrollMax);
			// final var scrollSpeed = Mth.lerp(currentZoomPercentage, 1.05, 1.2);
			final var scrollSpeed = 1.06;
			if (scrollDelta > 0) {
				fpCamera.speed.target *= scrollSpeed;
			} else if (scrollDelta < 0) {
				fpCamera.speed.target /= scrollSpeed;
			}
			fpCamera.speed.target = Mth.clamp(fpCamera.speed.target, 1e-8, 1e6);
			return true;
		}
		return false;
	}

	private void forEach3dLayer(Consumer<Layer3d> consumer) {
		this.layers.forEach(layer -> {
			if (layer instanceof Layer3d layer3d)
				consumer.accept(layer3d);
		});
	}

	private void debugCameraFrustum() {
		forEach3dLayer(layer -> {
			if (layer.lastCamera == null)
				return;
			layer.frustumPoints = layer.lastCamera.captureFrustumCornersWorld();
		});
	}

	private void debugCullingCamera() {
		forEach3dLayer(layer -> {
			if (layer.lastCamera == null)
				return;
			layer.cullingCamera = layer.lastCamera;
			layer.cullingFrustumPoints = layer.lastCamera.captureFrustumCornersWorld();
		});
	}

	private void clearDebug() {
		forEach3dLayer(layer -> {
			layer.frustumPoints = null;
			layer.cullingFrustumPoints = null;
			layer.cullingCamera = null;
		});
	}

	@Override
	public boolean keyPressed(Keypress keypress) {
		if (keypress.hasModifiers(GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)) {
			if (keypress.keyCode == GLFW.GLFW_KEY_F) {
				debugCameraFrustum();
				return true;
			} else if (keypress.keyCode == GLFW.GLFW_KEY_C) {
				debugCullingCamera();
				return true;
			} else if (keypress.keyCode == GLFW.GLFW_KEY_V) {
				clearDebug();
				return true;
			}
		}

		if (keypress.keyCode == GLFW.GLFW_KEY_TAB) {
			final var oldCamera = this.camera;
			if (this.camera instanceof OrbitCamera) {
				final var newCamera = new FpCamera(this.camera.metersPerUnit);
				newCamera.pos.set(oldCamera.cached(new CameraConfig(1, true, 1, true)).pos.xyz());
				newCamera.nearPlane = oldCamera.nearPlane;
				newCamera.farPlane = oldCamera.farPlane;
				newCamera.fovDeg = oldCamera.fovDeg;
				this.camera = newCamera;
			} else {
				final var newCamera = new OrbitCamera(this.camera.metersPerUnit);
				newCamera.focus.set(oldCamera.cached(new CameraConfig(1, true, 1, true)).pos.xyz());
				newCamera.nearPlane = oldCamera.nearPlane;
				newCamera.farPlane = oldCamera.farPlane;
				newCamera.fovDeg = oldCamera.fovDeg;
				this.camera = newCamera;
			}
			return true;
		}

		// TODO: key mappings
		if (keypress.keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_Q) {
			this.isDownPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_E) {
			this.isUpPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_Z) {
			this.isRotateCWPressed = true;
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_C) {
			this.isRotateCCWPressed = true;
			return true;
		}

		return false;
	}

	private void renderCameraFrustum(CachedCamera camera, CachedCamera.FrustumCorners frustum, ColorRgba color) {
		if (frustum == null)
			return;

		final var builder = BufferRenderer.IMMEDIATE_BUILDER
				.beginGeneric(IndexPattern.VANILLA_LINES, BufferLayout.POSITION_COLOR_NORMAL);

		// near
		RenderHelper.addLine(builder, camera, frustum.nnn(), frustum.npn(), color);
		RenderHelper.addLine(builder, camera, frustum.pnn(), frustum.ppn(), color);
		RenderHelper.addLine(builder, camera, frustum.nnn(), frustum.pnn(), color);
		RenderHelper.addLine(builder, camera, frustum.npn(), frustum.ppn(), color);
		// far
		RenderHelper.addLine(builder, camera, frustum.nnp(), frustum.npp(), color);
		RenderHelper.addLine(builder, camera, frustum.pnp(), frustum.ppp(), color);
		RenderHelper.addLine(builder, camera, frustum.nnp(), frustum.pnp(), color);
		RenderHelper.addLine(builder, camera, frustum.npp(), frustum.ppp(), color);
		// sides
		RenderHelper.addLine(builder, camera, frustum.nnn(), frustum.nnp(), color);
		RenderHelper.addLine(builder, camera, frustum.npn(), frustum.npp(), color);
		RenderHelper.addLine(builder, camera, frustum.pnn(), frustum.pnp(), color);
		RenderHelper.addLine(builder, camera, frustum.ppn(), frustum.ppp(), color);

		builder.end().draw(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(), DRAW_STATE_LINES);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (super.keyReleased(keyCode, scanCode, modifiers))
			return true;

		// TODO: key mappings
		if (keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_Q) {
			this.isDownPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_E) {
			this.isUpPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_Z) {
			this.isRotateCWPressed = false;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_C) {
			this.isRotateCCWPressed = false;
			return true;
		}

		return false;
	}

	@Override
	public void tick() {
		super.tick();

		final double speed = 25, rotateSpeed = 10;

		double forward = 0, right = 0, up = 0;
		forward += this.isForwardPressed ? speed : 0;
		forward += this.isBackwardPressed ? -speed : 0;
		right += this.isLeftPressed ? speed : 0;
		right += this.isRightPressed ? -speed : 0;
		up += this.isUpPressed ? -speed : 0;
		up += this.isDownPressed ? speed : 0;
		moveCamera(Vec2.from(right, forward), up, false);

		double rotate = 0;
		rotate += this.isRotateCWPressed ? rotateSpeed : 0;
		rotate += this.isRotateCCWPressed ? -rotateSpeed : 0;
		rotateCamera(Vec2.from(rotate, 0));
	}

	public abstract CachedCamera setupCamera(CameraConfig config, float partialTick);

	public void onMoved(Vec3 displacement) {
		forEach3dLayer(layer -> layer.onMoved(displacement));
	}

	private static CameraConfig getDebugCameraConfig() {
		return new CameraConfig(0.01, true, 1e6, true);
	}

	@Override
	public void renderScreenPreLayers(RenderContext ctx) {
		this.camera.tick(ctx.deltaTime);
		forEach3dLayer(layer -> {
			layer.setup3d(this.camera, ctx.partialTick);
			layer.lastCamera = layer.camera;
			layer.camera = setupCamera(layer.cameraConfig, ctx.partialTick);
			if (layer.lastCamera == null)
				layer.lastCamera = layer.camera;
		});
	}

	@Override
	public void renderScreenPostLayers(RenderContext ctx) {
		final var debugCamera = setupCamera(getDebugCameraConfig(), ctx.partialTick);

		final var snapshot = RenderMatricesSnapshot.capture();
		debugCamera.applyProjection();
		CachedCamera.applyView(debugCamera.orientation);
		forEach3dLayer(layer -> {
			renderCameraFrustum(debugCamera, layer.frustumPoints, ColorRgba.YELLOW);
			renderCameraFrustum(debugCamera, layer.cullingFrustumPoints, ColorRgba.CYAN);
		});
		snapshot.restore();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (this.client.mouseHandler.isMouseGrabbed() && this.client.screen != null) {
			this.client.mouseHandler.releaseMouse();
		}
	}

}
