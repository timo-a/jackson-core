package tools.jackson.core.write;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RawStringWriteTest extends JUnit5TestBase
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    /**
     * Unit test for "JsonGenerator.writeRawUTF8String()"
     */
    @Test
    void utf8RawStrings() throws Exception
    {
        // Let's create set of Strings to output; no ctrl chars as we do raw
        List<byte[]> strings = generateStrings(new Random(28), 750000, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream(16000);
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8);
        g.writeStartArray();
        for (byte[] str : strings) {
            g.writeRawUTF8String(str, 0, str.length);
        }
        g.writeEndArray();
        g.close();
        byte[] json = out.toByteArray();

        // Ok: let's verify that stuff was written out ok
        JsonParser jp = JSON_F.createParser(ObjectReadContext.empty(), json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (byte[] inputBytes : strings) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            String string = jp.getText();
            byte[] outputBytes = string.getBytes("UTF-8");
            assertEquals(inputBytes.length, outputBytes.length);
            assertArrayEquals(inputBytes, outputBytes);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    /**
     * Unit test for "JsonGenerator.writeUTF8String()", which needs
     * to handle escaping properly
     */
    @Test
    void utf8StringsWithEscaping() throws Exception
    {
        // Let's create set of Strings to output; do include control chars too:
        List<byte[]> strings = generateStrings(new Random(28), 720000, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream(16000);
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), out, JsonEncoding.UTF8);
        g.writeStartArray();

        for (byte[] str : strings) {
            g.writeUTF8String(str, 0, str.length);
            g.writeRaw('\n');
        }
        g.writeEndArray();
        g.close();
        byte[] json = out.toByteArray();

        // Ok: let's verify that stuff was written out ok
        JsonParser jp = JSON_F.createParser(ObjectReadContext.empty(), json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (byte[] inputBytes : strings) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            String string = jp.getText();

            byte[] outputBytes = string.getBytes("UTF-8");
            assertEquals(inputBytes.length, outputBytes.length);
            assertArrayEquals(inputBytes, outputBytes);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    @Test
    void writeRawWithSerializable() throws Exception
    {
        _testWriteRawWithSerializable(JSON_F, true);
        _testWriteRawWithSerializable(JSON_F, false);
    }

    private void _testWriteRawWithSerializable(JsonFactory f, boolean useBytes) throws Exception
    {
        JsonGenerator g;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();

        if (useBytes) {
            g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        } else {
            g = f.createGenerator(ObjectWriteContext.empty(), sw);
        }

        g.writeStartArray();
        g.writeRawValue(new SerializedString("\"foo\""));
        g.writeRawValue(new SerializedString("12"));
        g.writeRaw(new SerializedString(", false"));
        g.writeEndArray();
        g.close();

        JsonParser p = useBytes
                ? f.createParser(ObjectReadContext.empty(), bytes.toByteArray())
                : f.createParser(ObjectReadContext.empty(), sw.toString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foo", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(12, p.getIntValue());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private List<byte[]> generateStrings(Random rnd, int totalLength, boolean includeCtrlChars)
        throws IOException
    {
        ArrayList<byte[]> strings = new ArrayList<>();
        do {
            int len = 2;
            int bits = rnd.nextInt(13);
            while (--bits >= 0) {
                len += len;
            }
            len = 1 + ((len + len) / 3);
            String str = generateString(rnd, len, includeCtrlChars);
            byte[] bytes = str.getBytes("UTF-8");
            strings.add(bytes);
            totalLength -= bytes.length;
        } while (totalLength > 0);
        return strings;
    }

    private String generateString(Random rnd, int length, boolean includeCtrlChars)
    {
        StringBuilder sb = new StringBuilder(length);
        do {
            int i;
            switch (rnd.nextInt(3)) {
            case 0: // 3 byte one
                i = 2048 + rnd.nextInt(16383);
                break;
            case 1: // 2 byte
                i = 128 + rnd.nextInt(1024);
                break;
            default: // ASCII
                i = rnd.nextInt(192);
                if (!includeCtrlChars) {
                    i += 32;
                    // but also need to avoid backslash, double-quote
                    if (i == '\\' || i == '"') {
                        i = '@'; // just arbitrary choice
                    }
                }
            }
            sb.append((char) i);
        } while (sb.length() < length);
        return sb.toString();
    }
}
