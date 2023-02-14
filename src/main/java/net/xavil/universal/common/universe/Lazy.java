package net.xavil.universal.common.universe;

import java.util.function.Function;

import javax.annotation.Nullable;

public class Lazy<I, F> {

	private final I initial;
	private @Nullable F full;
	private final Function<I, F> evaluator;

	public @Nullable Runnable evaluationHook;

	public Lazy(I initial, Function<I, F> evaluator) {
		this.initial = initial;
		this.evaluator = evaluator;
	}

	public boolean hasFull() {
		return this.full != null;
	}

	public I getInitial() {
		return this.initial;
	}

	public F getFull() {
		if (this.full == null) {
			this.full = this.evaluator.apply(this.initial);
			if (this.evaluationHook != null)
				this.evaluationHook.run();
		}
		return this.full;
	}

}
