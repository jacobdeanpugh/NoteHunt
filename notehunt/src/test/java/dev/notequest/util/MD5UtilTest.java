package dev.notequest.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MD5UtilTest {

    @Test
    public void testMd5HexKnownValue() {
        // Test against a known MD5 hash
        String input = "hello";
        String expected = "5d41402abc4b2a76b9719d911017c592";
        String result = MD5Util.md5Hex(input);
        assertEquals(expected, result, "MD5 of 'hello' should match known value");
    }

    @Test
    public void testMd5HexEmptyString() {
        // Test MD5 of empty string
        String input = "";
        String expected = "d41d8cd98f00b204e9800998ecf8427e";
        String result = MD5Util.md5Hex(input);
        assertEquals(expected, result, "MD5 of empty string should match known value");
    }

    @Test
    public void testMd5HexDifferentInputsDiffer() {
        // Test that different inputs produce different hashes
        String hash1 = MD5Util.md5Hex("hello");
        String hash2 = MD5Util.md5Hex("world");
        assertNotEquals(hash1, hash2, "Different inputs should produce different MD5 hashes");
    }

    @Test
    public void testMd5HexDeterministic() {
        // Test that same input always produces same hash
        String input = "hello";
        String hash1 = MD5Util.md5Hex(input);
        String hash2 = MD5Util.md5Hex(input);
        assertEquals(hash1, hash2, "MD5 should be deterministic");
    }

    @Test
    public void testMd5HexWithUnicodeAndSpecialChars() {
        // Test that unicode and special characters don't throw exceptions
        String input = "hello™ café éàü 你好 !@#$%^&*()";
        assertDoesNotThrow(() -> MD5Util.md5Hex(input), "MD5 should handle unicode and special chars");
    }
}
