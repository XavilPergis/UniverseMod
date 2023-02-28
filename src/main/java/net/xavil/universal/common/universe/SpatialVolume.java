package net.xavil.universal.common.universe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;

public class SpatialVolume<T> {

	public record Id(int layerIndex, int elementIndex) {
		public static final Codec<Id> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.INT.fieldOf("layer").forGetter(Id::layerIndex),
				Codec.INT.fieldOf("id").forGetter(Id::elementIndex))
				.apply(inst, Id::new));
	}

	public Node<T> rootNode;
	public final Int2ObjectMap<List<Element<T>>> elements = new Int2ObjectOpenHashMap<>();
	public final Set<Id> markedElements = new HashSet<>();

	public SpatialVolume(Vec3 min, Vec3 max) {
		this.rootNode = new Node<T>(min, max);
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

	public static final class Node<T> {
		// The spatial bounds of this node. All points stored within this node are
		// within these bounds.
		public final Vec3 min, max;
		public final List<Element<T>> elements = new ArrayList<>();
		public Node<T> nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;

		public Node(Vec3 min, Vec3 max) {
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

		public static double visibilityRadius(double luminosity, double luminosityThreshold) {
			// I = L / (4 * pi * r^2)
			// r = sqrt(L / (4 * pi * I))
			return Math.sqrt(luminosity / (4 * Math.PI * luminosityThreshold));
		}

		public void split() {
			double halfX = (max.x - min.x) / 2;
			double halfY = (max.y - min.y) / 2;
			double halfZ = (max.z - min.z) / 2;
			double lnx = min.x, lpx = lnx + halfX;
			double lny = min.y, lpy = lny + halfY;
			double lnz = min.z, lpz = lnz + halfZ;
			double hnx = lpx, hpx = hnx + halfX;
			double hny = lpy, hpy = hny + halfY;
			double hnz = lpz, hpz = hnz + halfZ;

			this.nnn = new Node<T>(Vec3.from(lnx, lny, lnz), Vec3.from(hnx, hny, hnz));
			this.nnp = new Node<T>(Vec3.from(lnx, lny, lpz), Vec3.from(hnx, hny, hpz));
			this.npn = new Node<T>(Vec3.from(lnx, lpy, lnz), Vec3.from(hnx, hpy, hnz));
			this.npp = new Node<T>(Vec3.from(lnx, lpy, lpz), Vec3.from(hnx, hpy, hpz));
			this.pnn = new Node<T>(Vec3.from(lpx, lny, lnz), Vec3.from(hpx, hny, hnz));
			this.pnp = new Node<T>(Vec3.from(lpx, lny, lpz), Vec3.from(hpx, hny, hpz));
			this.ppn = new Node<T>(Vec3.from(lpx, lpy, lnz), Vec3.from(hpx, hpy, hnz));
			this.ppp = new Node<T>(Vec3.from(lpx, lpy, lpz), Vec3.from(hpx, hpy, hpz));
		}

		public void unsplit() {
			this.nnn = this.nnp = this.npn = this.npp = this.pnn = this.pnp = this.ppn = this.ppp = null;
		}

		public void enumerateVisible(Vec3 viewPos, Consumer<Element<T>> consumer) {}

	}

}
