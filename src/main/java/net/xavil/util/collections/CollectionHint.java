package net.xavil.util.collections;

import net.xavil.util.collections.interfaces.ImmutableCollection;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.collections.interfaces.MutableMultiMap;

public final class CollectionHint {

	private static final MutableMultiMap<Class<? extends ImmutableCollection>, CollectionHint> HINT_SETS = MutableMultiMap
			.hashMultiMap();

	public static final CollectionHint UNORDERED = new CollectionHint(MutableList.class, "unordered");

	private final Class<? extends ImmutableCollection> clazz;
	private final String id;

	private CollectionHint(Class<? extends ImmutableCollection> clazz, String id) {
		this.clazz = clazz;
		this.id = id;
		HINT_SETS.insert(clazz, this);
	}

	public static boolean supportsHint(Class<? extends ImmutableCollection> clazz, CollectionHint hint) {
		return HINT_SETS.contains(clazz, hint);
	}

}
