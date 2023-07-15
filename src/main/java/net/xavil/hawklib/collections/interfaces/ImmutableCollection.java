package net.xavil.hawklib.collections.interfaces;

public interface ImmutableCollection {

	/**
	 * This method returns the number of elements in this collection.
	 * 
	 * @return The number of elements in this collection.
	 */
	int size();

	/**
	 * This method returns whether the collection is empty (i.e., contains zero
	 * elements) or not. This method returns {@code true} if and only if
	 * {@link #size()} would have returned {@code 0} if it were called instead of
	 * this method. For some collections, it may be more efficient to call this
	 * method if the only information needed is whether the collection is empty or
	 * not (ex. a naive linked list could check if it has a head element instead of
	 * traversing the list and counting the nodes it visits).
	 * 
	 * @return {@code true} if this collection is empty
	 */
	default boolean isEmpty() {
		return this.size() == 0;
	}

}
