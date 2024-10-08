package tools.jackson.core.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.filter.FilteringGeneratorDelegate;
import tools.jackson.core.filter.JsonPointerBasedFilter;
import tools.jackson.core.filter.TokenFilter.Inclusion;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.UTF8JsonGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UTF8GeneratorTest extends JUnit5TestBase
{
    private final TokenStreamFactory JSON_F = newStreamFactory();

    private final JsonFactory JSON_MAX_NESTING_1 = JsonFactory.builder()
            .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(1).build())
            .build();

    @Test
    void utf8Issue462() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOContext ioc = testIOContext();
        JsonGenerator gen = new UTF8JsonGenerator(ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        String str = "Natuurlijk is alles gelukt en weer een tevreden klant\uD83D\uDE04";
        int length = 4000 - 38;

        for (int i = 1; i <= length; ++i) {
            gen.writeNumber(1);
        }
        gen.writeString(str);
        gen.flush();
        gen.close();

        // Also verify it's parsable?
        JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), bytes.toByteArray());
        for (int i = 1; i <= length; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(str, p.getText());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    void nestingDepthWithSmallLimit() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartObject();
            gen.writeName("array");
            gen.writeStartArray();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)");
        }
    }

    @Test
    void nestingDepthWithSmallLimitNestedObject() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(ObjectWriteContext.empty(), bytes)) {
            gen.writeStartObject();
            gen.writeName("object");
            gen.writeStartObject();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)");
        }
    }

    // for [core#115]
    @Test
    void surrogatesWithRaw() throws Exception
    {
        final String VALUE = q("\ud83d\ude0c");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);
        g.writeStartArray();
        g.writeRaw(VALUE);
        g.writeEndArray();
        g.close();

        final byte[] JSON = out.toByteArray();

        JsonParser jp = JSON_F.createParser(ObjectReadContext.empty(), JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String str = jp.getText();
        assertEquals(2, str.length());
        assertEquals((char) 0xD83D, str.charAt(0));
        assertEquals((char) 0xDE0C, str.charAt(1));
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }

    @Test
    void filteringWithEscapedChars() throws Exception
    {
        final String SAMPLE_WITH_QUOTES = "\b\t\f\n\r\"foo\"\u0000";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        JsonGenerator g = JSON_F.createGenerator(ObjectWriteContext.empty(), out);

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(g,
                new JsonPointerBasedFilter("/escapes"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
        );

        //final String JSON = "{'a':123,'array':[1,2],'escapes':'\b\t\f\n\r\"foo\"\u0000'}";

        gen.writeStartObject();

        gen.writeName("a");
        gen.writeNumber(123);

        gen.writeName("array");
        gen.writeStartArray();
        gen.writeNumber((short) 1);
        gen.writeNumber((short) 2);
        gen.writeEndArray();

        gen.writeName("escapes");

        final byte[] raw = utf8Bytes(SAMPLE_WITH_QUOTES);
        gen.writeUTF8String(raw, 0, raw.length);

        gen.writeEndObject();
        gen.close();

        try (JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), out.toByteArray())) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("escapes", p.currentName());
    
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(SAMPLE_WITH_QUOTES, p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
        }
    }
}
