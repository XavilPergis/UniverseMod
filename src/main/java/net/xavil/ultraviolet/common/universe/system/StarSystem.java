package net.xavil.ultraviolet.common.universe.system;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;

public final class StarSystem {

	public final SystemId id;
	public final Galaxy parentGalaxy;
	public final UniversePosition position;
	public final String name;
	// relative to the parent galaxy.
	public final Quat orientation;
	public CelestialNode rootNode;
	private GalaxySector.ElementHolder systemInfo;

	public StarSystem(GalaxySectorId id,
			Galaxy parentGalaxy,
			GalaxySector.ElementHolder systemInfo,
			CelestialNode rootNode,
			Quat orientation,
			String name) {
		this.id = new SystemId(parentGalaxy.galaxyId, id);
		this.name = name;
		this.parentGalaxy = parentGalaxy;
		this.systemInfo = systemInfo;
		this.orientation = orientation;
		this.rootNode = rootNode;
		this.position = parentGalaxy.position.add(systemInfo.systemPosTm, Units.u_PER_Tu);
		this.rootNode.setParentSystem(this);
	}

	public Vec3 pos() {
		return this.position.relativeTo(this.parentGalaxy.position, Units.Tu_PER_u);
	}

	public void copySystemInfo(GalaxySector.ElementHolder info) {
		info.loadCopyOf(this.systemInfo);
	}

	public double metallicity() {
		return this.systemInfo.metallicity;
	}

	public void tick() {
		final var time = this.parentGalaxy.parentUniverse.getCelestialTime();
		this.rootNode.visit(node -> {
			Vec3.set(node.lastPosition, node.position);
		});
		this.rootNode.updatePositions(time);
	}

}
