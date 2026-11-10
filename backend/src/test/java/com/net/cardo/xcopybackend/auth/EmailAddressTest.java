package com.net.cardo.xcopybackend.auth;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailAddressTest {
    @Test void caseInsensitiveKeyAndDeliverySpellingAreSeparate() {
        String input=" \u00a0Owner.Name+Tag@Example.COM\uFEFF ";
        assertEquals("Owner.Name+Tag@Example.COM",EmailAddress.clean(input));
        assertEquals("owner.name+tag@example.com",EmailAddress.key(input));
        assertTrue(EmailAddress.same(input,"owner.name+tag@EXAMPLE.com"));
        assertNull(EmailAddress.key(null));assertFalse(EmailAddress.same(null,null));
    }
    @Test void localeDotsPlusTagsAndDifferentAddressesCannotSilentlyChangeIdentity() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("identity@example.com",EmailAddress.key("IDENTITY@EXAMPLE.COM"));
        } finally {Locale.setDefault(previous);}
        assertFalse(EmailAddress.same("a.b@example.com","ab@example.com"));
        assertFalse(EmailAddress.same("ab+tag@example.com","ab@example.com"));
        assertFalse(EmailAddress.same("a@example.com","a@example.org"));
    }
}
