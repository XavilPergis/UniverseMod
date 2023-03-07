package net.xavil.util;

public final class Assert {
	
	public static RuntimeException isUnreachable() {
		throw new AssertionError("entered unreachable code");
	}

	public static void isNotEqual(int a, int b) {
		if (a == b) {
			throw new AssertionError("lhs and rhs were equal: value=" + a);
		}
	}

}
