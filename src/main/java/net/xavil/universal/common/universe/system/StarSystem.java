package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;

import net.xavil.universal.common.universe.galaxy.Galaxy;

public class StarSystem {

	public final Galaxy parentGalaxy;
	public StarSystemNode rootNode;

	public StarSystem(Galaxy parentGalaxy, StarSystemNode rootNode) {
		this.parentGalaxy = parentGalaxy;
		this.rootNode = rootNode;
	}

	public static class Info {
		public double systemAgeMya;
		public double remainingHydrogenYg;
		public String name;
		public List<StarNode> stars = new ArrayList<>();
	}

}
