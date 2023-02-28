package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

		private final List<StarNode> stars = new ArrayList<>();
		private StarNode displayStar;

		public void addStar(StarNode star) {
			for (var i = 0; i < this.stars.size(); ++i) {
				if (star.luminosityLsol < this.stars.get(i).luminosityLsol) {
					// not the brightest star, don't update the display star.
					this.stars.add(i, star);
					return;
				}
			}
			this.stars.add(star);
			this.displayStar = star;
		}

		public Stream<StarNode> getStars() {
			return this.stars.stream();
		}

		public StarNode getDisplayStar() {
			return this.displayStar;
		}
	}

}
