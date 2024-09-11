package net.xavil.ultraviolet.common.universe.id;

import net.xavil.hawklib.math.Quat;

public final class UniverseFrame {

    public final UniversePosition position;
    public final Quat orientation;

    public UniverseFrame(UniversePosition position, Quat orientation) {
        this.position = position;
        this.orientation = orientation;
    }

}
