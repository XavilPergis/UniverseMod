package net.xavil.universal.client.screen;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.collections.Blackboard;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.math.matrices.Vec2;
import net.xavil.util.math.matrices.Vec2i;

public abstract class UniversalScreen extends Screen {

	protected final Minecraft client = Minecraft.getInstance();
	protected final @Nullable Screen previousScreen;
	protected final Disposable.Multi disposer = new Disposable.Multi();
	public final Blackboard<String> blackboard = new Blackboard<>();

	protected final MutableList<Layer2d> layers = new Vector<>();

	public static abstract class Layer2d implements Disposable {
		protected final Minecraft client = Minecraft.getInstance();
		public final UniversalScreen attachedScreen;
		public final Disposable.Multi disposer = new Disposable.Multi();

		public Layer2d(UniversalScreen attachedScreen) {
			this.attachedScreen = attachedScreen;
		}

		@Override
		public void close() {
			this.disposer.close();
		}

		public abstract void render(PoseStack poseStack, Vec2i mousePos, float partialTick);

		public boolean handleClick(Vec2 mousePos, int button) {
			return false;
		}

		public boolean handleKeypress(int keyCode, int scanCode, int modifiers) {
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

		public final <T> Option<T> getBlackboard(Blackboard.Key<String, T> key) {
			return this.attachedScreen.blackboard.get(key);
		}

		public final <T> Option<T> insertBlackboard(Blackboard.Key<String, T> key, T value) {
			return this.attachedScreen.blackboard.insert(key, value);
		}

		public final <T> Option<T> removeBlackboard(Blackboard.Key<String, T> key) {
			return this.attachedScreen.blackboard.remove(key);
		}
	}

	protected UniversalScreen(Component component, @Nullable Screen previousScreen) {
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
		return mouseScrolled(Vec2.from(mouseX, mouseY), scrollDelta);
	}

	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return dispatchEvent(layer -> layer.handleKeypress(keyCode, scanCode, modifiers));
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
		final var mousePos = Vec2i.from(mouseX, mouseY);
		final var partialTick = this.client.getFrameTime();
		this.layers.forEach(layer -> layer.render(poseStack, mousePos, partialTick));
		super.render(poseStack, mouseX, mouseY, tickDelta);
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
