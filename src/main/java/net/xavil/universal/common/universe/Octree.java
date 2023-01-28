package net.xavil.universal.common.universe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;

public class Octree<T> {

	public record Config(int splitThreshold) {
		public static final Config DEFAULT = new Config(8);
	}

	public Node<T> rootNode;
	public final List<Element<T>> elements = new ArrayList<>();

	public Octree(Vec3 min, Vec3 max) {
		this(Config.DEFAULT, min, max);
	}

	public Octree(Config config, Vec3 min, Vec3 max) {
		this.rootNode = new Node.Leaf<T>(config, min, max);
	}

	public int insert(Vec3 pos, T value) {
		var id = this.elements.size();
		var element = new Element<T>(id, pos, value);
		var newRoot = this.rootNode.insert(element);
		if (newRoot == null) {
			Mod.LOGGER.warn("failed to insert element " + element.id + " into octree, pos=" + element.pos);
			return -1;
		}
		this.rootNode = newRoot;

		this.elements.add(element);
		return id;
	}

	public @Nullable T getById(int id) {
		return id >= elements.size() ? null : elements.get(id).value;
	}

	public @Nullable Vec3 posById(int id) {
		return id >= elements.size() ? null : elements.get(id).pos;
	}

	public int elementCount() {
		return this.elements.size();
	}

	public IntStream streamIds() {
		return IntStream.range(0, this.elements.size());
	}

	public void enumerateInRadius(Vec3 pos, double radius, BiConsumer<Vec3, T> consumer) {
		this.rootNode.enumerateInRadius(pos, radius, elem -> consumer.accept(elem.pos, elem.value));
	}

	public record Positioned<T>(int id, Vec3 pos, T value) {
	}

	public @Nullable Positioned<T> nearestInRadius(Vec3 pos, double radius) {
		var nearest = new Object() {
			Element<T> elem = null;
			double distanceSqr = Double.MAX_VALUE;
		};

		this.rootNode.enumerateInRadius(pos, radius, elem -> {
			var distanceToCenterSqr = pos.distanceToSqr(elem.pos);
			var insideSphere = distanceToCenterSqr < radius * radius;
			if (insideSphere) {
				if (nearest.elem == null || distanceToCenterSqr < nearest.distanceSqr) {
					nearest.elem = elem;
					nearest.distanceSqr = distanceToCenterSqr;
				}
			}
			// if (nearest.elem == null && pos.distanceToSqr(elem.pos) < radius * radius) {
			// } else if (nearest.elem != null && pos.distanceToSqr(elem.pos) < radius *
			// radius && nearest.distanceSqr > pos.distanceToSqr(elem.pos)) {
			// nearest.elem = elem;
			// nearest.distanceSqr = pos.distanceToSqr(elem.pos);
			// }
		});

		if (nearest.elem == null)
			return null;
		return new Positioned<T>(nearest.elem.id, nearest.elem.pos, nearest.elem.value);
	}

	public static class Element<T> {
		public int id;
		public Vec3 pos;
		public T value;

		public Element(int id, Vec3 pos, T value) {
			this.id = id;
			this.pos = pos;
			this.value = value;
		}
	}

	public static abstract sealed class Node<T> {
		public final Config config;
		public final Vec3 min, max;

		public Node(Config config, Vec3 min, Vec3 max) {
			this.config = config;
			this.min = min;
			this.max = max;
		}

		public boolean containsPoint(Vec3 pos) {
			return pos.x >= this.min.x && pos.x < this.max.x
					&& pos.y >= this.min.y && pos.y < this.max.y
					&& pos.z >= this.min.z && pos.z < this.max.z;
		}

		public boolean intersectsSphere(Vec3 center, double radius) {
			var closestX = Mth.clamp(center.x, this.min.x, this.max.x);
			var closestY = Mth.clamp(center.y, this.min.y, this.max.y);
			var closestZ = Mth.clamp(center.z, this.min.z, this.max.z);
			var diff = center.subtract(closestX, closestY, closestZ);
			return diff.lengthSqr() < radius * radius;

		}

		public static final class Leaf<T> extends Node<T> {
			public final List<Element<T>> elements = new ArrayList<>();

			public Leaf(Config config, Vec3 min, Vec3 max) {
				super(config, min, max);
			}

			@Override
			public Node<T> insert(Element<T> element) {
				if (!containsPoint(element.pos))
					return null;

				if (this.elements.size() >= this.config.splitThreshold) {
					var branch = new Branch<T>(this.config, this.min, this.max);
					this.elements.forEach(branch::insert);
					branch.insert(element);
					return branch;
				}

				this.elements.add(element);
				return this;
			}

			@Override
			public void enumerateInRadius(Vec3 pos, double radius, Consumer<Element<T>> consumer) {
				if (!intersectsSphere(pos, radius))
					return;
				for (var element : this.elements) {
					if (pos.distanceToSqr(element.pos) <= radius * radius)
						consumer.accept(element);
				}
			}
		}

		public static final class Branch<T> extends Node<T> {
			public Node<T> nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;

			public Branch(Config config, Vec3 min, Vec3 max) {
				super(config, min, max);

				double halfX = (max.x - min.x) / 2;
				double halfY = (max.y - min.y) / 2;
				double halfZ = (max.z - min.z) / 2;
				double lnx = min.x, lpx = lnx + halfX;
				double lny = min.y, lpy = lny + halfY;
				double lnz = min.z, lpz = lnz + halfZ;
				double hnx = lpx, hpx = hnx + halfX;
				double hny = lpy, hpy = hny + halfY;
				double hnz = lpz, hpz = hnz + halfZ;

				this.nnn = new Leaf<T>(config, new Vec3(lnx, lny, lnz), new Vec3(hnx, hny, hnz));
				this.nnp = new Leaf<T>(config, new Vec3(lnx, lny, lpz), new Vec3(hnx, hny, hpz));
				this.npn = new Leaf<T>(config, new Vec3(lnx, lpy, lnz), new Vec3(hnx, hpy, hnz));
				this.npp = new Leaf<T>(config, new Vec3(lnx, lpy, lpz), new Vec3(hnx, hpy, hpz));
				this.pnn = new Leaf<T>(config, new Vec3(lpx, lny, lnz), new Vec3(hpx, hny, hnz));
				this.pnp = new Leaf<T>(config, new Vec3(lpx, lny, lpz), new Vec3(hpx, hny, hpz));
				this.ppn = new Leaf<T>(config, new Vec3(lpx, lpy, lnz), new Vec3(hpx, hpy, hnz));
				this.ppp = new Leaf<T>(config, new Vec3(lpx, lpy, lpz), new Vec3(hpx, hpy, hpz));
			}

			@Override
			public Node<T> insert(Element<T> element) {
				if (!containsPoint(element.pos)) {
					return null;
				}

				// @formatter:off
				var nnnr = this.nnn.insert(element);
				if (nnnr != null) { this.nnn = nnnr; return this; }
				var nnpr = this.nnp.insert(element);
				if (nnpr != null) { this.nnp = nnpr; return this; }
				var npnr = this.npn.insert(element);
				if (npnr != null) { this.npn = npnr; return this; }
				var nppr = this.npp.insert(element);
				if (nppr != null) { this.npp = nppr; return this; }
				var pnnr = this.pnn.insert(element);
				if (pnnr != null) { this.pnn = pnnr; return this; }
				var pnpr = this.pnp.insert(element);
				if (pnpr != null) { this.pnp = pnpr; return this; }
				var ppnr = this.ppn.insert(element);
				if (ppnr != null) { this.ppn = ppnr; return this; }
				var pppr = this.ppp.insert(element);
				if (pppr != null) { this.ppp = pppr; return this; }
				// @formatter:on
				Mod.LOGGER.warn("failed to insert element " + element.id + " into branch, pos=" + element.pos);
				return this;
			}

			@Override
			public void enumerateInRadius(Vec3 pos, double radius, Consumer<Element<T>> consumer) {
				if (!intersectsSphere(pos, radius))
					return;

				this.nnn.enumerateInRadius(pos, radius, consumer);
				this.nnp.enumerateInRadius(pos, radius, consumer);
				this.npn.enumerateInRadius(pos, radius, consumer);
				this.npp.enumerateInRadius(pos, radius, consumer);
				this.pnn.enumerateInRadius(pos, radius, consumer);
				this.pnp.enumerateInRadius(pos, radius, consumer);
				this.ppn.enumerateInRadius(pos, radius, consumer);
				this.ppp.enumerateInRadius(pos, radius, consumer);
			}
		}

		public abstract Node<T> insert(Element<T> element);

		public abstract void enumerateInRadius(Vec3 pos, double radius, Consumer<Element<T>> consumer);

	}

}
