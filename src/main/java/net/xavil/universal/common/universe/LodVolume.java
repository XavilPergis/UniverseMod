package net.xavil.universal.common.universe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class LodVolume<I, F> {

	public final Vec3i position;
	private final double cellResolution;

	// private final Map<Vec3i, Object2IntMap<Vec3>> initialCells = new HashMap<>();
	// private final Map<Vec3i, Object2IntMap<Vec3>> fullCells = new HashMap<>();

	private record InitEntry<I>(I init, Vec3 pos, int full) {
		public static <I> InitEntry<I> of(I init, Vec3 pos) {
			return new InitEntry<I>(init, pos, -1);
		}
	}

	public interface FullSupplier<I, F> {
		F generate(I initial, Vec3 offset, int id);
	}

	private final List<InitEntry<I>> initialList = new ArrayList<>();
	private final List<F> fullList = new ArrayList<>();
	private final FullSupplier<I, F> fullSupplier;

	public LodVolume(Vec3i position, double cellResolution, FullSupplier<I, F> fullSupplier) {
		this.position = position;
		this.cellResolution = cellResolution;
		this.fullSupplier = fullSupplier;
	}

	public int addInitial(Vec3 pos, I value) {
		var id = this.initialList.size();
		this.initialList.add(InitEntry.of(value, pos));
		return id;
		// final var scaledPos = pos.scale(1 / this.cellResolution);
		// final var cellX = (int) Math.floor(scaledPos.x);
		// final var cellY = (int) Math.floor(scaledPos.y);
		// final var cellZ = (int) Math.floor(scaledPos.z);
		// final var cellPos = new Vec3i(cellX, cellY, cellZ);
		// initialCells.computeIfAbsent(cellPos, k -> new HashMap<>()).put(pos, value);
	}

	public @Nullable F addFull(int id, F value) {
		var prev = this.initialList.get(id);
		if (prev.full == -1) {
			var fullId = this.fullList.size();
			this.fullList.add(value);
			this.initialList.set(id, new InitEntry<>(prev.init, prev.pos, fullId));
			return null;
		} else {
			return this.fullList.set(prev.full, value);
		}
	}

	public @Nullable I initialById(int id) {
		return id >= initialList.size() ? null : initialList.get(id).init;
	}

	public @Nullable F fullById(int id) {
		if (id >= initialList.size())
			return null;

		var fullId = initialList.get(id).full;
		if (fullId == -1) {
			var full = this.fullSupplier.generate(initialList.get(id).init, initialList.get(id).pos, id);
			addFull(id, full);
			fullId = initialList.get(id).full;
		}
		return this.fullList.get(fullId);
	}

	public @Nullable Vec3 offsetById(int id) {
		return id >= initialList.size() ? null : initialList.get(id).pos;
	}

	public IntStream streamIds() {
		return IntStream.range(0, this.initialList.size());
	}

	public int size() {
		return this.initialList.size();
	}

	// public Stream<T> streamInitial() {
	// return this.spatialCells.values().stream().flatMap(cell ->
	// cell.values().stream());
	// }

}
