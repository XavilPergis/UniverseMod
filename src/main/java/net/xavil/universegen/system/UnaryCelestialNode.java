package net.xavil.universegen.system;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public abstract sealed class UnaryCelestialNode extends CelestialNode permits StellarCelestialNode, PlanetaryCelestialNode {

	public final MutableList<CelestialRing> rings = new Vector<>();
	public double radius; // km
	public double obliquityAngle; // rad
	public double rotationalRate; // rad/s
	public double temperature; // K

	public UnaryCelestialNode() {
	}

	public UnaryCelestialNode(double massYg) {
		super(massYg);
	}

}
