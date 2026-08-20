package com.company.devvault.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static String sha256(InputStream input) {
        return digest("SHA-256", input);
    }

    public static String sha1(InputStream input) {
        return digest("SHA-1", input);
    }

    public static String md5(InputStream input) {
        return digest("MD5", input);
    }

    private static String digest(String algorithm, InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute " + algorithm + " checksum", e);
        }
    }
}