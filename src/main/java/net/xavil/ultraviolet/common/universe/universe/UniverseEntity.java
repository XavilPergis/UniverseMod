package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.hawklib.math.Quat;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;

public class UniverseEntity {

    public UniversePosition position = UniversePosition.ZERO;
    public Quat orientation = Quat.IDENTITY;

}
