package edu.upenn.cis;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;

public class DictTest {
	
	Dict d;
	
	@Before
	public void setup() {
		d = new Dict();
	}	
	
	@Test
	public void testMaxEmpty() {
		assertEquals(0, d.max());
	}
	
	@Test
	public void testMaxNonEmpty() {
		ArrayList<Integer> out = new ArrayList<Integer>();
		String in = "thisisalloneword";
		d.ConvertWhitespaceDelimitedLine(in, out);
		assertEquals(1,d.max());
	}

	@Test
	public void testIsWSAllWhitespace() {
		assertTrue(Dict.is_ws(' '));
		assertTrue(Dict.is_ws('\t'));
	}
	
	@Test
	public void testIsWSNotWhitespace() {
		assertFalse(Dict.is_ws('x'));
		assertFalse(Dict.is_ws('.'));
		assertFalse(Dict.is_ws('¶'));
	}
	
	@Test
	public void testConvertWhitespaceDelimitedNoWhitespace() {
		ArrayList<Integer> out = new ArrayList<Integer>();
		String in = "thisisalloneword";
		d.ConvertWhitespaceDelimitedLine(in, out);
		Integer[] result = {1};
		ArrayList<Integer> expected = new ArrayList<Integer>(Arrays.asList(result));
		assertEquals(expected, out);
		
	}
}
