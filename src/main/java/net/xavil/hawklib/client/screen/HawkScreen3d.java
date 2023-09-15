package net.xavil.hawklib.client.screen;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static net.xavil.hawklib.client.HawkDrawStates.*;

import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public abstract class HawkScreen3d extends HawkScreen {

	protected boolean isForwardPressed = false, isBackwardPressed = false,
			isLeftPressed = false, isRightPressed = false,
			isUpPressed = false, isDownPressed = false,
			isRotateCWPressed = false, isRotateCCWPressed = false;
	public double scrollMultiplier = 1.2;
	public double scrollMin, scrollMax;

	public final OrbitCamera camera;

	public static abstract class Layer3d extends Layer2d {
		protected CameraConfig cameraConfig;
		protected OrbitCamera.Cached lastCamera;
		protected OrbitCamera.Cached camera;

		private CachedCamera.FrustumCorners frustumPoints = null;
		private CachedCamera.FrustumCorners cullingFrustumPoints = null;
		private OrbitCamera.Cached cullingCamera = null;

		public Layer3d(HawkScreen3d attachedScreen, CameraConfig cameraConfig) {
			super(attachedScreen);
			this.cameraConfig = cameraConfig;
		}

		@Override
		public void render(PoseStack poseStack, Vec2i mousePos, float partialTick) {
			final var prevMatrices = this.camera.setupRenderMatrices();
			render3d(this.camera, partialTick);
			prevMatrices.restore();
		}

		public void setup3d(OrbitCamera camera, float partialTick) {
		}

		public abstract void render3d(OrbitCamera.Cached camera, float partialTick);

		public void onMoved(Vec3 displacement) {
		}

		public OrbitCamera.Cached getCullingCamera() {
			return this.cullingCamera != null ? this.cullingCamera : this.camera;
		}
	}

	protected HawkScreen3d(Component component, Screen previousScreen, OrbitCamera camera,
			double scrollMin, double scrollMax) {
		super(component, previousScreen);
		this.camera = camera;
		this.scrollMin = scrollMin;
		this.scrollMax = scrollMax;

		this.camera.pitch.set(Math.PI / 8);
		this.camera.yaw.set(Math.PI / 8);
		this.camera.scale.set((scrollMin + scrollMax) / 2.0);
		this.camera.scale.target = (scrollMin + scrollMax) / 2.0;
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
		this.camera.yaw.target = this.camera.yaw.target + horiz.x * 0.005;
		var desiredPitch = this.camera.pitch.target + horiz.y * 0.005;
		var actualPitch = Mth.clamp(desiredPitch, -Math.PI / 2, Math.PI / 2);
		this.camera.pitch.target = actualPitch;
	}

	public void moveCamera(Vec2 horiz, double vert, boolean invert) {
		final var partialTick = this.client.getFrameTime();
		final var dragScale = this.camera.scale.get(partialTick) * (this.camera.metersPerUnit / 1e12) * 0.0035;

		horiz = invert && this.camera.pitch.get(partialTick) < 0 ? horiz.withY(-horiz.y) : horiz;
		final var offset = new Vec3(horiz.x, 0, horiz.y)
				.rotateY(-this.camera.yaw.get(partialTick))
				.add(0, vert, 0).mul(dragScale);
		if (offset.length() > 0) {
			this.camera.focus.target = this.camera.focus.target.add(offset);
			onMoved(offset);
		}
	}

	@Override
	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		if (scrollDelta > 0) {
			var prevTarget = this.camera.scale.target;
			this.camera.scale.target = Math.max(prevTarget / scrollMultiplier, this.scrollMin);
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.target;
			this.camera.scale.target = Math.min(prevTarget * scrollMultiplier, this.scrollMax);
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
		if (keypress.keyCode == GLFW.GLFW_KEY_F3 && keypress.hasModifiers(GLFW.GLFW_MOD_CONTROL)) {
			Minecraft.getInstance().reloadResourcePacks();
			return true;
		}

		if (keypress.hasModifiers(GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)) {
			if (keypress.keyCode == GLFW.GLFW_KEY_R) {
				GameRendererAccessor.reloadModShaders();
				return true;
			} else if (keypress.keyCode == GLFW.GLFW_KEY_F) {
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

	private void renderCameraFrustum(OrbitCamera.Cached camera, CachedCamera.FrustumCorners frustum, Color color) {
		if (frustum == null)
			return;

		final var builder = BufferRenderer.IMMEDIATE_BUILDER
				.beginGeneric(PrimitiveType.LINES, BufferLayout.POSITION_COLOR_NORMAL);

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
		up += this.isUpPressed ? speed : 0;
		up += this.isDownPressed ? -speed : 0;
		moveCamera(Vec2.from(right, forward), up, false);

		double rotate = 0;
		rotate += this.isRotateCWPressed ? rotateSpeed : 0;
		rotate += this.isRotateCCWPressed ? -rotateSpeed : 0;
		rotateCamera(Vec2.from(rotate, 0));

		this.camera.tick();
	}

	public abstract OrbitCamera.Cached setupCamera(CameraConfig config, float partialTick);

	public void onMoved(Vec3 displacement) {
		forEach3dLayer(layer -> layer.onMoved(displacement));
	}

	private static CameraConfig getDebugCameraConfig() {
		return new CameraConfig(0.01, true, 1e6, true);
	}

	@Override
	public void renderScreenPreLayers(PoseStack poseStack, Vec2i mousePos, float partialTick) {
		forEach3dLayer(layer -> {
			layer.setup3d(this.camera, partialTick);
			layer.lastCamera = layer.camera;
			layer.camera = setupCamera(layer.cameraConfig, partialTick);
			if (layer.lastCamera == null)
				layer.lastCamera = layer.camera;
		});
		super.renderScreenPreLayers(poseStack, mousePos, partialTick);
	}

	@Override
	public void renderScreenPostLayers(PoseStack poseStack, Vec2i mousePos, float partialTick) {
		super.renderScreenPostLayers(poseStack, mousePos, partialTick);
		final var debugCamera = setupCamera(getDebugCameraConfig(), partialTick);

		final var prevMatrices = debugCamera.setupRenderMatrices();
		forEach3dLayer(layer -> {
			renderCameraFrustum(debugCamera, layer.frustumPoints, Color.YELLOW);
			renderCameraFrustum(debugCamera, layer.cullingFrustumPoints, Color.CYAN);
		});
		prevMatrices.restore();
	}

}
