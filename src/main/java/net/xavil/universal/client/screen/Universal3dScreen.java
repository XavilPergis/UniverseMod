package net.xavil.universal.client.screen;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec2i;
import net.xavil.util.math.Vec3;

public abstract class Universal3dScreen extends UniversalScreen {

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

		private Vec3[] frustumPoints = null;
		private Vec3[] cullingFrustumPoints = null;
		private OrbitCamera.Cached cullingCamera = null;

		public Layer3d(Universal3dScreen attachedScreen, CameraConfig cameraConfig) {
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

	protected Universal3dScreen(Component component, Screen previousScreen, OrbitCamera camera,
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
			moveCamera(delta, 0 , true);
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
		final var offset = Vec3.from(horiz.x, 0, horiz.y)
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
			layer.frustumPoints = captureCameraFrustum(layer.lastCamera);
		});
	}

	private void debugCullingCamera() {
		forEach3dLayer(layer -> {
			if (layer.lastCamera == null)
				return;
			layer.cullingCamera = layer.lastCamera;
			layer.cullingFrustumPoints = captureCameraFrustum(layer.lastCamera);
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
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		if (keyCode == GLFW.GLFW_KEY_F3 && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			Minecraft.getInstance().reloadResourcePacks();
			return true;
		}

		if (((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) && ((modifiers & GLFW.GLFW_MOD_ALT) != 0)) {
			if (keyCode == GLFW.GLFW_KEY_F) {
				debugCameraFrustum();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_C) {
				debugCullingCamera();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_V) {
				clearDebug();
				return true;
			}
		}

		// TODO: key mappings
		if (keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_Q) {
			this.isDownPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_E) {
			this.isUpPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_Z) {
			this.isRotateCWPressed = true;
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_C) {
			this.isRotateCCWPressed = true;
			return true;
		}

		return false;
	}

	private Vec3[] captureCameraFrustum(OrbitCamera.Cached camera) {
		final var frustumPoints = new Vec3[8];
		// @formatter:off
		int i = 0;
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1, -1, -1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1, -1,  1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1,  1, -1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from(-1,  1,  1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1, -1, -1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1, -1,  1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1,  1, -1));
		frustumPoints[i++] = camera.ndcToWorld(Vec3.from( 1,  1,  1));
		// @formatter:on
		return frustumPoints;
	}

	private void renderCameraFrustum(OrbitCamera.Cached camera, Vec3[] frustumPoints, Color color) {
		if (frustumPoints == null)
			return;

		int i = 0;
		final var nnn = frustumPoints[i++];
		final var nnp = frustumPoints[i++];
		final var npn = frustumPoints[i++];
		final var npp = frustumPoints[i++];
		final var pnn = frustumPoints[i++];
		final var pnp = frustumPoints[i++];
		final var ppn = frustumPoints[i++];
		final var ppp = frustumPoints[i++];

		final var builder = BufferRenderer.immediateBuilder();
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		// near
		RenderHelper.addLine(builder, camera, nnn, npn, color);
		RenderHelper.addLine(builder, camera, pnn, ppn, color);
		RenderHelper.addLine(builder, camera, nnn, pnn, color);
		RenderHelper.addLine(builder, camera, npn, ppn, color);
		// far
		RenderHelper.addLine(builder, camera, nnp, npp, color);
		RenderHelper.addLine(builder, camera, pnp, ppp, color);
		RenderHelper.addLine(builder, camera, nnp, pnp, color);
		RenderHelper.addLine(builder, camera, npp, ppp, color);
		// sides
		RenderHelper.addLine(builder, camera, nnn, nnp, color);
		RenderHelper.addLine(builder, camera, npn, npp, color);
		RenderHelper.addLine(builder, camera, pnn, pnp, color);
		RenderHelper.addLine(builder, camera, ppn, ppp, color);

		builder.end();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.lineWidth(2.0f);
		builder.draw(GameRenderer.getRendertypeLinesShader());

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
		return new CameraConfig(0.01, 1e6);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		final var partialTick = this.client.getFrameTime();
		forEach3dLayer(layer -> {
			layer.setup3d(this.camera, partialTick);
			layer.lastCamera = layer.camera;
			layer.camera = setupCamera(layer.cameraConfig, partialTick);
			if (layer.lastCamera == null)
				layer.lastCamera = layer.camera;
		});
		super.render(poseStack, mouseX, mouseY, tickDelta);

		final var debugCamera = setupCamera(getDebugCameraConfig(), partialTick);

		final var prevMatrices = debugCamera.setupRenderMatrices();
		forEach3dLayer(layer -> {
			renderCameraFrustum(debugCamera, layer.frustumPoints, Color.YELLOW);
			renderCameraFrustum(debugCamera, layer.cullingFrustumPoints, Color.CYAN);
		});
		prevMatrices.restore();
	}

}
