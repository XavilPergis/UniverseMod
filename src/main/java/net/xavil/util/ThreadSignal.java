package net.xavil.util;

import java.util.concurrent.CompletableFuture;

import com.mojang.datafixers.util.Unit;

public final class ThreadSignal {
	
	private final CompletableFuture<Unit> future = new CompletableFuture<>();

	public void signal() {
		this.future.complete(Unit.INSTANCE);
	}

	public void waitUntilSignalled() {
		this.future.join();
	}

}
