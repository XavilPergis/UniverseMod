package net.xavil.ultraviolet.common.universe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.xavil.ultraviolet.Mod;
import net.xavil.util.math.matrices.Vec3;

public class Octree<T> {

	public record Id(int layerIndex, int elementIndex) {
		public static final Codec<Id> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.INT.fieldOf("layer").forGetter(Id::layerIndex),
				Codec.INT.fieldOf("id").forGetter(Id::elementIndex))
				.apply(inst, Id::new));
	}

	public record Config(int splitThreshold) {
		public static final Config DEFAULT = new Config(8);
	}

	public Node<T> rootNode;
	public final Int2ObjectMap<List<Element<T>>> elements = new Int2ObjectOpenHashMap<>();
	public final Set<Id> markedElements = new HashSet<>();

	public Octree(Vec3 min, Vec3 max) {
		this(Config.DEFAULT, min, max);
	}

	public Octree(Config config, Vec3 min, Vec3 max) {
		this.rootNode = new Node.Leaf<T>(config, min, max);
	}

	public int insert(Vec3 pos, int layerId, T value) {
		if (!this.elements.containsKey(layerId)) {
			this.elements.put(layerId, new ArrayList<>());
		}
		final var layer = this.elements.get(layerId);

		var id = layer.size();
		var element = new Element<T>(new Id(layerId, id), pos, value);
		var newRoot = this.rootNode.insert(element);
		if (newRoot == null) {
			Mod.LOGGER.warn("failed to insert element " + id + " into octree, pos=" + element.pos);
			return -1;
		}
		this.rootNode = newRoot;
		layer.add(element);

		return id;
	}

	public T getById(Id id) {
		var layer = this.elements.get(id.layerIndex);
		if (layer == null)
			return null;
		return id.elementIndex >= layer.size() ? null : layer.get(id.elementIndex).value;
	}

	public Vec3 posById(Id id) {
		var layer = this.elements.get(id.layerIndex);
		if (layer == null)
			return null;
		return id.elementIndex >= layer.size() ? null : layer.get(id.elementIndex).pos;
	}

	public void enumerateInRadius(Vec3 pos, double radius, Consumer<Element<T>> consumer) {
		this.rootNode.enumerateInRadius(pos, radius, consumer);
	}

	public void enumerateElements(Consumer<Element<T>> consumer) {
		this.elements.forEach((layerId, elements) -> elements.forEach(consumer::accept));
	}

	public Stream<Element<T>> streamElements() {
		var layerIdStream = this.elements.keySet().intStream();
		// IntStream's flatMap can't map to an object stream for some reason
		return layerIdStream.boxed().flatMap(layer -> this.elements.get((int) layer).stream());
	}

	public @Nullable Element<T> nearestInRadius(Vec3 pos, double radius) {
		var nearest = new Object() {
			Element<T> elem = null;
			double distanceSqr = Double.MAX_VALUE;
		};

		this.rootNode.enumerateInRadius(pos, radius, elem -> {
			var distanceToCenterSqr = pos.distanceToSquared(elem.pos);
			var insideSphere = distanceToCenterSqr < radius * radius;
			if (insideSphere) {
				if (nearest.elem == null || distanceToCenterSqr < nearest.distanceSqr) {
					nearest.elem = elem;
					nearest.distanceSqr = distanceToCenterSqr;
				}
			}
		});

		if (nearest.elem == null)
			return null;
		return nearest.elem;
	}

	public static class Element<T> {
		public final Id id;
		public final Vec3 pos;
		public final T value;

		public Element(Id id, Vec3 pos, T value) {
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
			var diff = center.sub(closestX, closestY, closestZ);
			return diff.lengthSquared() < radius * radius;

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
					if (pos.distanceToSquared(element.pos) <= radius * radius)
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

				this.nnn = new Leaf<T>(config, Vec3.from(lnx, lny, lnz), Vec3.from(hnx, hny, hnz));
				this.nnp = new Leaf<T>(config, Vec3.from(lnx, lny, lpz), Vec3.from(hnx, hny, hpz));
				this.npn = new Leaf<T>(config, Vec3.from(lnx, lpy, lnz), Vec3.from(hnx, hpy, hnz));
				this.npp = new Leaf<T>(config, Vec3.from(lnx, lpy, lpz), Vec3.from(hnx, hpy, hpz));
				this.pnn = new Leaf<T>(config, Vec3.from(lpx, lny, lnz), Vec3.from(hpx, hny, hnz));
				this.pnp = new Leaf<T>(config, Vec3.from(lpx, lny, lpz), Vec3.from(hpx, hny, hpz));
				this.ppn = new Leaf<T>(config, Vec3.from(lpx, lpy, lnz), Vec3.from(hpx, hpy, hnz));
				this.ppp = new Leaf<T>(config, Vec3.from(lpx, lpy, lpz), Vec3.from(hpx, hpy, hpz));
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
				Mod.LOGGER
						.warn("failed to insert element " + element.id + " into branch, pos=" + element.pos);
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
