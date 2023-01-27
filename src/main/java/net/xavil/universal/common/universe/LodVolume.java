package net.xavil.universal.common.universe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class LodVolume<I, F> {

	public final Vec3i position;
	public final double volumeLength;

	private final List<InitEntry<I>> initialList = new ArrayList<>();
	private final List<F> fullList = new ArrayList<>();
	private final FullSupplier<I, F> fullSupplier;

	private record InitEntry<I>(I init, Vec3 pos, int full) {
		public static <I> InitEntry<I> of(I init, Vec3 pos) {
			return new InitEntry<I>(init, pos, -1);
		}
	}

	public interface FullSupplier<I, F> {
		F generate(I initial, Vec3 offset, int id);
	}

	public LodVolume(Vec3i position, double cellResolution, FullSupplier<I, F> fullSupplier) {
		this.position = position;
		this.volumeLength = cellResolution;
		this.fullSupplier = fullSupplier;
	}

	public int addInitial(Vec3 pos, I value) {
		var id = this.initialList.size();
		this.initialList.add(InitEntry.of(value, pos));
		return id;
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

	public Vec3 getBasePos() {
		return Vec3.atLowerCornerOf(this.position).scale(this.volumeLength);
	}

}
