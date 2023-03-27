package net.xavil.universal.client.screen;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.xavil.util.math.Vec3;

public abstract class Universal3dScreen extends UniversalScreen {

	protected boolean isForwardPressed = false, isBackwardPressed = false, isLeftPressed = false,
			isRightPressed = false;
	public final OrbitCamera camera;
	public double scrollMultiplier = 1.2;
	public double scrollMin, scrollMax;

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

		final var partialTick = this.client.getFrameTime();
		final var dragScale = this.camera.scale.get(partialTick) * this.camera.renderScaleFactor * 0.0035;

		if (button == 2) {
			var realDy = this.camera.pitch.get(partialTick) < 0 ? -dy : dy;
			var offset = Vec3.from(dx, 0, realDy).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));
		} else if (button == 1) {
			this.camera.focus.setTarget(this.camera.focus.getTarget().add(0, dragScale * dy, 0));
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

		if (keyCode == GLFW.GLFW_KEY_R && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
			Minecraft.getInstance().reloadResourcePacks();
			return true;
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
		}

		return false;
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
		}

		return false;
	}

	@Override
	public void tick() {
		super.tick();
		this.camera.tick();

		double forward = 0, right = 0;
		double speed = 25;
		forward += this.isForwardPressed ? speed : 0;
		forward += this.isBackwardPressed ? -speed : 0;
		right += this.isLeftPressed ? speed : 0;
		right += this.isRightPressed ? -speed : 0;

		// TODO: consolidate with the logic in mouseDragged()?
		final var partialTick = this.client.getFrameTime();
		final var dragScale = this.camera.scale.get(partialTick) * this.camera.renderScaleFactor * 0.0035;

		var offset = Vec3.from(right, 0, forward).rotateY(-this.camera.yaw.get(partialTick)).mul(dragScale);
		this.camera.focus.setTarget(this.camera.focus.getTarget().add(offset));

		if (forward != 0 || right != 0) {
			onMoved(offset);
		}
	}

	@Override
	public boolean shouldRenderWorld() {
		return false;
	}

	public void drawBackground(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		RenderSystem.depthMask(false);
		fillGradient(poseStack, 0, 0, this.width, this.height, 0xff000000, 0xff000000);
		RenderSystem.depthMask(true);
	}

	public abstract OrbitCamera.Cached setupCamera(float partialTick);

	public void onMoved(Vec3 displacement) {}

	public void render3d(OrbitCamera.Cached camera, float partialTick) {
	}

	public void render2d(PoseStack poseStack, float partialTick) {
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		final var partialTick = this.client.getFrameTime();
		drawBackground(poseStack, mouseX, mouseY, partialTick);
		final var camera = setupCamera(partialTick);
		final var prevMatrices = camera.setupRenderMatrices();
		render3d(camera, partialTick);
		prevMatrices.restore();
		render2d(poseStack, partialTick);
		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

}
