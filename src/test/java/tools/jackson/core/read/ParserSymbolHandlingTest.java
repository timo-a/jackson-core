package tools.jackson.core.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ParserSymbolHandlingTest
    extends JUnit5TestBase
{
    // For [core#148]
    @Test
    void symbolsWithNullBytes() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNull(f, true);
        // and repeat with same factory, just for fun, and to ensure symbol table is fine
        _testSymbolsWithNull(f, true);
    }

    // For [core#148]
    @Test
    void symbolsWithNullChars() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNull(f, false);
        _testSymbolsWithNull(f, false);
    }

    private void _testSymbolsWithNull(JsonFactory f, boolean useBytes) throws Exception
    {
        final String INPUT = "{\"\\u0000abc\" : 1, \"abc\":2}";
        JsonParser parser = useBytes ? f.createParser(ObjectReadContext.empty(), INPUT.getBytes("UTF-8"))
                : f.createParser(ObjectReadContext.empty(), INPUT);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        String currName = parser.currentName();
        if (!"\u0000abc".equals(currName)) {
            fail("Expected \\0abc (4 bytes), '"+currName+"' ("+currName.length()+")");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(1, parser.getIntValue());

        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        currName = parser.currentName();
        if (!"abc".equals(currName)) {
            /*
            for (int i = 0; i < currName.length(); ++i) {
                System.out.println("#"+i+" -> 0x"+Integer.toHexString(currName.charAt(i)));
            }
            */
            fail("Expected 'abc' (3 bytes), '"+currName+"' ("+currName.length()+")");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
        assertEquals(2, parser.getIntValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();
    }

    // // Additional testing inspired by [dataformats-binary#312]; did not
    // // affect JSON backend but wanted to ensure

    @Test
    void symbolsWithNullOnlyNameBytes() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNullOnlyNameBytes(f, true);
        // and repeat with same factory, just for fun, and to ensure symbol table is fine
        _testSymbolsWithNullOnlyNameBytes(f, true);
    }

    @Test
    void symbolsWithNullOnlyNameChars() throws Exception {
        JsonFactory f = new JsonFactory();
        _testSymbolsWithNullOnlyNameBytes(f, false);
        _testSymbolsWithNullOnlyNameBytes(f, false);
    }

    private void _testSymbolsWithNullOnlyNameBytes(JsonFactory f, boolean useBytes) throws Exception
    {
        final String NAME_1 = "\u0000";
        final String NAME_2 = NAME_1 + NAME_1;
        final String NAME_3 = NAME_2 + NAME_1;
        final String NAME_4 = NAME_3 + NAME_1;
        final String QUOTED_NULL = "\\u0000";

        final String INPUT = a2q(String.format("{'%s':1, '%s':2, '%s':3, '%s':4}",
                QUOTED_NULL, QUOTED_NULL + QUOTED_NULL,
                QUOTED_NULL + QUOTED_NULL + QUOTED_NULL,
                QUOTED_NULL + QUOTED_NULL + QUOTED_NULL + QUOTED_NULL
                ));
        try (JsonParser p = useBytes ? f.createParser(ObjectReadContext.empty(), INPUT.getBytes("UTF-8"))
                : f.createParser(ObjectReadContext.empty(), INPUT)) {

            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            _assertNullStrings(NAME_1, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            _assertNullStrings(NAME_2, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(2, p.getIntValue());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            _assertNullStrings(NAME_3, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(3, p.getIntValue());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            _assertNullStrings(NAME_4, p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(4, p.getIntValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private void _assertNullStrings(String exp, String actual) {
        if (exp.length() != actual.length()) {
            fail("Expected "+exp.length()+" nulls, got "+actual.length());
        }
        assertEquals(exp, actual);
    }
}
