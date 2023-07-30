package net.xavil.ultraviolet.common.universe.system;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;

public class StarSystem {

	public final String name;
	public final Galaxy parentGalaxy;
	public final Vec3 pos;
	public CelestialNode rootNode;

	public StarSystem(String name, Galaxy parentGalaxy, Vec3 pos, CelestialNode rootNode) {
		this.name = name;
		this.parentGalaxy = parentGalaxy;
		this.pos = pos;
		this.rootNode = rootNode;
	}

}
