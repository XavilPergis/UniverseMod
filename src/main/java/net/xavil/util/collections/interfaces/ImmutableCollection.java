package net.xavil.util.collections.interfaces;

public interface ImmutableCollection {

	int size();

	default boolean isEmpty() {
		return this.size() == 0;
	}

}
