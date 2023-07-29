package net.xavil.ultraviolet.common.universe.system;

import net.xavil.hawklib.Assert;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
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

	public static class Info {
		public double systemAgeMyr;
		public double remainingMass;
		public double primaryStarProtoDiscMass;
		public StellarCelestialNode primaryStar;

		public Info(double systemAgeMya, double remainingMass, double primaryStarProtoDiscMass, StellarCelestialNode primaryStar) {
			this.systemAgeMyr = systemAgeMya;
			this.remainingMass = remainingMass;
			this.primaryStarProtoDiscMass = primaryStarProtoDiscMass;
			this.primaryStar = primaryStar;
		}

		public static Info custom(double systemAge, StarSystem system) {
			StellarCelestialNode primaryStar = null;
			for (var child : system.rootNode.selfAndChildren().iterable()) {
				if (child instanceof StellarCelestialNode starNode) {
					if (primaryStar == null)
						primaryStar = starNode;
					else if (starNode.luminosityLsol > primaryStar.luminosityLsol)
						primaryStar = starNode;
				}
			}
			Assert.isTrue(primaryStar != null);
			return new Info(systemAge, 0, 0, primaryStar);
		}
	}

}
