package net.xavil.hawklib.hash;

public interface Hashable {

	void appendHash(Hasher hasher);

	default long hash(Hasher hasher) {
		appendHash(hasher);
		return hasher.currentHash();
	}

	default long hash() {
		return hash(FastHasher.create());
	}

}
