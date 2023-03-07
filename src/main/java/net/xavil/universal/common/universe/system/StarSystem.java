package net.xavil.universal.common.universe.system;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.universegen.system.CelestialNode;

public class StarSystem {

	public final Galaxy parentGalaxy;
	public CelestialNode rootNode;

	public StarSystem(Galaxy parentGalaxy, CelestialNode rootNode) {
		this.parentGalaxy = parentGalaxy;
		this.rootNode = rootNode;
	}

	public static class Info {
		public double systemAgeMya;
		public double remainingHydrogenYg;
		public String name;

		private final List<StellarCelestialNode> stars = new ArrayList<>();
		private StellarCelestialNode displayStar;

		public void addStar(StellarCelestialNode star) {
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

		public Stream<StellarCelestialNode> getStars() {
			return this.stars.stream();
		}

		public StellarCelestialNode getDisplayStar() {
			return this.displayStar;
		}
	}

}
