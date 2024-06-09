package net.xavil.hawklib.tuple;

import java.util.Objects;

import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class Tuple3<A, B, C> implements Hashable {

    public final A a;
    public final B b;
    public final C c;

    public Tuple3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple3<?, ?, ?> other) {
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
        if (this.a instanceof Hashable ha
                && this.b instanceof Hashable hb
                && this.c instanceof Hashable hc) {
            hasher.append(ha);
            hasher.append(hb);
            hasher.append(hc);
        } else {
            hasher.appendInt(this.a.hashCode());
            hasher.appendInt(this.b.hashCode());
            hasher.appendInt(this.c.hashCode());
        }
    }

}
