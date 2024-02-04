package net.xavil.hawklib;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

import net.xavil.hawklib.collections.interfaces.ImmutableListFloat;

public final class Util {

	private Util() {}

	public static final Executor ASYNC_POOL = new ForkJoinPool(1);

	public static <T> CompletableFuture<T> makeSupplyFuture(boolean isAsync, Supplier<T> supplier) {
		return isAsync ? CompletableFuture.supplyAsync(supplier, ASYNC_POOL) : CompletableFuture.completedFuture(supplier.get());
	}

	public static <T, U> CompletableFuture<U> makeApplyFuture(boolean isAsync, CompletableFuture<T> future, Function<T, U> function) {
		return isAsync ? future.thenApplyAsync(function, ASYNC_POOL) : future.thenApply(function);
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

	public static int binarySearch(ImmutableListFloat list, float value) {
		return binarySearch(list, 0, list.size(), value);
	}

	public static int binarySearch(ImmutableListFloat list, int start, int end, float value) {
		int boundL = start, boundH = end;
		while (boundL < boundH && boundH >= start) {
			final var midpointIndex = (boundL + boundH) / 2;
			final var midpointValue = list.get(midpointIndex);

			if (midpointValue < value) {
				boundL = midpointIndex + 1;
			} else if (midpointValue > value) {
				boundH = midpointIndex;
			} else {
				boundL = boundH = midpointIndex;
			}
		}

		final var listValue = list.get(boundL);
		if (value > listValue)
			return boundL + 1;

		return boundL;
	}
	
}
