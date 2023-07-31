package net.xavil.ultraviolet.client;

import javax.annotation.Nullable;

import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;

public final class SystemPickingTree {

	public static final class Node {
		public final Vec3 min, max;
		public Node nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;
		private int size;
		// (0,0,0) is at the nnn corner of the node.
		// packed into arrays for compactness
		private float[] packedCoords;
		private int[] packedSectorIds;

		public Node(Vec3 min, Vec3 max) {
			this.min = min;
			this.max = max;
		}

		public void loadCoord(Vec3.Mutable out, int i) {
			i *= 3;
			out.x = this.packedCoords[i++] + this.min.x;
			out.y = this.packedCoords[i++] + this.min.y;
			out.z = this.packedCoords[i++] + this.min.z;
		}

		public void storeCoord(Vec3Access in, int i) {
			i *= 3;
			this.packedCoords[i++] = (float) (in.x() - this.min.x);
			this.packedCoords[i++] = (float) (in.y() - this.min.y);
			this.packedCoords[i++] = (float) (in.z() - this.min.z);
		}

		public GalaxySectorId getId(int i) {
			i *= 4;
			final int x = this.packedSectorIds[i++];
			final int y = this.packedSectorIds[i++];
			final int z = this.packedSectorIds[i++];
			final int info = this.packedSectorIds[i++];
			return new GalaxySectorId(new Vec3i(x, y, z), info);
		}

		public void setId(int i, GalaxySectorId id) {
			i *= 4;
			this.packedSectorIds[i++] = id.levelCoords().x;
			this.packedSectorIds[i++] = id.levelCoords().y;
			this.packedSectorIds[i++] = id.levelCoords().z;
			this.packedSectorIds[i++] = id.packedInfo();
		}

		public void split() {
			if (this.nnn != null)
				return;
			final var center = this.min.div(2.0).add(this.max.div(2.0));
			final Vec3 n = this.min, c = center, p = this.max;
			final var nnn = new Vec3(n.x, n.y, n.z);
			final var nnc = new Vec3(n.x, n.y, c.z);
			final var ncn = new Vec3(n.x, c.y, n.z);
			final var ncc = new Vec3(n.x, c.y, c.z);
			final var cnn = new Vec3(c.x, n.y, n.z);
			final var cnc = new Vec3(c.x, n.y, c.z);
			final var ccn = new Vec3(c.x, c.y, n.z);
			final var ccc = new Vec3(c.x, c.y, c.z);
			final var ccp = new Vec3(c.x, c.y, p.z);
			final var cpc = new Vec3(c.x, p.y, c.z);
			final var cpp = new Vec3(c.x, p.y, p.z);
			final var pcc = new Vec3(p.x, c.y, c.z);
			final var pcp = new Vec3(p.x, c.y, p.z);
			final var ppc = new Vec3(p.x, p.y, c.z);
			final var ppp = new Vec3(p.x, p.y, p.z);
			this.nnn = new Node(nnn, ccc);
			this.nnp = new Node(nnc, ccp);
			this.npn = new Node(ncn, cpc);
			this.npp = new Node(ncc, cpp);
			this.pnn = new Node(cnn, pcc);
			this.pnp = new Node(cnc, pcp);
			this.ppn = new Node(ccn, ppc);
			this.ppp = new Node(ccc, ppp);
		}

	}

	public final MutableMap<Vec3i, Node> rootNodes = MutableMap.hashMap();

	private static final class PickingContext {
		public final Ray ray;
		public double closestDistance = Double.POSITIVE_INFINITY;
		public Node closestNode = null;
		public int closestIndex = -1;

		public PickingContext(Ray ray) {
			this.ray = ray;
		}
	}

	public void pickNode(PickingContext ctx, Node node) {
		// ok.. technically this isnt quite right, since the selection area for a point
		// could "bleed over" the edge of a node, and the ray could miss that, but i
		// feel like that will be pretty rare, and we get potentially quite large
		// savings from not having to check every child node.
		if (!ctx.ray.intersectAABB(node.min, node.max))
			return;

		final var pos = new Vec3.Mutable(0, 0, 0);
		final var proj = new Vec3.Mutable(0, 0, 0);
		for (int i = 0; i < node.size; ++i) {
			node.loadCoord(pos, i);

			// if (pos.distanceTo(viewCenter) > levelSize)
			// return;

			final var distance = ctx.ray.origin().distanceTo(pos);
			if (!ctx.ray.intersectsSphere(pos, 0.02 * distance))
				continue;

			Vec3.sub(pos, pos, ctx.ray.origin());
			Vec3.projectOnto(proj, pos, ctx.ray.dir());
			final var projDist = pos.distanceTo(proj);
			if (projDist < ctx.closestDistance) {
				ctx.closestDistance = projDist;
				ctx.closestNode = node;
				ctx.closestIndex = i;
			}
		}

		// unfortunately, we still need to check all children, since there are no
		// guaranteed orderings between octree levels.
		// @formatter:off
		if (node.nnn != null) pickNode(ctx, node.nnn);
		if (node.nnp != null) pickNode(ctx, node.nnp);
		if (node.npn != null) pickNode(ctx, node.npn);
		if (node.npp != null) pickNode(ctx, node.npp);
		if (node.pnn != null) pickNode(ctx, node.pnn);
		if (node.pnp != null) pickNode(ctx, node.pnp);
		if (node.ppn != null) pickNode(ctx, node.ppn);
		if (node.ppp != null) pickNode(ctx, node.ppp);
		// @formatter:on
	}

	@Nullable
	public GalaxySectorId pick(Ray ray) {
		// NOTE: we could optimize this further by not scanning each loaded root node,
		// but the savings dont seem worth the complexity.
		final var ctx = new PickingContext(ray);
		for (final var root : this.rootNodes.values().iterable()) {
			pickNode(ctx, root);
		}

		if (ctx.closestNode != null)
			return ctx.closestNode.getId(ctx.closestIndex);

		return null;
	}

}
