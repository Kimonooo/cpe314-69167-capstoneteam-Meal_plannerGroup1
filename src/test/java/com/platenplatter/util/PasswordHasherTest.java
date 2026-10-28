package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void samePasswordAndSaltAlwaysGiveTheSameHash() {
        String salt = PasswordHasher.generateSalt();
        assertEquals(PasswordHasher.hash("secret123", salt), PasswordHasher.hash("secret123", salt));
    }

    @Test
    void differentSaltsGiveDifferentHashesForTheSamePassword() {
        // edge case: two users with the same password must not look the same in the database
        String saltOne = PasswordHasher.generateSalt();
        String saltTwo = PasswordHasher.generateSalt();

        assertNotEquals(saltOne, saltTwo);
        assertNotEquals(PasswordHasher.hash("secret123", saltOne), PasswordHasher.hash("secret123", saltTwo));
    }

    @Test
    void verifyAcceptsTheRightPasswordAndRejectsAWrongOne() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("secret123", salt);

        assertTrue(PasswordHasher.verify("secret123", salt, hash));
        assertFalse(PasswordHasher.verify("Secret123", salt, hash)); // edge case: one capital letter
        assertNotEquals("secret123", hash); // the real password is never what gets saved
    }
}
