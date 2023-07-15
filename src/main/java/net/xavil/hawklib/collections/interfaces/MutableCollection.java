package net.xavil.hawklib.collections.interfaces;

import net.xavil.hawklib.collections.CollectionHint;

public interface MutableCollection extends ImmutableCollection {

	/**
	 * This method removes all elements from this collection.
	 */
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

	/**
	 * This method provides a usage hint to this collection, indicating the manner
	 * in which this collection is intended to be used.
	 * <p>
	 * For example, vectors can provide constant-time insertion and removal
	 * operations, but lose the ordering of the elements in the vector by doing so.
	 * By default, vectors will go with the more general option and preserve
	 * ordering, but this method allows one to signal that this behavior is not
	 * required.
	 * </p>
	 * 
	 * @param hint The hint to apply to the collection.
	 * @return A collection with the hint applied, typically just {@code this}.
	 */
	default MutableCollection hint(CollectionHint hint) {
		return this;
	}

}
