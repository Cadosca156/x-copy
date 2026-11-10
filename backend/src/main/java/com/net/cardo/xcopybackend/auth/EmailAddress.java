package com.net.cardo.xcopybackend.auth;

import java.util.Locale;
import java.util.regex.Pattern;

/** Account identity is case-insensitive; the delivery address keeps its spelling. */
public final class EmailAddress {
    private static final Pattern EDGE_WHITESPACE = Pattern.compile("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$");
    private EmailAddress() {}

    public static String clean(String email) {
        return email == null ? null : EDGE_WHITESPACE.matcher(email).replaceAll("");
    }

    public static String key(String email) {
        String cleaned = clean(email);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    public static boolean same(String first, String second) {
        String firstKey = key(first);
        return firstKey != null && firstKey.equals(key(second));
    }
}
