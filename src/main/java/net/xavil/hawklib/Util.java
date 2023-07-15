package net.xavil.hawklib;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Util {

	private Util() {}

	public static <T> CompletableFuture<T> makeSupplyFuture(boolean isAsync, Supplier<T> supplier) {
		return isAsync ? CompletableFuture.supplyAsync(supplier) : CompletableFuture.completedFuture(supplier.get());
	}

	public static <T, U> CompletableFuture<U> makeApplyFuture(boolean isAsync, CompletableFuture<T> future, Function<T, U> function) {
		return isAsync ? future.thenApplyAsync(function) : future.thenApply(function);
	}

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

	public static String escapeMinus(int n) {
		return n >= 0 ? "" + n : "m" + -n;
	}
	
}
