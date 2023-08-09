package net.xavil.hawklib.client.screen;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.hawklib.collections.Blackboard;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;

public abstract class HawkScreen extends Screen {

	protected final Minecraft client = Minecraft.getInstance();
	protected final @Nullable Screen previousScreen;
	protected final Disposable.Multi disposer = new Disposable.Multi();
	public final Blackboard<String> blackboard = new Blackboard<>();

	protected final MutableList<Layer2d> layers = new Vector<>();

	public static abstract class Layer2d implements Disposable {
		protected final Minecraft client = Minecraft.getInstance();
		public final HawkScreen attachedScreen;
		public final Disposable.Multi disposer = new Disposable.Multi();

		public Layer2d(HawkScreen attachedScreen) {
			this.attachedScreen = attachedScreen;
		}

		@Override
		@OverridingMethodsMustInvokeSuper
		public void close() {
			this.disposer.close();
		}

		public void tick() {
		}

		public abstract void render(PoseStack poseStack, Vec2i mousePos, float partialTick);

		public boolean handleClick(Vec2 mousePos, int button) {
			return false;
		}

		public boolean handleKeypress(Keypress keypress) {
			return false;
		}

		public boolean handleScroll(double mouseX, double mouseY, double scrollDelta) {
			return false;
		}

		/**
		 * This method is used to avoid renderign the minecraft world when it cannot
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
		if (mouseReleased(mousePos, button))
			return true;
		if (!wasDragging) {
			return dispatchEvent(layer -> layer.handleClick(mousePos, button));
		}
		return false;
	}

	public boolean mouseReleased(Vec2 mousePos, int button) {
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
		if (dispatchEvent(layer -> layer.handleScroll(mouseX, mouseY, scrollDelta)))
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

	private static final RenderTexture.StaticDescriptor DESC = RenderTexture.StaticDescriptor.create(builder -> {
		builder.colorFormat = GlTexture.Format.RGBA16_FLOAT;
		builder.isDepthReadable = false;
		builder.depthFormat = GlTexture.Format.DEPTH_UNSPECIFIED;
	});

	@Override
	public void tick() {
		super.tick();
		this.layers.forEach(layer -> layer.tick());
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		GlManager.pushState();

		final var window = this.client.getWindow();

		// managed code: enter
		try (final var disposer = Disposable.scope()) {
			final var output = disposer.attach(RenderTexture
					.getTemporary(new Vec2i(window.getWidth(), window.getHeight()), DESC));
			output.framebuffer.bind();
			output.framebuffer.clear();

			final var mousePos = Vec2i.from(mouseX, mouseY);
			final var partialTick = this.client.getFrameTime();
			renderScreenPreLayers(poseStack, mousePos, partialTick);
			this.layers.forEach(layer -> layer.render(poseStack, mousePos, partialTick));
			renderScreenPostLayers(poseStack, mousePos, partialTick);

			final var mainTarget = new GlFramebuffer(this.client.getMainRenderTarget());
			// mainTarget.bind();
			mainTarget.enableAllColorAttachments();
			HawkRendering.doPostProcessing(mainTarget, output.colorTexture);
		}
		// managed code: exit

		GlManager.popState();
		super.render(poseStack, mouseX, mouseY, tickDelta);
	}

	public void renderScreenPreLayers(PoseStack poseStack, Vec2i mousePos, float partialTick) {
	}

	public void renderScreenPostLayers(PoseStack poseStack, Vec2i mousePos, float partialTick) {
	}

	/**
	 * Controls whether the client will render the world.
	 */
	public boolean shouldRenderWorld() {
		return this.layers.iter().all(layer -> !layer.clobbersScreen());
	}

	@Override
	public void onClose() {
		// NOTE: explicitly not calling super's onClose because we want to set the
		// screen to the previous screen instead of always setting it to null.
		this.client.setScreen(previousScreen);
		this.layers.forEach(layer -> layer.close());
		this.disposer.close();
	}

}
