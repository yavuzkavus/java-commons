package com.readjournal.util;

import static com.readjournal.util.StringUtil.ifEmpty;
import static com.readjournal.util.StringUtil.ifNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringUtilTest {
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	public void ifNullTest() {
		assertEquals("", ifNull(null));
		assertEquals("", ifNull(""));
		assertEquals("not null", ifNull("not null"));

		assertNull(ifNull(null, null));
		assertEquals("", ifNull(null, ""));
		assertEquals(" ", ifNull(null, " "));
		assertEquals("1", ifNull("1", null));
		assertEquals("2", ifNull(null, "2"));
		assertEquals("1", ifNull("1", "2"));

		assertNull(ifNull(null, null, null, null, null));
		assertEquals("1", ifNull(null, null, null, "1", "2", null));
	}

	@Test
	public void ifEmptyTest() {
		assertNull(null, ifEmpty(null));
		assertNull(null, ifEmpty(""));
		assertNull(null, ifEmpty("   "));
		assertNull(null, ifEmpty("\t\n "));
		assertEquals("not empty", ifEmpty("not empty"));

		assertNull(ifEmpty(null, null));
		assertNull(ifEmpty("", null));
		assertNull(ifEmpty("   ", null));
		assertEquals("", ifEmpty(null, ""));
		assertEquals("1", ifEmpty(null, "1"));
		assertEquals("1", ifEmpty("", "1"));
		assertEquals("1", ifEmpty("      \t\r\n  ", "1"));
		assertEquals("1", ifEmpty("1", null));
		assertEquals("2", ifEmpty(null, "2"));
		assertEquals("1", ifEmpty("1", "2"));

		assertNull(ifEmpty(null, null, null, null, null));
		assertEquals("1", ifEmpty(null, "", " ", "\t \r \n ", "1", "2", null));
	}

}
