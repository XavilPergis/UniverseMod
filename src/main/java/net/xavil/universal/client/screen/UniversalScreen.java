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
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec2i;

public abstract class UniversalScreen extends Screen {

	protected final Minecraft client = Minecraft.getInstance();
	protected final @Nullable Screen previousScreen;
	protected final Disposable.Multi disposer = new Disposable.Multi();
	public final Blackboard blackboard = new Blackboard();

	protected final MutableList<Layer2d> layers = new Vector<>();

	public static abstract class Layer2d implements Disposable {
		protected final Minecraft client = Minecraft.getInstance();
		public final UniversalScreen attachedScreen;
		public final Disposable.Multi disposer = new Disposable.Multi();

		public Layer2d(UniversalScreen attachedScreen) {
			this.attachedScreen = attachedScreen;
		}

		@Override
		public void dispose() {
			this.disposer.dispose();
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

		public final <T> Option<T> getBlackboard(Blackboard.Key<T> key) {
			return this.attachedScreen.blackboard.get(key);
		}

		public final <T> Option<T> insertBlackboard(Blackboard.Key<T> key, T value) {
			return this.attachedScreen.blackboard.insert(key, value);
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
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		final var wasDragging = this.isDragging();
		if (super.mouseReleased(mouseX, mouseY, button))
			return true;
		if (!wasDragging) {
			final var mousePos = Vec2.from(mouseX, mouseY);
			return dispatchEvent(layer -> layer.handleClick(mousePos, button));
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return dispatchEvent(layer -> layer.handleKeypress(keyCode, scanCode, modifiers));
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		final var mousePos = Vec2i.from(mouseX, mouseY);
		this.layers.forEach(layer -> layer.render(poseStack, mousePos, partialTick));
		super.render(poseStack, mouseX, mouseY, partialTick);
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
		this.layers.forEach(layer -> layer.dispose());
		this.disposer.dispose();
	}

}
