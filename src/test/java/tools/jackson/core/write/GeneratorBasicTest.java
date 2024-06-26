package tools.jackson.core.write;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class GeneratorBasicTest
    extends JUnit5TestBase
{
    private final TokenStreamFactory JSON_F = newStreamFactory();

    // // // First, tests for primitive (non-structured) values

    @Test
    void stringWrite() throws Exception
    {
        String[] inputStrings = new String[] { "", "X", "1234567890" };
        for (int useReader = 0; useReader < 2; ++useReader) {
            for (int writeString = 0; writeString < 2; ++writeString) {
                for (int strIx = 0; strIx < inputStrings.length; ++strIx) {
                    String input = inputStrings[strIx];
                    JsonGenerator gen;
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    if (useReader != 0) {
                        gen = JSON_F.createGenerator(ObjectWriteContext.empty(),
                                new OutputStreamWriter(bout, "UTF-8"));
                    } else {
                        gen = JSON_F.createGenerator(ObjectWriteContext.empty(),
                                bout, JsonEncoding.UTF8);
                    }
                    if (writeString > 0) {
                        gen.writeString(input);
                    } else {
                        int len = input.length();
                        char[] buffer = new char[len + 20];
                        // Let's use non-zero base offset too...
                        input.getChars(0, len, buffer, strIx);
                        gen.writeString(buffer, strIx, len);
                    }
                    gen.flush();
                    gen.close();
                    JsonParser jp = JSON_F.createParser(ObjectReadContext.empty(),
                            new ByteArrayInputStream(bout.toByteArray()));

                    JsonToken t = jp.nextToken();
                    assertNotNull(t, "Document \""+utf8String(bout)+"\" yielded no tokens");
                    assertEquals(JsonToken.VALUE_STRING, t);
                    assertEquals(input, jp.getText());
                    assertNull(jp.nextToken());
                    jp.close();
                }
            }
        }
    }

    @Test
    void intValueWrite() throws Exception
    {
        // char[]
        doTestIntValueWrite(false, false);
        doTestIntValueWrite(true, false);
        // byte[]
        doTestIntValueWrite(false, true);
        doTestIntValueWrite(true, true);
    }

    @Test
    void longValueWrite() throws Exception
    {
        // char[]
        doTestLongValueWrite(false, false);
        doTestLongValueWrite(true, false);
        // byte[]
        doTestLongValueWrite(false, true);
        doTestLongValueWrite(true, true);
    }

    @Test
    void booleanWrite() throws Exception
    {
        for (int i = 0; i < 4; ++i) {
            boolean state = (i & 1) == 0;
            boolean pad = (i & 2) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
            gen.writeBoolean(state);
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = Boolean.valueOf(state).toString();
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, t);
            assertNull(jp.nextToken());
            jp.close();
        }
    }

    @Test
    void nullWrite()
            throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            boolean pad = (i & 1) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
            gen.writeNull();
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = "null";
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(JsonToken.VALUE_NULL, t);
            assertNull(jp.nextToken());
            jp.close();
        }
    }

    // // Then root-level output testing

    @Test
    void rootIntsWrite() throws Exception {
        _testRootIntsWrite(false);
        _testRootIntsWrite(true);
    }

    private void _testRootIntsWrite(boolean useBytes) throws Exception
    {
         StringWriter sw = new StringWriter();
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         JsonGenerator gen;

         if (useBytes) {
             gen = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
         } else {
             gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
         }

         gen.writeNumber(1);
         gen.writeNumber((short) 2); // for test coverage
         gen.writeNumber(-13);
         gen.close();

         String docStr = useBytes ? utf8String(bytes) : sw.toString();
         try (JsonParser jp = createParserUsingReader(docStr)) {
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(1, jp.getIntValue());
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(2, jp.getIntValue());
             assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
             assertEquals(-13, jp.getIntValue());
         }
     }

    // Convenience methods

    @Test
    void fieldValueWrites() throws Exception {
        _testFieldValueWrites(false);
        _testFieldValueWrites(true);
    }

    public void _testFieldValueWrites(boolean useBytes) throws Exception
    {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator gen;

        if (useBytes) {
            gen = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
        } else {
            gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        }

        gen.writeStartObject();
        gen.writeNumberProperty("short", (short) 3);
        gen.writeNumberProperty("int", 3);
        gen.writeNumberProperty("long", 3L);
        gen.writeNumberProperty("big", new BigInteger("1707"));
        gen.writeNumberProperty("double", 0.25);
        gen.writeNumberProperty("float", -0.25f);
        gen.writeNumberProperty("decimal", new BigDecimal("17.07"));
        gen.writeEndObject();
        gen.close();

        String docStr = useBytes ? utf8String(bytes) : sw.toString();

        assertEquals("{\"short\":3,\"int\":3,\"long\":3,\"big\":1707,\"double\":0.25,\"float\":-0.25,\"decimal\":17.07}",
                docStr.trim());
    }

    /**
     * Test to verify that output context actually contains useful information
     */
    @Test
    void outputContext() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        TokenStreamContext ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inRoot());

        gen.writeStartObject();
        assertTrue(gen.streamWriteContext().inObject());

        gen.writeName("a");
        assertEquals("a", gen.streamWriteContext().currentName());

        gen.writeStartArray();
        assertTrue(gen.streamWriteContext().inArray());

        gen.writeStartObject();
        assertTrue(gen.streamWriteContext().inObject());

        gen.writeName("b");
        ctxt = gen.streamWriteContext();
        assertEquals("b", ctxt.currentName());
        gen.writeNumber(123);
        assertEquals("b", ctxt.currentName());

        gen.writeName("c");
        assertEquals("c", gen.streamWriteContext().currentName());
        gen.writeNumber(5);
//        assertEquals("c", gen.getOutputContext().currentName());

        gen.writeName("d");
        assertEquals("d", gen.streamWriteContext().currentName());

        gen.writeStartArray();
        ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inArray());
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals(0, ctxt.getEntryCount());

        gen.writeBoolean(true);
        ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inArray());
        // NOTE: index still refers to currently output entry
        assertEquals(0, ctxt.getCurrentIndex());
        assertEquals(1, ctxt.getEntryCount());

        gen.writeNumber(3);
        ctxt = gen.streamWriteContext();
        assertTrue(ctxt.inArray());
        assertEquals(1, ctxt.getCurrentIndex());
        assertEquals(2, ctxt.getEntryCount());

        gen.writeEndArray();
        assertTrue(gen.streamWriteContext().inObject());

        gen.writeEndObject();
        assertTrue(gen.streamWriteContext().inArray());

        gen.writeEndArray();
        assertTrue(gen.streamWriteContext().inObject());

        gen.writeEndObject();

        assertTrue(gen.streamWriteContext().inRoot());

        gen.close();
    }

    @Test
    void getOutputTarget() throws Exception
    {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        assertSame(out, gen.streamWriteOutputTarget());
        gen.close();

        StringWriter sw = new StringWriter();
        gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        assertSame(sw, gen.streamWriteOutputTarget());
        gen.close();
    }

    // for [core#195]
    @Test
    void getOutputBufferd() throws Exception
    {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        _testOutputBuffered(gen);
        gen.close();

        StringWriter sw = new StringWriter();
        gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
        _testOutputBuffered(gen);
        gen.close();
    }

    private void _testOutputBuffered(JsonGenerator gen) throws IOException
    {
        gen.writeStartArray(); // 1 byte
        gen.writeNumber(1234); // 4 bytes
        assertEquals(5, gen.streamWriteOutputBuffered());
        gen.flush();
        assertEquals(0, gen.streamWriteOutputBuffered());
        gen.writeEndArray();
        assertEquals(1, gen.streamWriteOutputBuffered());
        gen.close();
        assertEquals(0, gen.streamWriteOutputBuffered());
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void doTestIntValueWrite(boolean pad, boolean useBytes) throws Exception
    {
        int[] VALUES = new int[] {
            0, 1, -9, 32, -32, 57, 189, 2017, -9999, 13240, 123456,
            1111111, 22222222, 123456789,
            7300999, -7300999,
            99300999, -99300999,
            999300999, -999300999,
            1000300999, 2000500126, -1000300999, -2000500126,
            Integer.MIN_VALUE, Integer.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            int VALUE = VALUES[i];
            String docStr;
            JsonParser p;

            if (useBytes) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = utf8String(bytes);
                p = JSON_F.createParser(ObjectReadContext.empty(), bytes.toByteArray());
            } else {
                StringWriter sw = new StringWriter();
                JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = sw.toString();
                p = JSON_F.createParser(ObjectReadContext.empty(), docStr);
            }
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (Exception e) {
                fail("Problem with value "+VALUE+", document ["+docStr+"]: "+e.getMessage());
            }
            assertNotNull(t, "Document \""+docStr+"\" yielded no tokens");
            // Number are always available as lexical representation too
            String exp = ""+VALUE;
            if (!exp.equals(p.getText())) {
                fail("Expected '"+exp+"', got '"+p.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, p.getIntValue());
            assertNull(p.nextToken());
            p.close();
        }
    }

    private void doTestLongValueWrite(boolean pad, boolean useBytes) throws Exception
    {
        long[] VALUES = new long[] {
            0L, 1L, -1L, 2000100345, -12005002294L,
            5111222333L, -5111222333L,
            65111222333L, -65111222333L,
            123456789012L, -123456789012L,
            123456789012345L, -123456789012345L,
            123456789012345789L, -123456789012345789L,
            Long.MIN_VALUE, Long.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            long VALUE = VALUES[i];
            String docStr;
            JsonParser p;

            if (useBytes) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), bytes);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = utf8String(bytes);
                p = JSON_F.createParser(ObjectReadContext.empty(), bytes.toByteArray());
            } else {
                StringWriter sw = new StringWriter();
                JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw);
                gen.writeNumber(VALUE);
                if (pad) {
                    gen.writeRaw(" ");
                }
                gen.close();
                docStr = sw.toString();
                p = JSON_F.createParser(ObjectReadContext.empty(), docStr);
            }
            JsonToken t = null;
            try {
                t = p.nextToken();
            } catch (JacksonException e) {
                fail("Problem with number "+VALUE+", document ["+docStr+"]: "+e.getMessage());
            }
            assertNotNull(t, "Document \""+docStr+"\" yielded no tokens");
            String exp = ""+VALUE;
            if (!exp.equals(p.getText())) {
                fail("Expected '"+exp+"', got '"+p.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, p.getLongValue());
            assertNull(p.nextToken());
            p.close();
        }
    }
}

