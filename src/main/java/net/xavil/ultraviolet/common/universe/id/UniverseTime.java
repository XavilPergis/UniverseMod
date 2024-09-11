package net.xavil.ultraviolet.common.universe.id;

public final class UniverseTime {
    // Q90.38
    public final long hi, lo;

    private static final double S_PER_LO = 0x1p-38, LO_PER_S = 1.0 / S_PER_LO;
    private static final double S_PER_HI = 0x1p-38 * 0x1p+64, HI_PER_S = 1.0 / S_PER_HI;

    public static final int TOTAL_BITS = 128;
    public static final int INTEGER_BITS = 90;
    public static final int FRACTIONAL_BITS = TOTAL_BITS - INTEGER_BITS;

    public UniverseTime(long hi, long lo) {
        this.hi = hi;
        this.lo = lo;
    }

    public UniverseTime add(UniverseTime other) {
        long lo = this.lo + other.lo, hi = this.hi + other.hi;
        if (lo + Long.MIN_VALUE < this.lo + Long.MIN_VALUE)
            hi += 1;
        return new UniverseTime(hi, lo);
    }

    public UniverseTime neg() {
        long lo = ~this.lo + 1L, hi = ~this.hi;
        if (lo + Long.MIN_VALUE < ~this.lo + Long.MIN_VALUE)
            hi += 1;
        return new UniverseTime(hi, lo);
    }

    public UniverseTime sub(UniverseTime other) {
        long olo = ~other.lo + 1L, ohi = ~other.hi;
        if (olo + Long.MIN_VALUE < ~other.lo + Long.MIN_VALUE)
            ohi += 1;
        long lo = this.lo + olo, hi = this.hi + ohi;
        if (lo + Long.MIN_VALUE < this.lo + Long.MIN_VALUE)
            hi += 1;
        return new UniverseTime(hi, lo);
    }

    private static double reconstructFloat(long hi, long lo, double unitsPerSecond) {
        if (lo < 0)
            hi += 1;
        return hi * (unitsPerSecond * S_PER_HI) + ((double) lo) * (unitsPerSecond * S_PER_LO);
    }

    private static long makeLoBits(double value) {
        final double pm = value - Math.floor(value);
        final double nm = value - Math.ceil(value);
        if (pm < 0.5) {
            return (long) (0x1p+64 * pm);
        } else {
            final var res = (long) (0x1p+64 * nm);
            // fuck
            return res == 0 ? -1 : res;
        }
    }

    public double relativeTo(UniverseTime anchor, double unitsPerSecond) {
        final var rel = this.sub(anchor);
        return reconstructFloat(rel.hi, rel.lo, unitsPerSecond);
    }

    public static UniverseTime from(double t, double scaleFactor) {
        final double f = t * (scaleFactor * HI_PER_S);
        final long hi = (long) Math.floor(f);
        final long lo = makeLoBits(f);
        return new UniverseTime(hi, lo);
    }

}
