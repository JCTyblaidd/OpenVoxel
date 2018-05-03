package net.openvoxel.utility.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {


	@Test
	@DisplayName("JsonParser - floating")
	void testFloatingPoints() {
		JsonParser parser = new JsonParser("0.0e0  ".getBytes());
		assertEquals(0.0,parser.seekDouble());

		parser = new JsonParser("-1.05343e-12  ".getBytes());
		assertEquals(-1.05343e-12,parser.seekDouble());
	}

	@Test
	@DisplayName("JsonParser - expected")
	void testValidParse() {
		String src = "{\"test\" : [100,  -9223372036854775808 , false,true,null ] }  ";
		JsonParser parser = new JsonParser(src.getBytes());

		parser.reset();
		assertTrue(parser.isMapBegin());

		assertTrue(parser.seekNext());
		assertEquals("test",parser.seekString());

		assertTrue(parser.seekNext());
		assertTrue(parser.isMapping());

		assertTrue(parser.seekNext());
		assertTrue(parser.isArrayBegin());

		assertTrue(parser.seekNext());
		assertEquals(100,parser.seekLong());

		assertTrue(parser.seekNext());
		assertTrue(parser.isSeparator());

		assertTrue(parser.seekNext());
		assertEquals(Long.MIN_VALUE,parser.seekLong());

		assertTrue(parser.seekNext());
		assertTrue(parser.isSeparator());

		assertTrue(parser.seekNext());
		assertTrue(parser.seekFalse());

		assertTrue(parser.seekNext());
		assertTrue(parser.isSeparator());

		assertTrue(parser.seekNext());
		assertTrue(parser.seekTrue());

		assertTrue(parser.seekNext());
		assertTrue(parser.isSeparator());

		assertTrue(parser.seekNext());
		assertTrue(parser.seekNull());

		assertTrue(parser.seekNext());
		assertTrue(parser.isArrayEnd());

		assertTrue(parser.seekNext());
		assertTrue(parser.isMapEnd());

		assertFalse(parser.seekNext());
	}

}