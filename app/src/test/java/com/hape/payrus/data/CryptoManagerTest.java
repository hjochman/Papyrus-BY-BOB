package com.hape.payrus.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Robolectric is required because CryptoManager uses android.util.Base64.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CryptoManagerTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Test
    public void roundtrip_returnsOriginalText() throws Exception {
        String plain = "Hello Wörld \u20ac\nZeile 2";
        String encrypted = CryptoManager.encrypt(plain, PASSWORD);

        assertTrue(CryptoManager.isEncrypted(encrypted));
        assertFalse(encrypted.contains("Hello"));
        assertEquals(plain, CryptoManager.decrypt(encrypted, PASSWORD));
    }

    @Test
    public void roundtrip_emptyAndNullPlainText() throws Exception {
        assertEquals("", CryptoManager.decrypt(CryptoManager.encrypt("", PASSWORD), PASSWORD));
        assertEquals("", CryptoManager.decrypt(CryptoManager.encrypt(null, PASSWORD), PASSWORD));
    }

    @Test
    public void decrypt_withWrongPassword_fails() throws Exception {
        String encrypted = CryptoManager.encrypt("secret", PASSWORD);

        assertThrows(GeneralSecurityException.class,
                () -> CryptoManager.decrypt(encrypted, "wrong password"));
    }

    @Test
    public void encrypt_samePlainTextTwice_usesDifferentSaltAndIv() throws Exception {
        String first = CryptoManager.encrypt("same text", PASSWORD);
        String second = CryptoManager.encrypt("same text", PASSWORD);

        assertNotEquals(first, second);
        assertEquals("same text", CryptoManager.decrypt(first, PASSWORD));
        assertEquals("same text", CryptoManager.decrypt(second, PASSWORD));
    }

    @Test
    public void decrypt_tamperedPayload_fails() throws Exception {
        String encrypted = CryptoManager.encrypt("secret text", PASSWORD);
        // Change a Base64 character inside the ciphertext (avoid trailing '=' padding).
        int pos = encrypted.length() - 10;
        char original = encrypted.charAt(pos);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, pos) + replacement + encrypted.substring(pos + 1);

        assertThrows(GeneralSecurityException.class,
                () -> CryptoManager.decrypt(tampered, PASSWORD));
    }

    @Test
    public void encryptAndDecrypt_emptyPassword_throws() {
        assertThrows(IllegalArgumentException.class, () -> CryptoManager.encrypt("x", ""));
        assertThrows(IllegalArgumentException.class, () -> CryptoManager.encrypt("x", null));
        assertThrows(IllegalArgumentException.class, () -> CryptoManager.decrypt("ENC_PAPYRUS_V1:abc", ""));
    }

    @Test
    public void decrypt_invalidFormat_throws() {
        assertThrows(IllegalArgumentException.class, () -> CryptoManager.decrypt("plain text", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> CryptoManager.decrypt(null, PASSWORD));
    }

    @Test
    public void decrypt_tooShortPayload_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> CryptoManager.decrypt("ENC_PAPYRUS_V1:AAAA", PASSWORD));
    }

    @Test
    public void isEncrypted_detectsPrefix() {
        assertTrue(CryptoManager.isEncrypted("  ENC_PAPYRUS_V1:abc"));
        assertFalse(CryptoManager.isEncrypted("just a note"));
        assertFalse(CryptoManager.isEncrypted(null));
    }
}
