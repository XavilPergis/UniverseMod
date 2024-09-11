package net.xavil.ultraviolet.common.universe.id;

import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public class UniversePosition {
    // Q110.18 for each axis. position is in meters.
    // this allows us to represent positions as far out as about 12 million times
    // the radius of the observable universe, with an accuracy of about 3
    // micrometers.

    public static final UniversePosition ZERO = new UniversePosition(0, 0, 0, 0, 0, 0);

    // signed 128 bit number for each axis, 2s complement.
    public final long xh, xl;
    public final long yh, yl;
    public final long zh, zl;

    public static final int TOTAL_BITS = 128;
    public static final int INTEGER_BITS = 110;
    public static final int FRACTIONAL_BITS = TOTAL_BITS - INTEGER_BITS;

    private static final double M_PER_LO = 0x1p-18, LO_PER_M = 1.0 / M_PER_LO;
    // 1 long limit of lo = 1 hi
    private static final double M_PER_HI = 0x1p-18 * 0x1p+64, HI_PER_M = 1.0 / M_PER_HI;

    public UniversePosition(long xh, long xl, long yh, long yl, long zh, long zl) {
        this.xl = xl;
        this.xh = xh;
        this.yl = yl;
        this.yh = yh;
        this.zl = zl;
        this.zh = zh;
    }

    public static UniversePosition from(Vec3Access p, double scaleFactor) {
        final double xf = p.x() * (scaleFactor * HI_PER_M);
        final double yf = p.y() * (scaleFactor * HI_PER_M);
        final double zf = p.z() * (scaleFactor * HI_PER_M);
        final long xh = (long) Math.floor(xf);
        final long yh = (long) Math.floor(yf);
        final long zh = (long) Math.floor(zf);
        final long xl = makeLoBits(xf);
        final long yl = makeLoBits(yf);
        final long zl = makeLoBits(zf);
        return new UniversePosition(xh, xl, yh, yl, zh, zl);
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

    private static double reconstructFloat(long hi, long lo, double unitsPerMeter) {
        // i know this looks kind of insane, but it does work out.
        // treating the unsigned lo bits as if they were two's complement encoded, small
        // negative numbers like -0.1234 get encoded as -1:-1234. Incrementing the hi
        // number by 1 and then using the negative lo bits works out the same as just
        // adding the scaled hi and lo, just with more accuracy.
        if (lo < 0)
            hi += 1;
        return hi * (unitsPerMeter * M_PER_HI) + ((double) lo) * (unitsPerMeter * M_PER_LO);
    }

    public Vec3 relativeTo(UniversePosition anchor) {
        return relativeTo(anchor, 1);
    }

    public Vec3 relativeTo(UniversePosition anchor, double unitsPerMeter) {
        final var rel = this.sub(anchor);
        final var x = reconstructFloat(rel.xh, rel.xl, unitsPerMeter);
        final var y = reconstructFloat(rel.yh, rel.yl, unitsPerMeter);
        final var z = reconstructFloat(rel.zh, rel.zl, unitsPerMeter);
        return new Vec3(x, y, z);
    }

    public UniversePosition add(UniversePosition other) {
        // @formatter:off
        long xl = this.xl + other.xl, xh = this.xh + other.xh;
        long yl = this.yl + other.yl, yh = this.yh + other.yh;
        long zl = this.zl + other.zl, zh = this.zh + other.zh;
        if (xl + Long.MIN_VALUE < this.xl + Long.MIN_VALUE) xh += 1;
        if (yl + Long.MIN_VALUE < this.yl + Long.MIN_VALUE) yh += 1;
        if (zl + Long.MIN_VALUE < this.zl + Long.MIN_VALUE) zh += 1;
        // @formatter:on
        return new UniversePosition(xh, xl, yh, yl, zh, zl);
    }

    public UniversePosition neg() {
        // @formatter:off
        long xl = ~this.xl + 1L, xh = ~this.xh;
        long yl = ~this.yl + 1L, yh = ~this.yh;
        long zl = ~this.zl + 1L, zh = ~this.zh;
        // don't handle overflow on hi parts, its how 2s complement negation works.
        if (xl + Long.MIN_VALUE < ~this.xl + Long.MIN_VALUE) xh += 1;
        if (yl + Long.MIN_VALUE < ~this.yl + Long.MIN_VALUE) yh += 1;
        if (zl + Long.MIN_VALUE < ~this.zl + Long.MIN_VALUE) zh += 1;
        // @formatter:on
        return new UniversePosition(xh, xl, yh, yl, zh, zl);
    }

    public UniversePosition sub(UniversePosition other) {
        // @formatter:off
        long olx = ~other.xl + 1L, ohx = ~other.xh;
        long oly = ~other.yl + 1L, ohy = ~other.yh;
        long olz = ~other.zl + 1L, ohz = ~other.zh;
        // don't handle overflow on hi parts, its how 2s complement negation works.
        if (olx + Long.MIN_VALUE < ~other.xl + Long.MIN_VALUE) ohx += 1;
        if (oly + Long.MIN_VALUE < ~other.yl + Long.MIN_VALUE) ohy += 1;
        if (olz + Long.MIN_VALUE < ~other.zl + Long.MIN_VALUE) ohz += 1;

        long xl = this.xl + olx, xh = this.xh + ohx;
        long yl = this.yl + oly, yh = this.yh + ohy;
        long zl = this.zl + olz, zh = this.zh + ohz;
        if (xl + Long.MIN_VALUE < this.xl + Long.MIN_VALUE) xh += 1;
        if (yl + Long.MIN_VALUE < this.yl + Long.MIN_VALUE) yh += 1;
        if (zl + Long.MIN_VALUE < this.zl + Long.MIN_VALUE) zh += 1;
        // @formatter:on
        return new UniversePosition(xh, xl, yh, yl, zh, zl);
    }

    public UniversePosition add(Vec3Access offset, double scaleFactor) {
        return add(from(offset, scaleFactor));
    }

    public static UniversePosition lerp(double t, UniversePosition a, UniversePosition b) {
        return a.add(from(b.sub(a).relativeTo(UniversePosition.ZERO).mul(t), 1));
    }

}
