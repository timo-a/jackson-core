package tools.jackson.core.json;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Some unit tests to try to exercise part of parser code that
 * deals with symbol (table) management.
 */
class TestWithTonsaSymbols
        extends JUnit5TestBase
{
    /**
     * How many fields to generate? Since maximum symbol table
     * size is defined as 6000 (above which table gets cleared,
     * assuming the name vocabulary is unbounded), let's do something
     * just slightly below it.
     */
    final static int PROP_COUNT = 5000;

    @Test
    void streamReaderParser() throws Exception {
        _testWith(true);
    }

    @Test
    void readerParser() throws Exception {
        _testWith(false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testWith(boolean useStream)
        throws Exception
    {
        JsonFactory f = new JsonFactory();
        String doc = buildDoc(PROP_COUNT);

        // And let's do this multiple times: just so that symbol table
        // state is different between runs.
        for (int x = 0; x < 3; ++x) {
            JsonParser p = useStream ?
                f.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(doc.getBytes("UTF-8")))
                : f.createParser(ObjectReadContext.empty(), new StringReader(doc));
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            for (int i = 0; i < PROP_COUNT; ++i) {
                assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                assertEquals(fieldNameFor(i), p.currentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
            }
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            p.close();
        }
    }

    private String buildDoc(int len)
    {
        StringBuilder sb = new StringBuilder(len * 12);
        sb.append('{');
        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"');
            sb.append(fieldNameFor(i));
            sb.append('"');
            sb.append(':');
            sb.append(i);
        }
        sb.append('}');
        return sb.toString();
    }
}
