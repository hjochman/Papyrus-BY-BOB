package com.hape.payrus.data;

import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String ENCRYPTED_PREFIX = "ENC_PAPYRUS_V1:";
    private static final int ITERATION_COUNT = 100000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12; // Standard for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128;

    public static boolean isEncrypted(String text) {
        if (text == null) return false;
        return text.trim().startsWith(ENCRYPTED_PREFIX);
    }

    public static String encrypt(String plainText, String password) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (plainText == null) {
            plainText = "";
        }

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        random.nextBytes(iv);

        SecretKey secretKey = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Format: [SALT (16)] + [IV (12)] + [CIPHERTEXT]
        ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(cipherText);

        String encoded = Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
        return ENCRYPTED_PREFIX + encoded;
    }

    public static String decrypt(String encryptedText, String password) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (encryptedText == null || !encryptedText.trim().startsWith(ENCRYPTED_PREFIX)) {
            throw new IllegalArgumentException("Invalid encrypted data format");
        }

        String base64Payload = encryptedText.trim().substring(ENCRYPTED_PREFIX.length());
        byte[] combined = Base64.decode(base64Payload, Base64.NO_WRAP);

        if (combined.length < SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Corrupted encrypted payload");
        }

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        buffer.get(salt);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        SecretKey secretKey = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
