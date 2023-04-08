package net.xavil.util;

public final class Util {

	private Util() {}

	public static long longMask(int start, int end) {
		final var mask1 = (1L << end) - 1;
		final var mask2 = ~((1L << start) - 1);
		return mask1 & mask2;
	}

	public static int intMask(int start, int end) {
		final var mask1 = (1 << end) - 1;
		final var mask2 = ~((1 << start) - 1);
		return mask1 & mask2;
	}

	public static void tryNotify(Object notifier) {
		if (notifier != null)
			synchronized (notifier) {
				notifier.notifyAll();
			}
	}
	
}
