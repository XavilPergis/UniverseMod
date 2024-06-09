package net.xavil.hawklib.tuple;

import java.util.Objects;

import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class Tuple2<A, B> implements Hashable {

    public final A a;
    public final B b;

    public Tuple2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple2<?, ?> other) {
            return Objects.equals(this.a, other.a)
                    && Objects.equals(this.b, other.b);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", this.a, this.b);
    }

    @Override
    public int hashCode() {
        return FastHasher.hashToInt(this);
    }

    @Override
    public void appendHash(Hasher hasher) {
        if (this.a instanceof Hashable ha && this.b instanceof Hashable hb) {
            hasher.append(ha);
            hasher.append(hb);
        } else {
            hasher.appendInt(this.a.hashCode());
            hasher.appendInt(this.b.hashCode());
        }
    }

}
