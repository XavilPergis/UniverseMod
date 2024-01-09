package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.util.VisibleForDebug;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class GalaxySector {

	// level 0 is the smallest (base) size.
	// 0 -> 7, resulting in 8 total levels
	public static final int ROOT_LEVEL = 7;
	// the total amount of subsectors contained within the space a single root
	// sector occupies.
	public static final int SUBSECTORS_PER_ROOT_SECTOR;
	public static final double BASE_SIZE_Tm = 10.0 / Units.ly_PER_Tm;
	public static final double ROOT_SIZE_Tm = sizeForLevel(ROOT_LEVEL);

	// public static final long SECTOR_NAME_SEED = 621;
	// public static final int SECTOR_NAME_CHOICE_COUNT = 256;
	// public static final String[][] SECTOR_NAME_CHOICES;
	// public static final NameTemplate SECTOR_NAME_TEMPLATE =
	// NameTemplate.compile("[(BV)(?VCV)]?(CV?(CL))");

	static {
		int subsectorTotal = 0;
		for (int i = 0; i <= ROOT_LEVEL; ++i)
			subsectorTotal += sectorsPerRootSector(i);
		SUBSECTORS_PER_ROOT_SECTOR = subsectorTotal;

		// SECTOR_NAME_CHOICES = new String[ROOT_LEVEL + 1][SECTOR_NAME_CHOICE_COUNT];
		// final var rng = new SplittableRng(SECTOR_NAME_SEED);
		// for (int i = 0; i <= SECTOR_NAME_CHOICES.length; ++i) {
		// final var levelTable = SECTOR_NAME_CHOICES[i];
		// rng.push(i);
		// for (int j = 0; j <= levelTable.length; ++j) {
		// levelTable[j] = SECTOR_NAME_TEMPLATE.generate(rng.rng("name"));
		// rng.advance();
		// }
		// rng.pop();
		// rng.advance();
		// }
	}

	// public static String nameForSector(SectorPos pos) {
	// return Rng.fromSeed(pos.hash()).pick(SECTOR_NAME_CHOICES[pos.level()]);
	// }

	public static int sectorsPerRootSector(int level) {
		return 1 << (3 * (ROOT_LEVEL - level));
	}

	public static double sizeForLevel(int level) {
		return (1 << level) * BASE_SIZE_Tm;
	}

	public static Vec3i levelCoordsForPos(int level, Vec3Access pos) {
		return pos.div(sizeForLevel(level)).floor();
	}

	public final int level;
	// the sum of all the reference counts for all the descendant of this sector,
	// including itself. Used for figuring out whether this sector can be unloaded
	// or not.
	private int weakReferenceCount = 0;
	// the number of things that want this sector to remain fully expanded. Loaded
	// sectors will not be automatically collected by calls to unload()
	private int strongReferenceCount = 0;
	public final int x, y, z;
	public volatile PackedElements elements = null;
	private Branch branch = null;

	public record Branch(
			GalaxySector nnn, GalaxySector nnp,
			GalaxySector npn, GalaxySector npp,
			GalaxySector pnn, GalaxySector pnp,
			GalaxySector ppn, GalaxySector ppp) {
	}

	public GalaxySector(Vec3i coords) {
		this(ROOT_LEVEL, coords.x, coords.y, coords.z);
	}

	private GalaxySector(int level, int x, int y, int z) {
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean isComplete() {
		return isLoaded() && this.elements != null;
	}

	public boolean isLoaded() {
		return this.strongReferenceCount > 0;
	}

	/**
	 * Creates a branch for this node if it did not already have one.
	 */
	private void createChildren() {
		if (this.branch != null || this.level == 0)
			return;
		final var nl = this.level - 1;
		final int bx = 2 * x, by = 2 * y, bz = 2 * z;
		// @formatter:off
		final var nnn = new GalaxySector(nl, bx + 0, by + 0, bz + 0);
		final var nnp = new GalaxySector(nl, bx + 0, by + 0, bz + 1);
		final var npn = new GalaxySector(nl, bx + 0, by + 1, bz + 0);
		final var npp = new GalaxySector(nl, bx + 0, by + 1, bz + 1);
		final var pnn = new GalaxySector(nl, bx + 1, by + 0, bz + 0);
		final var pnp = new GalaxySector(nl, bx + 1, by + 0, bz + 1);
		final var ppn = new GalaxySector(nl, bx + 1, by + 1, bz + 0);
		final var ppp = new GalaxySector(nl, bx + 1, by + 1, bz + 1);
		// @formatter:on
		this.branch = new Branch(nnn, nnp, npn, npp, pnn, pnp, ppn, ppp);
	}

	/**
	 * Loads the sector at the given sector pos. Does nothing if the sector pos is
	 * invalid, either by having a level more broad than the current level, or by
	 * the current sector not containing the target position.
	 * 
	 * This method automatically splits this sector and its descendants as many
	 * times as needed to create the target sector, and will increase the target's
	 * reference count by one.
	 * 
	 * @param pos The position of the target sector.
	 */
	public void load(SectorPos pos) {
		Assert.isGreaterOrEqual(this.level, pos.level());

		// this is basically `pos` but in the coorinate system for the level that this
		// node is on.
		final var levelPos = pos.levelCoords().floorDiv((1 << this.level) / (1 << pos.level()));
		// bail if our target node is not a descendant of this node
		if (this.x != levelPos.x || this.y != levelPos.y || this.z != levelPos.z)
			return;

		// if execution has gotten to this point, we know that the target sector was
		// either this sector, or contained somewhere in this sector's bounds. This
		// marks all the ancestors of the target node with an additional weak count.
		this.weakReferenceCount += 1;

		if (pos.level() == this.level) {
			this.strongReferenceCount += 1;
		} else {
			createChildren();
			this.branch.nnn.load(pos);
			this.branch.nnp.load(pos);
			this.branch.npn.load(pos);
			this.branch.npp.load(pos);
			this.branch.pnn.load(pos);
			this.branch.pnp.load(pos);
			this.branch.ppn.load(pos);
			this.branch.ppp.load(pos);
		}
	}

	/**
	 * Unloads the sector at the given sector pos.
	 * 
	 * @param pos The position of the target sector.
	 * @return Whether the current node is allowed to be unloaded.
	 */
	public boolean unload(SectorPos pos) {
		Assert.isGreaterOrEqual(this.level, pos.level());

		// this is basically `pos` but in the coorinate system for the level that this
		// node is on.
		final var levelPos = pos.levelCoords().floorDiv((1 << this.level) / (1 << pos.level()));
		// bail if our target node is not a descendant of this node
		if (this.x != levelPos.x || this.y != levelPos.y || this.z != levelPos.z)
			return this.weakReferenceCount <= 0;

		if (this.weakReferenceCount <= 0) {
			// sectors with a weak count of zero should not have any loaded subsectors, so
			// trying to unload one of them recursively doesn't make any sense and likely
			// indicates a double free.
			Mod.LOGGER.error(
					"tried to transitively unload sector with weak count of {}. level {}, pos ({}, {}, {})",
					this.weakReferenceCount, this.level, this.x, this.y, this.z);
			return true;
		}

		if (this.level != pos.level() && this.branch != null) {
			boolean mayUnload = true;
			mayUnload &= this.branch.nnn.unload(pos);
			mayUnload &= this.branch.nnp.unload(pos);
			mayUnload &= this.branch.npn.unload(pos);
			mayUnload &= this.branch.npp.unload(pos);
			mayUnload &= this.branch.pnn.unload(pos);
			mayUnload &= this.branch.pnp.unload(pos);
			mayUnload &= this.branch.ppn.unload(pos);
			mayUnload &= this.branch.ppp.unload(pos);
			if (mayUnload)
				this.branch = null;
		}

		this.weakReferenceCount -= 1;

		if (pos.level() == this.level) {
			if (this.strongReferenceCount <= 0)
				this.elements = null;
			// we're the target node that's being unloaded... and we're already unloaded!
			// This is likely caused by a double free.
			if (this.strongReferenceCount <= 0) {
				Mod.LOGGER.error(
						"tried to transitively unload sector with strong count of {}. level {}, pos ({}, {}, {})",
						this.strongReferenceCount, this.level, this.x, this.y, this.z);
				return true;
			}
			this.strongReferenceCount -= 1;
		}

		return this.weakReferenceCount <= 0;
	}

	public static final class SectorDebugInfo {
		// TODO: add "populated" counter
		/** The total number of branch nodes. */
		public int branchCount;
		/** The total number of leaf nodes. */
		public int leafCount;
		/** The total number of nodes that are strongly loaded. */
		public int stronglyLoadedCount;
		/** The total number of nodes that are weakly loaded. */
		public int weaklyLoadedCount;
		/**
		 * The weak count of the root node. It should equal
		 * {@link #stronglyLoadedCount}, otherwise there is a bug in the program.
		 */
		public int rootWeakCount;

		public int total() {
			return this.branchCount + this.leafCount;
		}
	}

	@VisibleForDebug
	public SectorDebugInfo gatherDebugInfo() {
		final var res = new SectorDebugInfo();
		res.rootWeakCount = this.weakReferenceCount;
		gatherDebugInfoInternal(res);
		return res;
	}

	private void gatherDebugInfoInternal(SectorDebugInfo output) {
		output.stronglyLoadedCount += this.strongReferenceCount > 0 ? 1 : 0;
		output.weaklyLoadedCount += this.weakReferenceCount > 0 ? 1 : 0;
		if (this.branch != null) {
			output.branchCount += 1;
			this.branch.nnn.gatherDebugInfoInternal(output);
			this.branch.nnp.gatherDebugInfoInternal(output);
			this.branch.npn.gatherDebugInfoInternal(output);
			this.branch.npp.gatherDebugInfoInternal(output);
			this.branch.pnn.gatherDebugInfoInternal(output);
			this.branch.pnp.gatherDebugInfoInternal(output);
			this.branch.ppn.gatherDebugInfoInternal(output);
			this.branch.ppp.gatherDebugInfoInternal(output);
		} else {
			output.leafCount += 1;
		}
	}

	public GalaxySector lookupNode(SectorPos pos) {
		if (pos.level() > this.level)
			return null;

		// this is basically `pos` but in the coorinate system for the level that this
		// node is on.
		final var levelPos = pos.levelCoords().floorDiv((1 << this.level) / (1 << pos.level()));
		// bail if our target node is not a descendant of this node
		if (this.x != levelPos.x || this.y != levelPos.y || this.z != levelPos.z)
			return null;

		if (pos.level() == this.level)
			return this;

		if (this.branch != null) {
			// @formatter:off
			final var nnn = this.branch.nnn.lookupNode(pos); if (nnn != null) return nnn;
			final var nnp = this.branch.nnp.lookupNode(pos); if (nnp != null) return nnp;
			final var npn = this.branch.npn.lookupNode(pos); if (npn != null) return npn;
			final var npp = this.branch.npp.lookupNode(pos); if (npp != null) return npp;
			final var pnn = this.branch.pnn.lookupNode(pos); if (pnn != null) return pnn;
			final var pnp = this.branch.pnp.lookupNode(pos); if (pnp != null) return pnp;
			final var ppn = this.branch.ppn.lookupNode(pos); if (ppn != null) return ppn;
			final var ppp = this.branch.ppp.lookupNode(pos); if (ppp != null) return ppp;
			// @formatter:on
		}

		return null;
	}

	public void loadElement(ElementHolder out, int subnodeIndex) {
		this.elements.load(out, subnodeIndex);
	}

	public SectorPos pos() {
		return new SectorPos(this.level, new Vec3i(this.x, this.y, this.z));
	}

	public static final class ElementHolder {
		public Vec3.Mutable systemPosTm = new Vec3.Mutable();
		public long systemSeed;
		public int generationLayer;
		// primary star info
		public double systemAgeMyr;
		public double massYg;
		// these are essentially cached versions of the information you can derive from
		// the seed, age, and mass.
		public double luminosityLsol;
		public double temperatureK;

		@Nullable
		public String name;

		public void loadCopyOf(ElementHolder other) {
			this.systemPosTm = new Vec3.Mutable(other.systemPosTm);
			this.systemSeed = other.systemSeed;
			this.generationLayer = other.generationLayer;
			this.systemAgeMyr = other.systemAgeMyr;
			this.massYg = other.massYg;
			this.luminosityLsol = other.luminosityLsol;
			this.temperatureK = other.temperatureK;
			this.name = other.name;
		}
	}

	public static final class PackedElements {
		public static final int FLOAT_ELEMENT_COUNT = 7;
		public static final int INT_ELEMENT_COUNT = 3;

		public final Vec3 sectorOrigin;

		// NOTE: position is stored relative to `sectorOrigin`
		// x y z age mass luminosity temperature
		private float[] floatBuffer = null;
		// generationlayer systemseed:systemseed
		private int[] intBuffer = null;

		// let's hope the overhead here isnt too bad...
		private final boolean hasNames;
		private String[] names = null;

		private int size = 0, capacity = 0;

		public PackedElements(Vec3 sectorOrigin, boolean hasNames) {
			this.sectorOrigin = sectorOrigin;
			this.hasNames = hasNames;
		}

		public int size() {
			return this.size;
		}

		public int capacity() {
			return this.capacity;
		}

		public ElementHolder load(ElementHolder out, int i) {
			int fi = i * FLOAT_ELEMENT_COUNT;
			out.systemPosTm.x = this.floatBuffer[fi++] + this.sectorOrigin.x;
			out.systemPosTm.y = this.floatBuffer[fi++] + this.sectorOrigin.y;
			out.systemPosTm.z = this.floatBuffer[fi++] + this.sectorOrigin.z;
			out.systemAgeMyr = this.floatBuffer[fi++];
			out.massYg = this.floatBuffer[fi++];
			out.luminosityLsol = this.floatBuffer[fi++];
			out.temperatureK = this.floatBuffer[fi++];
			int li = i * INT_ELEMENT_COUNT;
			out.generationLayer = this.intBuffer[li++];
			out.systemSeed = (long) this.intBuffer[li++];
			out.systemSeed |= ((long) this.intBuffer[li++]) << 32;
			if (this.hasNames) {
				out.name = this.names[i];
			}
			return out;
		}

		public void store(ElementHolder in, int i) {
			int fi = i * FLOAT_ELEMENT_COUNT;
			this.floatBuffer[fi++] = (float) (in.systemPosTm.x - this.sectorOrigin.x);
			this.floatBuffer[fi++] = (float) (in.systemPosTm.y - this.sectorOrigin.y);
			this.floatBuffer[fi++] = (float) (in.systemPosTm.z - this.sectorOrigin.z);
			this.floatBuffer[fi++] = (float) in.systemAgeMyr;
			this.floatBuffer[fi++] = (float) in.massYg;
			this.floatBuffer[fi++] = (float) in.luminosityLsol;
			this.floatBuffer[fi++] = (float) in.temperatureK;
			int li = i * INT_ELEMENT_COUNT;
			this.intBuffer[li++] = in.generationLayer;
			this.intBuffer[li++] = (int) (in.systemSeed);
			this.intBuffer[li++] = (int) (in.systemSeed >>> 32);
			if (this.hasNames) {
				this.names[i] = in.name;
			}
		}

		public void push(ElementHolder in) {
			reserve(1);
			store(in, this.size);
			markWritten(1);
		}

		public void storeSequence(PackedElements other, int i) {
			Assert.isLesserOrEqual(other.size + i, this.capacity);

			System.arraycopy(other.floatBuffer, 0, this.floatBuffer, i, FLOAT_ELEMENT_COUNT * other.size);
			System.arraycopy(other.intBuffer, 0, this.intBuffer, i, INT_ELEMENT_COUNT * other.size);
			if (this.hasNames && other.hasNames) {
				System.arraycopy(other.names, 0, this.names, i, other.size);
			}
		}

		public void reserve(int requestedSlots) {
			final var desiredCapacity = this.size + requestedSlots;
			if (desiredCapacity <= this.capacity)
				return;
			if (this.capacity == 0) {
				this.floatBuffer = new float[FLOAT_ELEMENT_COUNT * requestedSlots];
				this.intBuffer = new int[INT_ELEMENT_COUNT * requestedSlots];
				this.names = new String[requestedSlots];
				this.capacity = requestedSlots;
			} else {
				final var newCapacity = Math.max(2 * this.capacity, desiredCapacity);
				this.floatBuffer = Arrays.copyOf(this.floatBuffer, FLOAT_ELEMENT_COUNT * newCapacity);
				this.intBuffer = Arrays.copyOf(this.intBuffer, INT_ELEMENT_COUNT * newCapacity);
				this.names = Arrays.copyOf(this.names, newCapacity);
				this.capacity = newCapacity;
			}
		}

		public void markWritten(int usedSlots) {
			this.size += usedSlots;
		}

		public void shrinkToFit() {
			if (this.floatBuffer != null)
				this.floatBuffer = Arrays.copyOf(this.floatBuffer, FLOAT_ELEMENT_COUNT * this.size);
			if (this.intBuffer != null)
				this.intBuffer = Arrays.copyOf(this.intBuffer, INT_ELEMENT_COUNT * this.size);
			if (this.names != null)
				this.names = Arrays.copyOf(this.names, this.size);
			this.capacity = this.size;
		}

	}

}
