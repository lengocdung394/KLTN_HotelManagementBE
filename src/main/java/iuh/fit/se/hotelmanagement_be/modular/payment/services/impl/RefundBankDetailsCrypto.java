package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class RefundBankDetailsCrypto {
    private static final String PREFIX = "enc:v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public RefundBankDetailsCrypto(@Value("${app.refund.encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("REFUND_ENCRYPTION_KEY must be configured.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("REFUND_ENCRYPTION_KEY must be a Base64-encoded 256-bit key.", exception);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("REFUND_ENCRYPTION_KEY must decode to exactly 32 bytes.");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank() || isEncrypted(value)) {
            return value;
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            // ma hoa so tai khoan va tao them authentication tag
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt refund bank details.", exception);
        }
    }
    // doFinal tu kiem tra tag roi giai ma
    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!isEncrypted(value)) {
            throw new IllegalStateException("Refund bank details are not encrypted.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalStateException("Encrypted refund bank details are malformed.");
            }
            byte[] nonce = java.util.Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            byte[] ciphertext = java.util.Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt refund bank details.", exception);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
