package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RefundBankDetailsCryptoTest {
    private final RefundBankDetailsCrypto crypto =
            new RefundBankDetailsCrypto(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void encryptsAndDecryptsAccountData() {
        String accountNumber = "123456789012";

        String encrypted = crypto.encrypt(accountNumber);

        assertNotEquals(accountNumber, encrypted);
        assertTrue(crypto.isEncrypted(encrypted));
        assertEquals(accountNumber, crypto.decrypt(encrypted));
    }

    @Test
    void encryptsTheSameValueWithDifferentNonces() {
        String first = crypto.encrypt("123456789012");
        String second = crypto.encrypt("123456789012");

        assertNotEquals(first, second);
        assertEquals("123456789012", crypto.decrypt(first));
        assertEquals("123456789012", crypto.decrypt(second));
    }

    @Test
    void rejectsMissingOrInvalidKeys() {
        assertThrows(IllegalStateException.class, () -> new RefundBankDetailsCrypto(""));
        assertThrows(IllegalStateException.class, () -> new RefundBankDetailsCrypto(
                Base64.getEncoder().encodeToString(new byte[16])));
    }
}
