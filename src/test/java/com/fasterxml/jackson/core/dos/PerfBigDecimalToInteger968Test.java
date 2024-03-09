package com.fasterxml.jackson.core.dos;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

// For [core#968]]
public class PerfBigDecimalToInteger968Test
{
    private final JsonFactory JSON_F = new JsonFactory();

    // For [core#968]: shouldn't take multiple seconds
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    public void bigIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                Assertions.fail("Should not pass");
            } catch (StreamConstraintsException e) {
                Assertions.assertEquals("BigDecimal scale (-25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }

    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    public void tinyIntegerViaBigDecimal() throws Exception {
        final String DOC = "1e-25000000";

        try (JsonParser p = JSON_F.createParser(DOC)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            try {
                p.getBigIntegerValue();
                Assertions.fail("Should not pass");
            } catch (StreamConstraintsException e) {
                Assertions.assertEquals("BigDecimal scale (25000000) magnitude exceeds the maximum allowed (100000)", e.getMessage());
            }
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            Assertions.fail("Expected token "+expToken+", current token "+actToken);
        }
    }
}
