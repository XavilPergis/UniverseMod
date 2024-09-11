package net.xavil.hawklib.client.camera;

import org.lwjgl.glfw.GLFW;

import net.xavil.hawklib.client.screen.HawkScreen.Keypress;

public final class FirstPersonCameraControls {
	public boolean isForwardPressed = false, isBackwardPressed = false,
			isLeftPressed = false, isRightPressed = false,
			isUpPressed = false, isDownPressed = false,
			isRotateCWPressed = false, isRotateCCWPressed = false;

	public boolean updateFromKeypress(Keypress keypress) {
		// TODO: key mappings
		if (keypress.keyCode == GLFW.GLFW_KEY_W) {
			this.isForwardPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_S) {
			this.isBackwardPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_A) {
			this.isLeftPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_D) {
			this.isRightPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_Q) {
			this.isDownPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_E) {
			this.isUpPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_Z) {
			this.isRotateCWPressed = keypress.action.isPress();
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_C) {
			this.isRotateCCWPressed = keypress.action.isPress();
			return true;
		}
		return false;
	}

}