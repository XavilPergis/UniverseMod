package net.xavil.hawklib.client.gl;

import net.xavil.hawklib.math.ColorRgba;

public abstract sealed class ClearState {

	public static final DontCare DONT_CARE = new DontCare();
	public static final Keep KEEP = new Keep();

	public static SetFloat setFloat(float initialDepth) {
		final var state = new SetFloat(1);
		state.clearValue[0] = initialDepth;
		return state;
	}

	public static SetFloat setFloat(ColorRgba initialColor) {
		final var state = new SetFloat(4);
		state.load(initialColor);
		return state;
	}

	public static SetInt setInt(int initialStencil) {
		final var state = new SetInt(1);
		state.clearValue[0] = initialStencil;
		return state;
	}


	public static final class DontCare extends ClearState {
		private DontCare() {}
	}
	
	public static final class Keep extends ClearState {
		private Keep() {}
	}

	public static final class SetFloat extends ClearState {
		public final float[] clearValue;

		public SetFloat(int count) {
			validateClearValueCount(count);
			this.clearValue = new float[count];
		}

		public void load(ColorRgba clearValue) {
			this.clearValue[0] = clearValue.r();
			this.clearValue[1] = clearValue.g();
			this.clearValue[2] = clearValue.b();
			this.clearValue[3] = clearValue.a();
		}
	}

	public static final class SetInt extends ClearState {
		public final int[] clearValue;

		public SetInt(int count) {
			validateClearValueCount(count);
			this.clearValue = new int[count];
		}
	}

	private static void validateClearValueCount(int count) {
		if (count > 4) {
			throw new IllegalArgumentException(String.format(
				"%d clear values is more than maximum of 4.",
				count));
		}
	}

}
