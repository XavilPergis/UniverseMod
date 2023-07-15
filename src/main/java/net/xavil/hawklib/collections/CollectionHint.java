package net.xavil.hawklib.collections;

public final class CollectionHint {

	/**
	 * Hints that an ordered-by-default collection will not be used in a way that
	 * depends on the ordering of its elements. Some data structures (like hashmaps)
	 * are fundamentally unordered, and will not behave differently when this hint
	 * is applied to them.
	 * 
	 * Collections with this hint applied are free to reorder their contents in any
	 * ways they wish to.
	 */
	public static final CollectionHint UNORDERED = new CollectionHint("unordered");

	private final String id;

	private CollectionHint(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Collection Hint '" + this.id + "'";
	}

}
