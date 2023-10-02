package net.xavil.ultraviolet.common.universe.system;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarSystem {

	public final String name;
	public final Galaxy parentGalaxy;
	public final Vec3 pos;
	public CelestialNode rootNode;
	private GalaxySector.ElementHolder systemInfo;

	public StarSystem(String name, Galaxy parentGalaxy, GalaxySector.ElementHolder systemInfo, CelestialNode rootNode) {
		this.name = name;
		this.parentGalaxy = parentGalaxy;
		this.systemInfo = systemInfo;
		this.pos = systemInfo.systemPosTm.xyz();
		this.rootNode = rootNode;
	}

	public void copySystemInfo(GalaxySector.ElementHolder info) {
		info.loadCopyOf(this.systemInfo);
	}

}
