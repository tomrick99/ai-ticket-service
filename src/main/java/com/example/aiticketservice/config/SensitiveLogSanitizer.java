package com.example.aiticketservice.config;

import java.util.regex.Pattern;

public final class SensitiveLogSanitizer {

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._\\-]+");
    private static final Pattern API_KEY_PATTERN =
            Pattern.compile("(?i)(api[-_ ]?key|token|authorization)\\s*[:=]\\s*[^,\\s]+");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(?i)(password|passwd|pwd)\\s*[:=]\\s*[^,\\s]+");
    private static final Pattern JDBC_PASSWORD_PATTERN =
            Pattern.compile("(?i)(password=)[^&\\s]+");

    private SensitiveLogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = BEARER_PATTERN.matcher(value).replaceAll("Bearer ****");
        sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("$1=****");
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=****");
        sanitized = JDBC_PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1****");
        return sanitized;
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "<empty>";
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
