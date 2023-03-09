package net.xavil.universegen.galaxy;

import java.util.ArrayList;
import java.util.List;

import net.xavil.util.math.Interval;

public class SectorInfo {

	public final List<GalaxySectorEntry> entries = new ArrayList<>();
	public double remainingMass;
	public Interval massRange;

	public SectorInfo createDerived() {
		return new SectorInfo();
	}

}
