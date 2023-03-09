package net.xavil.universegen.galaxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.xavil.universegen.LinearSpline;
import net.xavil.util.math.Vec3i;

public class Galaxy {

	public final Map<Vec3i, GalaxySector> sectors = new Object2ObjectOpenHashMap<>();
	public final LinearSpline stellarMassDistribution;
	private final List<GalaxyTicket> tickets = new ArrayList<>();
	private final IntList freeTicketIds = new IntArrayList();

	public final double galaxyAgeMyr;

	public Galaxy(double galaxyAgeMyr) {
		this.galaxyAgeMyr = galaxyAgeMyr;
		this.stellarMassDistribution = new LinearSpline();
		
		// @formatter:off
		final double[] STAR_CLASS_PERCENTAGES = { 0.7645, 0.121, 0.076, 0.03, 0.006, 0.0013, 0.0000003      };
		final double[] STAR_CLASS_WEIGHTS     = { 0.08,   0.45,  0.8,   1.04, 1.4,   2.1,    16,       100  };
		// @formatter:on

		var p = 0;
		for (var i = 0; i < 7; ++i) {
			this.stellarMassDistribution.addControlPoint(p, STAR_CLASS_WEIGHTS[i]);
			p += STAR_CLASS_PERCENTAGES[i];
		}
		this.stellarMassDistribution.addControlPoint(1, STAR_CLASS_WEIGHTS[7]);
	}

	private int reserveTicketId() {
		if (!this.freeTicketIds.isEmpty()) {
			return this.freeTicketIds.removeInt(this.freeTicketIds.size() - 1);
		}
		this.tickets.add(null);
		return this.tickets.size() - 1;
	}

	private void releaseTicketId(int id) {
		this.freeTicketIds.add(id);
	}

	// CLIENT
	// galaxy map - camera cursor and/or camera position
	// background stars - for a short time, player position
	// system/galaxy warp animation - player position

	// SERVER
	// system ticking
	// vessel warping (source + target)

}
