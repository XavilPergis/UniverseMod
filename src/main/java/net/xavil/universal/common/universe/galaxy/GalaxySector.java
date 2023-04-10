package net.xavil.universal.common.universe.galaxy;

import java.util.List;

import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.util.Assert;
import net.xavil.util.Units;
import net.xavil.util.Util;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public final class GalaxySector {

	public static final int ROOT_LEVEL = 7; // 0 -> 7, resulting in 8 total levels
	public static final double BASE_SIZE_Tm = 10.0 / Units.ly_PER_Tm;
	public static final double ROOT_SIZE_Tm = sizeForLevel(ROOT_LEVEL);

	public static final int MASK_FLAG_GENERATION_STARTED = Util.intMask(0, 1);
	public static final int MASK_FLAG_GENERATION_FINISHED = Util.intMask(1, 2);

	public final int level;
	// the sum of all the reference counts for all the descendant of this sector,
	// including itself. Used for figuring out whether this sector can be unloaded
	// or not.
	private int weakReferenceCount = 0;
	// the number of things that want this sector to remain fully expanded. Loaded
	// sectors will not be automatically collected by calls to unload()
	private int referenceCount = 0;
	private int flags = 0;
	public final int x, y, z;
	public volatile ImmutableList<InitialElement> initialElements = null;
	private Branch branch = null;

	public record InitialElement(Vec3 pos, StarSystem.Info info, long seed, int generationLayer) {
	}

	public record Branch(
			GalaxySector nnn, GalaxySector nnp,
			GalaxySector npn, GalaxySector npp,
			GalaxySector pnn, GalaxySector pnp,
			GalaxySector ppn, GalaxySector ppp) {
	}

	public GalaxySector(Vec3i coords) {
		this(ROOT_LEVEL, coords.x, coords.y, coords.z);
	}

	public GalaxySector(int x, int y, int z) {
		this(ROOT_LEVEL, x, y, z);
	}

	private GalaxySector(int level, int x, int y, int z) {
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static double sizeForLevel(int level) {
		return (1 << level) * BASE_SIZE_Tm;
	}

	public static Vec3i levelCoordsForPos(int level, Vec3 pos) {
		return pos.div(sizeForLevel(level)).floor();
	}

	// public void setInitialElements(ImmutableList<InitialElement> initialElements) {
	// 	if (!isLoaded()) {
	// 		Mod.LOGGER.warn("tried to set initial elements of unloaded sector ({}, {}, {})", x, y, z);
	// 		return;
	// 	}
	// 	if (this.initialElements != null) {
	// 		Mod.LOGGER.warn(
	// 				"tried to set initial elements of sector ({}, {}, {}), but the sector already had {} elements", x,
	// 				y, z, this.initialElements.size());
	// 		return;
	// 	}
	// 	this.initialElements = initialElements;
	// }

	// public ImmutableList<InitialElement> getInitialElements() {
	// 	return this.initialElements;
	// }

	public boolean isComplete() {
		return isLoaded() && this.initialElements != null;
	}
	public boolean isLoaded() {
		return this.referenceCount > 0;
	}

	public void setGenerationStarted(boolean generationStarted) {
		this.flags &= ~MASK_FLAG_GENERATION_STARTED;
		this.flags |= generationStarted ? 0 : MASK_FLAG_GENERATION_STARTED;
	}
	public boolean isGenerationStarted() {
		return (this.flags & MASK_FLAG_GENERATION_STARTED) != 0;
	}
	public void setGenerationFinished(boolean generationFinished) {
		this.flags &= ~MASK_FLAG_GENERATION_FINISHED;
		this.flags |= generationFinished ? 0 : MASK_FLAG_GENERATION_FINISHED;
	}
	public boolean isGenerationFinished() {
		return (this.flags & MASK_FLAG_GENERATION_FINISHED) != 0;
	}

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
		if (pos.level() > this.level)
			return;
		// this is basically `pos` but in the coorinate system for the level that this
		// node is on.
		final var levelPos = pos.levelCoords().floorDiv((1 << this.level) / (1 << pos.level()));
		// bail if our target node is not a descendant of this node
		if (this.x != levelPos.x || this.y != levelPos.y || this.z != levelPos.z)
			return;

		if (pos.level() == this.level) {
			this.referenceCount += 1;
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

		// if execution has gotten to this point, we know that the target sector was
		// either this sector, or contained somewhere in this sector's bounds, and that
		// means loading *must* have succeeded. This marks all the ancestors of the
		// target node with an additional weak count.
		this.weakReferenceCount += 1;
	}

	/**
	 * Unloads the sector at the given sector pos.
	 * 
	 * @param pos The position of the target sector.
	 * @return Whether the current node is allowed to be unloaded.
	 */
	public boolean unload(SectorPos pos) {
		if (pos.level() > this.level)
			return this.weakReferenceCount == 0;
		// this is basically `pos` but in the coorinate system for the level that this
		// node is on.
		final var levelPos = pos.levelCoords().floorDiv((1 << this.level) / (1 << pos.level()));
		// bail if our target node is not a descendant of this node
		if (this.x != levelPos.x || this.y != levelPos.y || this.z != levelPos.z)
			return this.weakReferenceCount == 0;

		if (pos.level() == this.level) {
			if (this.referenceCount > 0) {
				setGenerationStarted(false);
				setGenerationFinished(false);
				if (--this.referenceCount == 0)
					this.initialElements = null;
			} else {
				Mod.LOGGER.error("tried to unload a sector with a reference count of 0.");
			}
		}

		boolean mayUnload = true;
		if (this.branch != null) {
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

		return mayUnload && this.weakReferenceCount == 0;
	}

	public GalaxySector lookupSubtree(SectorPos pos) {
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
			final var nnn = this.branch.nnn.lookupSubtree(pos); if (nnn != null) return nnn;
			final var nnp = this.branch.nnp.lookupSubtree(pos); if (nnp != null) return nnp;
			final var npn = this.branch.npn.lookupSubtree(pos); if (npn != null) return npn;
			final var npp = this.branch.npp.lookupSubtree(pos); if (npp != null) return npp;
			final var pnn = this.branch.pnn.lookupSubtree(pos); if (pnn != null) return pnn;
			final var pnp = this.branch.pnp.lookupSubtree(pos); if (pnp != null) return pnp;
			final var ppn = this.branch.ppn.lookupSubtree(pos); if (ppn != null) return ppn;
			final var ppp = this.branch.ppp.lookupSubtree(pos); if (ppp != null) return ppp;
			// @formatter:on
		}

		return null;
	}

	public InitialElement lookupInitial(SectorPos pos, int subnodeIndex) {
		return lookupSubtree(pos).initialElements.get(subnodeIndex);
	}

	public InitialElement lookupInitial(int subnodeIndex) {
		return this.initialElements.get(subnodeIndex);
	}

	public boolean isAtPos(SectorPos pos) {
		return this.level == pos.level() && this.x == pos.levelCoords().x && this.y == pos.levelCoords().y
				&& this.z == pos.levelCoords().z;
	}

	public SectorPos pos() {
		return new SectorPos(this.level, Vec3i.from(this.x, this.y, this.z));
	}

}
