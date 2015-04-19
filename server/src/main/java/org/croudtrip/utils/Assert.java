package org.croudtrip.utils;


import java.util.List;

public class Assert {

	private Assert() { }


	public static void assertNotNull(Object... objects) {
		for (Object o : objects) {
			if (o == null) throw new NullPointerException("param is null");
		}
	}


	public static void assertTrue(boolean expression) {
		assertTrue(expression, "expression is false");
	}


	public static void assertTrue(boolean expression, String errorMsg) {
		assertFalse(!expression, errorMsg);
	}


	public static void assertFalse(boolean expression, String errorMsg) {
		if (expression) throw new IllegalArgumentException(errorMsg);
	}


	public static void assertEquals(Object o1, Object o2, String errorMsg) {
		if (o1 == null && o2 == null) return;
		if (o1 == null || o2 == null) throw new IllegalArgumentException(errorMsg);
		if (!o1.equals(o2)) throw new IllegalArgumentException(errorMsg);
	}


	public static void assertValidIdx(List<?> list, int idx) {
		if (idx < 0 || idx >= list.size())
			throw new IndexOutOfBoundsException("idx was " + idx + " but size is " + list.size());
	}

}