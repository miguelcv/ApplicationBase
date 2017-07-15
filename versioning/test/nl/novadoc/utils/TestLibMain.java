package nl.novadoc.utils;

import static org.junit.Assert.*;
import nl.novadoc.utils.unittests.JUnitUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestLibMain {

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testTests() {
		JUnitUtils.assertIsClassUnderTest(LibMain.class, TestLibMain.class);
	}
	
	@Test
	public void testSomething() {
		fail("Write the fine Unit tests!");
	}
}
