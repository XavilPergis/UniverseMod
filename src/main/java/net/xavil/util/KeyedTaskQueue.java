package net.xavil.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class KeyedTaskQueue<K, V> {

	public static final ExecutorService DEFAULT_POOL = Executors.newWorkStealingPool();

	private final ExecutorService executor;
	private final Map<K, Future<?>> tasks = new Object2ObjectOpenHashMap<>();
	private final Queue<Pair<K, V>> completed = new ConcurrentLinkedQueue<>();

	public KeyedTaskQueue(ExecutorService executor) {
		this.executor = executor;
	}

	public KeyedTaskQueue() {
		this(DEFAULT_POOL);
	}

	public synchronized void enqueue(K key, Function<K, V> generatorTask, Runnable completionCallback) {
		if (!this.tasks.containsKey(key))
			return;
		this.tasks.put(key, this.executor.submit(() -> {
			this.completed.add(new Pair<>(key, generatorTask.apply(key)));
			if (completionCallback != null)
				completionCallback.run();
		}));
	}

	public synchronized void cancel(K key) {
		final var task = this.tasks.get(key);
		if (task != null)
			task.cancel(true);
	}

	public synchronized Stream<Pair<K, V>> drain() {
		final var res = new ArrayList<Pair<K, V>>(this.completed.size());
		while (!this.completed.isEmpty()) {
			final var result = this.completed.poll();
			this.tasks.remove(result.getFirst());
			res.add(result);
		}
		return res.stream();
	}

}
