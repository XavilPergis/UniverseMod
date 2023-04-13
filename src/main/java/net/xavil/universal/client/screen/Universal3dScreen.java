package net.xavil.universal.client.screen;

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
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.util.Option;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec3;

public abstract class Universal3dScreen extends UniversalScreen {

	protected boolean isForwardPressed = false, isBackwardPressed = false,
			isLeftPressed = false, isRightPressed = false,
			isUpPressed = false, isDownPressed = false;
	public double scrollMultiplier = 1.2;
	public double scrollMin, scrollMax;

	public final OrbitCamera camera;
	private OrbitCamera.Cached lastCamera = null;

	// debug info
	private Vec3[] frustumPoints = null;
	private Vec3[] cullingFrustumPoints = null;
	private OrbitCamera.Cached cullingCamera = null;

	public static abstract class Layer3d extends Layer2d {
		private OrbitCamera.Cached camera;

		public Layer3d(Universal3dScreen attachedScreen) {
			super(attachedScreen);
		}

		@Override
		public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
			final var prevMatrices = this.camera.setupRenderMatrices();
			render3d(this.camera, partialTick);
			prevMatrices.restore();
		}

		public abstract void render3d(OrbitCamera.Cached camera, float partialTick);
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
		this.camera.scale.setTarget((scrollMin + scrollMax) / 2.0);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy))
			return true;

		this.setDragging(true);

		final var partialTick = this.client.getFrameTime();
		final var dragScale = this.camera.scale.get(partialTick) * this.camera.renderScaleFactor * 0.0035;

		if (button == 2) {
			var realDy = this.camera.pitch.get(partialTick) < 0 ? -dy : dy;
			var offset = Vec3.from(dx, 0, realDy).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
			onMoved(offset);
		} else if (button == 1) {
			var offset = Vec3.from(0, dragScale * dy, 0);
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
			onMoved(offset);
		} else if (button == 0) {
			this.camera.yaw.setTarget(this.camera.yaw.getTarget() + dx * 0.005);
			var desiredPitch = this.camera.pitch.getTarget() + dy * 0.005;
			var actualPitch = Mth.clamp(desiredPitch, -Math.PI / 2, Math.PI / 2);
			this.camera.pitch.setTarget(actualPitch);
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDelta))
			return true;

		if (scrollDelta > 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.max(prevTarget / scrollMultiplier, this.scrollMin));
			return true;
		} else if (scrollDelta < 0) {
			var prevTarget = this.camera.scale.getTarget();
			this.camera.scale.setTarget(Math.min(prevTarget * scrollMultiplier, this.scrollMax));
			return true;
		}

		return false;
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
				// capture/hide camera frustum
				if (this.frustumPoints != null) {
					this.frustumPoints = null;
				} else {
					getLastCamera().ifSome(camera -> this.frustumPoints = captureCameraFrustum(camera));
				}
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_C) {
				// debug culling
				if (this.cullingCamera != null) {
					this.cullingCamera = null;
					this.cullingFrustumPoints = null;
				} else {
					getLastCamera().ifSome(camera -> {
						this.cullingCamera = camera;
						this.cullingFrustumPoints = captureCameraFrustum(camera);
					});
				}
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
		}

		return false;
	}

	public final OrbitCamera.Cached getCullingCamera(OrbitCamera.Cached camera) {
		return this.cullingCamera != null ? this.cullingCamera : camera;
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
		}

		return false;
	}

	@Override
	public void tick() {
		super.tick();

		double forward = 0, right = 0, up = 0;
		double speed = 25;
		forward += this.isForwardPressed ? speed : 0;
		forward += this.isBackwardPressed ? -speed : 0;
		right += this.isLeftPressed ? speed : 0;
		right += this.isRightPressed ? -speed : 0;
		up += this.isUpPressed ? speed : 0;
		up += this.isDownPressed ? -speed : 0;

		// TODO: consolidate with the logic in mouseDragged()?
		final var partialTick = this.client.getFrameTime();
		final var dragScale = this.camera.scale.get(partialTick) * this.camera.renderScaleFactor * 0.0035;

		var offset = Vec3.from(right, 0, forward).rotateY(-this.camera.yaw.get(partialTick)).add(0, up, 0)
				.mul(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));

		if (forward != 0 || right != 0 || up != 0) {
			onMoved(offset);
		}

		this.camera.tick();
	}

	public abstract OrbitCamera.Cached setupCamera(float partialTick);

	public void onMoved(Vec3 displacement) {
	}

	// public void render3d(OrbitCamera.Cached camera, float partialTick) {
	// }

	// public void render2d(PoseStack poseStack, float partialTick) {
	// }

	public Option<OrbitCamera.Cached> getLastCamera() {
		return Option.fromNullable(this.lastCamera);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		final var partialTick = this.client.getFrameTime();
		final var camera = setupCamera(partialTick);
		this.lastCamera = camera;
		this.layers.forEach(layer -> {
			if (layer instanceof Layer3d layer3d)
				layer3d.camera = camera;
		});
		super.render(poseStack, mouseX, mouseY, tickDelta);

		final var prevMatrices = camera.setupRenderMatrices();
		renderCameraFrustum(camera, this.frustumPoints, Color.YELLOW);
		renderCameraFrustum(camera, this.cullingFrustumPoints, Color.CYAN);
		prevMatrices.restore();
	}

}
