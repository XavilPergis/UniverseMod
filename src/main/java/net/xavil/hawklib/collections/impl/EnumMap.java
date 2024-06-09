package net.xavil.hawklib.collections.impl;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class EnumMap<E extends Enum<E>, V> implements MutableMap<E, V> {

    private final Class<E> keyType;
    private final E[] keyValues;
    private final V[] values;
    private final boolean[] inhabited;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public EnumMap(Class<E> clazz) {
        this.keyType = clazz;
        this.keyValues = clazz.getEnumConstants();
        this.values = (V[]) new Object[this.keyValues.length];
        this.inhabited = new boolean[this.keyValues.length];
    }

    public EnumMap<E, V> populate(Function<E, V> supplier) {
        for (int i = 0; i < this.keyValues.length; ++i) {
            this.values[i] = supplier.apply(this.keyValues[i]);
        }
        Arrays.fill(this.inhabited, true);
        return this;
    }

    @Override
    public void clear() {
        Arrays.fill(this.inhabited, false);
        Arrays.fill(this.values, null);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Maybe<V> get(E key) {
        return this.inhabited[key.ordinal()] ? Maybe.some(this.values[key.ordinal()]) : Maybe.none();
    }

    @Override
    public V getOrNull(E key) {
        return this.inhabited[key.ordinal()] ? this.values[key.ordinal()] : null;
    }

    @Override
    public V getOrThrow(E key) {
        if (!this.inhabited[key.ordinal()])
            throw new IllegalArgumentException(String.format(
                    "Key '%s' does not exist in map",
                    key));
        return this.values[key.ordinal()];
    }

    @Override
    public Iterator<E> keys() {
        return new ArrayIterator<>(this.keyValues);
    }

    @Override
    public boolean insert(E key, V value) {
        final var old = this.inhabited[key.ordinal()];
        this.values[key.ordinal()] = value;
        this.inhabited[key.ordinal()] = true;
        return !old;
    }

    @Override
    public boolean remove(E key) {
        final var old = this.inhabited[key.ordinal()];
        this.inhabited[key.ordinal()] = false;
        this.values[key.ordinal()] = null;
        return old;
    }

    @Override
    public Maybe<V> insertAndGet(E key, V value) {
        if (this.inhabited[key.ordinal()]) {
            final var old = this.values[key.ordinal()];
            this.values[key.ordinal()] = value;
            return Maybe.some(old);
        } else {
            this.values[key.ordinal()] = value;
            this.inhabited[key.ordinal()] = true;
            return Maybe.none();
        }
    }

    @Override
    public Maybe<V> removeAndGet(E key) {
        if (!this.inhabited[key.ordinal()])
            return Maybe.none();
        final var old = this.values[key.ordinal()];
        this.inhabited[key.ordinal()] = false;
        this.values[key.ordinal()] = null;
        return Maybe.some(old);
    }

    @Override
    public void retain(BiPredicate<E, V> predicate) {
        for (int i = 0; i < this.keyValues.length; ++i) {
            if (!this.inhabited[i])
                continue;
            if (!predicate.test(this.keyValues[i], this.values[i])) {
                this.inhabited[i] = false;
                this.values[i] = null;
            }
        }
    }

    // NOTE: i cant actually implement a generic equals() with the current
    // ImmutableMap API, since we don't know the type of either the keys or values
    // of the other map, so we can't do things like `other.containsKey(...)`

}
