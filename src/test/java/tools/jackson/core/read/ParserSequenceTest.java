package tools.jackson.core.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.util.JsonParserSequence;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
class ParserSequenceTest
    extends JUnit5TestBase
{
    @Test
    void simple() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 1 ]");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 2 ]");
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(1, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertFalse(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());
        assertToken(JsonToken.START_ARRAY, seq.nextToken());

        // first parser ought to be closed now
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());
        assertTrue(p1.isClosed());
        assertFalse(p2.isClosed());
        assertFalse(seq.isClosed());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(seq.isClosed());

        seq.close();
    }

    @Test
    void multiLevel() throws Exception
    {
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "[ 1 ] ");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), " 5");
        JsonParser p3 = JSON_FACTORY.createParser(ObjectReadContext.empty(), " { } ");
        JsonParserSequence seq1 = JsonParserSequence.createFlattened(true, p1, p2);
        JsonParserSequence seq = JsonParserSequence.createFlattened(false, seq1, p3);
        assertEquals(3, seq.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());

        assertToken(JsonToken.START_OBJECT, seq.nextToken());
        assertToken(JsonToken.END_OBJECT, seq.nextToken());

        assertNull(seq.nextToken());
        assertTrue(p1.isClosed());
        assertTrue(p2.isClosed());
        assertTrue(p3.isClosed());
        assertTrue(seq.isClosed());
    }

    // for [jackson-core#296]
    @Test
    void initializationDisabled() throws Exception
    {
        // // First, with old legacy settings

        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "1 2");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "3 true");
        assertToken(JsonToken.VALUE_NUMBER_INT, p1.nextToken());
        assertEquals(1, p1.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertEquals(3, p2.getIntValue());

        // with legacy settings, will see neither '1' nor '3'

        JsonParserSequence seq = JsonParserSequence.createFlattened(false, p1, p2);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertNull(seq.nextToken());
        seq.close();
    }

    // for [jackson-core#296]
    @Test
    void initializationEnabled() throws Exception
    {
        // // and then with new "check for current":
        JsonParser p1 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "1 2");
        JsonParser p2 = JSON_FACTORY.createParser(ObjectReadContext.empty(), "3 true");
        assertToken(JsonToken.VALUE_NUMBER_INT, p1.nextToken());
        assertEquals(1, p1.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertEquals(3, p2.getIntValue());

        // with new settings, both '1' and '3' will be visible

        JsonParserSequence seq = JsonParserSequence.createFlattened(true, p1, p2);
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(1, seq.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(2, seq.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, seq.nextToken());
        assertEquals(3, seq.getIntValue());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertNull(seq.nextToken());
        seq.close();
    }
}
