package tools.jackson.core;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.json.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests to verify functioning of {@link Version} class.
 */
public class ComponentVersionsTest
    extends JUnit5TestBase
{
    @Test
    public void testCoreVersions() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        assertVersion(f.version());
        try (JsonParser jp =  f.createParser(ObjectReadContext.empty(),
                new StringReader("true"))) {
            assertVersion(jp.version());
        }
        try (JsonGenerator jg = f.createGenerator(ObjectWriteContext.empty(),
                new ByteArrayOutputStream())) {
            assertVersion(jg.version());
        }
    }

    @Test
    public void testEquality() {
        Version unk = Version.unknownVersion();
        assertEquals("0.0.0", unk.toString());
        assertEquals("//0.0.0", unk.toFullString());
        assertTrue(unk.equals(unk));

        Version other = new Version(2, 8, 4, "",
                "groupId", "artifactId");
        assertEquals("2.8.4", other.toString());
        assertEquals("groupId/artifactId/2.8.4", other.toFullString());

        // [core#1141]: Avoid NPE for snapshot-info
        Version unk2 = new Version(0, 0, 0, null, null, null);
        assertEquals(unk, unk2);
    }

    @Test
    public void testMisc() {
        Version unk = Version.unknownVersion();
        int hash = unk.hashCode();
        // Happens to be 0 at this point (Jackson 2.16)
        assertEquals(0, hash);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void assertVersion(Version v)
    {
        assertEquals(PackageVersion.VERSION, v);
    }
}