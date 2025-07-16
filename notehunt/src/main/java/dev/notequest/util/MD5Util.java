package dev.notequest.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] digestBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            return toHex(digestBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 Algorithim not found");
        }
    }

    private static String toHex(byte[] digestBytes) {
        StringBuilder sb = new StringBuilder(digestBytes.length * 2);

        for (byte b : digestBytes) {
            // & 0xFF ensures the byte is treated as unsigned
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
