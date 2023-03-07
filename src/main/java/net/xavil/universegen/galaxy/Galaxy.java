package net.xavil.universegen.galaxy;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.xavil.util.math.Vec3i;

public class Galaxy {
	
	public final Map<Vec3i, GalaxySector> sectors = new Object2ObjectOpenHashMap<>();

}
