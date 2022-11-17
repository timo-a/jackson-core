package tools.jackson.core.read;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;

public class ParserErrorHandlingTest
    extends tools.jackson.core.BaseTest
{
    public void testInvalidKeywordsBytes() {
        _testInvalidKeywords(MODE_INPUT_STREAM);
        _testInvalidKeywords(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidKeywords(MODE_DATA_INPUT);
    }

    public void testInvalidKeywordsChars() {
        _testInvalidKeywords(MODE_READER);
    }

    // Tests for [core#105] ("eager number parsing misses errors")
    public void testMangledIntsBytes() {
        _testMangledNumbersInt(MODE_INPUT_STREAM);
        _testMangledNumbersInt(MODE_INPUT_STREAM_THROTTLED);

        // 02-Jun-2017, tatu: Fails to fail; should check whether this is expected
        //   (since DataInput can't do look-ahead)
//        _testMangledNumbersInt(MODE_DATA_INPUT);
    }

    public void testMangledFloatsBytes() {
        _testMangledNumbersFloat(MODE_INPUT_STREAM);
        _testMangledNumbersFloat(MODE_INPUT_STREAM_THROTTLED);

        // 02-Jun-2017, tatu: Fails as expected, unlike int one. Bit puzzling...
        _testMangledNumbersFloat(MODE_DATA_INPUT);
    }

    public void testMangledNumbersChars() {
        _testMangledNumbersInt(MODE_READER);
        _testMangledNumbersFloat(MODE_READER);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _testInvalidKeywords(int mode)
    {
        doTestInvalidKeyword1(mode, "nul");
        doTestInvalidKeyword1(mode, "Null");
        doTestInvalidKeyword1(mode, "nulla");
        doTestInvalidKeyword1(mode, "fal");
        doTestInvalidKeyword1(mode, "False");
        doTestInvalidKeyword1(mode, "fals0");
        doTestInvalidKeyword1(mode, "falsett0");
        doTestInvalidKeyword1(mode, "tr");
        doTestInvalidKeyword1(mode, "truE");
        doTestInvalidKeyword1(mode, "treu");
        doTestInvalidKeyword1(mode, "trueenough");
        doTestInvalidKeyword1(mode, "C");
    }

    private void doTestInvalidKeyword1(int mode, String value)
    {
        String doc = "{ \"key1\" : "+value+" }";
        JsonParser p = createParser(mode, doc);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try {
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        p = createParser(mode, doc);
        try {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (StreamReadException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }
    }

    private void _testMangledNumbersInt(int mode)
    {
        JsonParser p = createParser(mode, "123true");
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }

    private void _testMangledNumbersFloat(int mode)
    {
        // Also test with floats
        JsonParser p = createParser(mode, "1.5false");
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (StreamReadException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }
}