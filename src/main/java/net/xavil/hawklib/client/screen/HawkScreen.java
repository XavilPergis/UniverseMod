package net.xavil.hawklib.client.screen;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.hawklib.collections.Blackboard;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.ultraviolet.client.BloomEffect;
import net.xavil.ultraviolet.client.PostProcessing;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGpuTimers;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;

public abstract class HawkScreen extends Screen {

	protected final Minecraft client = Minecraft.getInstance();
	protected final @Nullable Screen previousScreen;
	public final Disposable.Multi disposer = new Disposable.Multi();
	public final Blackboard<String> blackboard = new Blackboard<>();

	protected final MutableList<Layer2d> layers = new Vector<>();
	protected final MutableList<Layer2d> overlays = new Vector<>();

	public static abstract class Layer2d implements Disposable {
		protected final Minecraft client = Minecraft.getInstance();
		public final HawkScreen attachedScreen;
		public final Disposable.Multi disposer = new Disposable.Multi();
		private boolean markedForRemoval = false;

		public Layer2d(HawkScreen attachedScreen) {
			this.attachedScreen = attachedScreen;
		}

		public void remove() {
			this.markedForRemoval = true;
		}

		@Override
		@OverridingMethodsMustInvokeSuper
		public void close() {
			this.disposer.close();
		}

		public void tick() {
		}

		public abstract void render(RenderContext ctx);

		public void renderPost(RenderTexture sceneTexture, RenderContext ctx) {
		}

		public boolean handleClick(Vec2 mousePos, int button) {
			return false;
		}

		public boolean handleKeypress(Keypress keypress) {
			return false;
		}

		public boolean handleScroll(Vec2 mousePos, double scrollDelta) {
			return false;
		}

		/**
		 * This method is used to avoid rendering the minecraft world when it cannot
		 * possibly be seen.
		 * 
		 * @return Whether this layer clobbers everything that has been previously
		 *         drawn.
		 */
		public boolean clobbersScreen() {
			return false;
		}

		public final <T> T getBlackboardOrDefault(Blackboard.Key<String, T> key) {
			return key.getOrDefault(this.attachedScreen.blackboard);
		}

		public final <T> Maybe<T> getBlackboard(Blackboard.Key<String, T> key) {
			return this.attachedScreen.blackboard.get(key);
		}

		public final <T> Maybe<T> insertBlackboard(Blackboard.Key<String, T> key, T value) {
			return this.attachedScreen.blackboard.insert(key, value);
		}

		public final <T> Maybe<T> removeBlackboard(Blackboard.Key<String, T> key) {
			return this.attachedScreen.blackboard.remove(key);
		}
	}

	protected HawkScreen(Component component, @Nullable Screen previousScreen) {
		super(component);
		this.previousScreen = previousScreen;
	}

	private boolean dispatchEvent(Predicate<Layer2d> predicate) {
		for (int i = this.layers.size() - 1; i >= 0; --i) {
			if (predicate.test(this.layers.get(i)))
				return true;
		}
		return false;
	}

	@Override
	public final boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		return mouseClicked(Vec2.from(mouseX, mouseY), button);
	}

	public boolean mouseClicked(Vec2 mousePos, int button) {
		return false;
	}

	@Override
	public final boolean mouseReleased(double mouseX, double mouseY, int button) {
		final var wasDragging = this.isDragging();
		if (super.mouseReleased(mouseX, mouseY, button))
			return true;
		final var mousePos = Vec2.from(mouseX, mouseY);
		if (mouseReleased(mousePos, button, wasDragging))
			return true;
		if (!wasDragging) {
			return dispatchEvent(layer -> layer.handleClick(mousePos, button));
		}
		return false;
	}

	public boolean mouseReleased(Vec2 mousePos, int button, boolean wasDragging) {
		return false;
	}

	@Override
	public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY))
			return true;
		return mouseDragged(Vec2.from(mouseX, mouseY), Vec2.from(dragX, dragY), button);
	}

	public boolean mouseDragged(Vec2 mousePos, Vec2 delta, int button) {
		return false;
	}

	@Override
	public final boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDelta))
			return true;
		if (dispatchEvent(layer -> layer.handleScroll(Vec2.from(mouseX, mouseY), scrollDelta)))
			return true;
		return mouseScrolled(Vec2.from(mouseX, mouseY), scrollDelta);
	}

	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		return false;
	}

	@Override
	public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		final var keypress = new Keypress(keyCode, scanCode, modifiers);

		if (keypress.hasModifiers(GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)) {
			if (keypress.keyCode == GLFW.GLFW_KEY_R) {
				GameRendererAccessor.reloadModShaders();
				return true;
			} else if (keypress.keyCode == GLFW.GLFW_KEY_F3) {
				this.client.reloadResourcePacks();
				return true;
			} else if (keypress.keyCode == GLFW.GLFW_KEY_Q) {
				final var overlayIndex = this.overlays.indexOf(layer -> layer instanceof ScreenLayerGpuTimers);
				if (overlayIndex >= 0) {
					this.overlays.remove(overlayIndex);
				} else {
					this.overlays.push(new ScreenLayerGpuTimers(this));
				}
				return true;
			}
		}

		if (keyPressed(keypress))
			return true;

		return dispatchEvent(layer -> layer.handleKeypress(keypress));
	}

	public static final class Keypress {
		public final int keyCode;
		public final int scanCode;
		public final int modifiers;

		public Keypress(int keyCode, int scanCode, int modifiers) {
			this.keyCode = keyCode;
			this.scanCode = scanCode;
			this.modifiers = modifiers;
		}

		public boolean hasModifiers(int flags) {
			return (this.modifiers & flags) == flags;
		}
	}

	public boolean keyPressed(Keypress keypress) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		this.layers.forEach(layer -> layer.tick());
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		GlManager.pushState();

		final var window = this.client.getWindow();
		final var windowSize = new Vec2i(window.getWidth(), window.getHeight());

		final var partialTick = this.client.getFrameTime();
		final var deltaTime = this.client.getDeltaFrameTime();

		final var mousePos = new Vec2(mouseX, mouseY);
		final var ctx = new RenderContext(poseStack, windowSize, mousePos, partialTick, deltaTime);

		final var disposer = Disposable.scope();
		try {
			ctx.currentTexture = RenderTexture.HDR_COLOR_DEPTH.acquireTemporary();
			ctx.currentTexture.framebuffer.clear();
			renderScreenPreLayers(ctx);
			for (final var layer : this.layers.iterable()) {
				ctx.currentTexture.framebuffer.bind();
				layer.render(ctx);
			}
			renderScreenPostLayers(ctx);
			for (final var layer : this.overlays.iterable()) {
				ctx.currentTexture.framebuffer.bind();
				layer.render(ctx);
			}

			final var sceneTexture = disposer.attach(RenderTexture.HDR_COLOR_DEPTH.acquireTemporary());
			ctx.currentTexture.framebuffer.copyTo(sceneTexture.framebuffer);

			for (final var layer : this.layers.iterable()) {
				ctx.currentTexture.framebuffer.bind();
				layer.renderPost(sceneTexture, ctx);
			}

			final var hdrPost = disposer.attach(RenderTexture.HDR_COLOR.acquireTemporary());
			BloomEffect.render(hdrPost.framebuffer, ctx.currentTexture.colorTexture);

			PostProcessing.runTonemappingPass(new RenderTexture(GlFramebuffer.getMainFramebuffer()),
					hdrPost.colorTexture);
		} finally {
			disposer.close();
			ctx.currentTexture.close();
		}

		this.layers.retain(layer -> !layer.markedForRemoval);
		this.overlays.retain(layer -> !layer.markedForRemoval);

		GlManager.popState();
		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

	public static final class RenderContext {
		public final PoseStack poseStack;
		public final Vec2i windowSize;
		public final Vec2 mousePos;
		public final float partialTick;
		public final float deltaTime;

		public RenderTexture currentTexture;

		public RenderContext(PoseStack poseStack, Vec2i windowSize, Vec2 mousePos, float partialTick, float deltaTime) {
			this.poseStack = poseStack;
			this.windowSize = windowSize;
			this.mousePos = mousePos;
			this.partialTick = partialTick;
			this.deltaTime = deltaTime;
		}

		public void replaceCurrentTexture(RenderTexture newTexture) {
			final var prevTexture = this.currentTexture;
			this.currentTexture = newTexture;
			prevTexture.close();
		}
	}

	public void renderScreenPreLayers(RenderContext ctx) {
	}

	public void renderScreenPostLayers(RenderContext ctx) {
	}

	/**
	 * Controls whether the client will render the world while this screen is.
	 */
	public boolean shouldRenderWorld() {
		return this.layers.iter().all(layer -> !layer.clobbersScreen())
				&& this.overlays.iter().all(layer -> !layer.clobbersScreen());
	}

	@Override
	public void onClose() {
		// NOTE: explicitly not calling super's onClose because we want to set the
		// screen to the previous screen instead of always setting it to null.
		this.client.setScreen(previousScreen);
		this.layers.forEach(layer -> layer.close());
		this.overlays.forEach(layer -> layer.close());
		this.disposer.close();
	}

}
