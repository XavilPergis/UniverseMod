package net.xavil.universal.client.flexible;

import java.util.OptionalInt;

import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;

import net.xavil.util.collections.interfaces.ImmutableMap;

public final class FlexibleFramebuffer {

	public record FramebufferFormat(boolean multisample,
			ImmutableMap<Integer, Integer> colorFormats,
			OptionalInt depthFormat) {
	}

}
