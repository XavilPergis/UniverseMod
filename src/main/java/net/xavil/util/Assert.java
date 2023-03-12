package net.xavil.util;

public final class Assert {

	public static RuntimeException isUnreachable() {
		throw new AssertionError("entered unreachable code");
	}

	private static void failedEquality(String a, String b) {
		throw new AssertionError(
				String.format("assertion failed: lhs with value '%s' and rhs with value '%s' are not equal.", a, b));
	}

	private static void failedInequality(String a, String b) {
		throw new AssertionError(String.format("assertion failed: lhs and rhs with value '%s' are equal.", a));
	}

	public static void isReferentiallyEqual(Object a, Object b) {
		if (a != b)
			failedEquality(a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isEqual(Object a, Object b) {
		if (!a.equals(b))
			failedEquality(a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isReferentiallyNotEqual(Object a, Object b) {
		if (a == b)
			failedInequality(a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isNotEqual(Object a, Object b) {
		if (a.equals(b))
			failedInequality(a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	// @formatter:off
	public static void isEqual(byte a, byte b)     { if (a != b) failedEquality("" + a, "" + b); }
	public static void isEqual(short a, short b)   { if (a != b) failedEquality("" + a, "" + b); }
	public static void isEqual(int a, int b)       { if (a != b) failedEquality("" + a, "" + b); }
	public static void isEqual(long a, long b)     { if (a != b) failedEquality("" + a, "" + b); }
	public static void isEqual(float a, float b)   { if (a != b) failedEquality("" + a, "" + b); }
	public static void isEqual(double a, double b) { if (a != b) failedEquality("" + a, "" + b); }
	public static void isNotEqual(byte a, byte b)     { if (a == b) failedInequality("" + a, "" + b); }
	public static void isNotEqual(short a, short b)   { if (a == b) failedInequality("" + a, "" + b); }
	public static void isNotEqual(int a, int b)       { if (a == b) failedInequality("" + a, "" + b); }
	public static void isNotEqual(long a, long b)     { if (a == b) failedInequality("" + a, "" + b); }
	public static void isNotEqual(float a, float b)   { if (a == b) failedInequality("" + a, "" + b); }
	public static void isNotEqual(double a, double b) { if (a == b) failedInequality("" + a, "" + b); }
	// @formatter:on

	public static void isTrue(boolean cond) {
		if (!cond) {
			throw new AssertionError("assertion failed");
		}
	}

}
