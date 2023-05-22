package net.xavil.util.collections.interfaces;

import net.xavil.util.collections.CollectionHint;

public interface MutableCollection extends ImmutableCollection {

	void clear();

	/**
	 * This method signals that this collection will probably not be used for
	 * writing much more, and will probably be kept around for a long time. In this
	 * case, it might make sense to do operations that may be expensive, but reduce
	 * memory footprint or increase read throughput.
	 * <p>
	 * Such operations might include shrinking a vector's capacity to its current
	 * size, rebalancing a tree, or cleaning up graveyard markers in an open hash
	 * map.
	 * </p>
	 */
	default void optimize() {
	}
	
	default MutableCollection hint(CollectionHint hint) {
		return this;
	}

}
