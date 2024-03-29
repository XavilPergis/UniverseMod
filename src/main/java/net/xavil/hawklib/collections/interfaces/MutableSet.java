package net.xavil.hawklib.collections.interfaces;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.proxy.MutableSetProxy;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;

public interface MutableSet<T> extends MutableCollection, ImmutableSet<T> {

	void retain(Predicate<T> predicate);

	void extend(IntoIterator<T> elements);

	boolean insert(T value);

	boolean remove(T value);

	default Iterator<T> difference(ImmutableSet<T> other) {
		return this.iter().filter(item -> !other.contains(item));
	}

	@Override
	default MutableSet<T> hint(CollectionHint hint) {
		return this;
	}

	static <T> MutableSetProxy<T> proxy(@Nonnull Set<T> set) {
		return new MutableSetProxy<>(set);
	}

	static <T> MutableSet<T> hashSet() {
		return new MutableSetProxy<>(new ObjectOpenHashSet<>());
	}

	static <T> MutableSet<T> identityHashSet() {
		return new MutableSetProxy<>(Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	static <T> MutableSet<T> hashSet(MutableSet<T> set) {
		final var newSet = new MutableSetProxy<T>(new ObjectOpenHashSet<>());
		newSet.extend(set);
		return newSet;
	}

}
