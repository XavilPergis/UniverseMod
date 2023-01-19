package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;

import net.xavil.universal.common.universe.galaxy.Galaxy;

public class StarSystem {

	public final Galaxy parentGalaxy;
	public String name;
	public CelestialNode rootNode;

	public StarSystem(Galaxy parentGalaxy, String name, CelestialNode rootNode) {
		this.parentGalaxy = parentGalaxy;
		this.name = name;
		this.rootNode = rootNode;
	}

	public static class Info {
		public double systemAgeMya;
		public double remainingHydrogenYg;
		public List<CelestialNode.StellarBodyNode> stars = new ArrayList<>();
	}

}
