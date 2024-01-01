package net.xavil.hawklib;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Assert {

	public static RuntimeException failed() {
		throw new AssertionError("assertion failed");
	}

	public static RuntimeException failed(String message) {
		throw new AssertionError(String.format(
				"assertion failed: %s",
				message));
	}

	public static void isTrue(boolean cond) {
		if (!cond)
			throw failed();
	}

	public static void isTrue(boolean cond, String message) {
		if (!cond)
			throw failed(message);
	}

	public static RuntimeException isUnreachable() {
		throw failed("entered unreachable code");
	}

	public static RuntimeException isUnreachable(String message) {
		throw failed(String.format(
				"entered unreachable code: %s", message));
	}

	private static void failedEquality(String op, String a, String b) {
		throw failed(String.format(
				"'%s' %s '%s'",
				a, op, b));
	}
	private static void failedEquality(String op, String a, String b, String message) {
		throw failed(String.format(
				"'%s' %s '%s': %s",
				a, op, b, message));
	}

	public static void isReferentiallyEqual(Object a, Object b) {
		if (a != b)
			failedEquality("==", a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isEqual(Object a, Object b) {
		if (!Objects.equals(a, b))
			failedEquality("eq", a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isReferentiallyNotEqual(Object a, Object b) {
		if (a == b)
			failedEquality("!=", a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	public static void isNotEqual(Object a, Object b) {
		if (Objects.equals(a, b))
			failedEquality("!eq", a == null ? "<null>" : a.toString(), b == null ? "<null>" : b.toString());
	}

	@Nonnull
	public static Object isNotNull(Object a) {
		if (a == null)
			throw failed("object was null");
		return a;
	}

	// @formatter:off
	public static void isEqual(byte a, byte b)              { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isEqual(short a, short b)            { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isEqual(int a, int b)                { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isEqual(long a, long b)              { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isEqual(float a, float b)            { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isEqual(double a, double b)          { if (!(a == b)) failedEquality("==", "" + a, "" + b); }
	public static void isNotEqual(byte a, byte b)           { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isNotEqual(short a, short b)         { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isNotEqual(int a, int b)             { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isNotEqual(long a, long b)           { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isNotEqual(float a, float b)         { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isNotEqual(double a, double b)       { if (!(a != b)) failedEquality("!=", "" + a, "" + b); }
	public static void isGreaterOrEqual(byte a, byte b)     { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreaterOrEqual(short a, short b)   { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreaterOrEqual(int a, int b)       { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreaterOrEqual(long a, long b)     { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreaterOrEqual(float a, float b)   { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreaterOrEqual(double a, double b) { if (!(a >= b)) failedEquality(">=", "" + a, "" + b); }
	public static void isGreater(byte a, byte b)            { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isGreater(short a, short b)          { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isGreater(int a, int b)              { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isGreater(long a, long b)            { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isGreater(float a, float b)          { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isGreater(double a, double b)        { if (!(a  > b)) failedEquality( ">", "" + a, "" + b); }
	public static void isLesserOrEqual(byte a, byte b)      { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesserOrEqual(short a, short b)    { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesserOrEqual(int a, int b)        { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesserOrEqual(long a, long b)      { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesserOrEqual(float a, float b)    { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesserOrEqual(double a, double b)  { if (!(a <= b)) failedEquality("<=", "" + a, "" + b); }
	public static void isLesser(byte a, byte b)             { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }
	public static void isLesser(short a, short b)           { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }
	public static void isLesser(int a, int b)               { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }
	public static void isLesser(long a, long b)             { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }
	public static void isLesser(float a, float b)           { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }
	public static void isLesser(double a, double b)         { if (!(a  < b)) failedEquality( "<", "" + a, "" + b); }

	public static void isEqual(byte a, byte b, String message)              { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isEqual(short a, short b, String message)            { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isEqual(int a, int b, String message)                { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isEqual(long a, long b, String message)              { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isEqual(float a, float b, String message)            { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isEqual(double a, double b, String message)          { if (!(a == b)) failedEquality("==", "" + a, "" + b, message); }
	public static void isNotEqual(byte a, byte b, String message)           { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isNotEqual(short a, short b, String message)         { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isNotEqual(int a, int b, String message)             { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isNotEqual(long a, long b, String message)           { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isNotEqual(float a, float b, String message)         { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isNotEqual(double a, double b, String message)       { if (!(a != b)) failedEquality("!=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(byte a, byte b, String message)     { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(short a, short b, String message)   { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(int a, int b, String message)       { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(long a, long b, String message)     { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(float a, float b, String message)   { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreaterOrEqual(double a, double b, String message) { if (!(a >= b)) failedEquality(">=", "" + a, "" + b, message); }
	public static void isGreater(byte a, byte b, String message)            { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isGreater(short a, short b, String message)          { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isGreater(int a, int b, String message)              { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isGreater(long a, long b, String message)            { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isGreater(float a, float b, String message)          { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isGreater(double a, double b, String message)        { if (!(a  > b)) failedEquality( ">", "" + a, "" + b, message); }
	public static void isLesserOrEqual(byte a, byte b, String message)      { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesserOrEqual(short a, short b, String message)    { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesserOrEqual(int a, int b, String message)        { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesserOrEqual(long a, long b, String message)      { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesserOrEqual(float a, float b, String message)    { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesserOrEqual(double a, double b, String message)  { if (!(a <= b)) failedEquality("<=", "" + a, "" + b, message); }
	public static void isLesser(byte a, byte b, String message)             { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	public static void isLesser(short a, short b, String message)           { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	public static void isLesser(int a, int b, String message)               { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	public static void isLesser(long a, long b, String message)             { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	public static void isLesser(float a, float b, String message)           { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	public static void isLesser(double a, double b, String message)         { if (!(a  < b)) failedEquality( "<", "" + a, "" + b, message); }
	// @formatter:on

}
