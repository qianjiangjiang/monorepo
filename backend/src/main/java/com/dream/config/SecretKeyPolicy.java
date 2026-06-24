package com.dream.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

public final class SecretKeyPolicy {

    public static final int MIN_SECRET_BYTES = 32;

    private SecretKeyPolicy() {
    }

    public static String requireStrongSecret(String propertyName, String value, Set<String> placeholders) {
        if (!StringUtils.hasText(value)) {
            throw new BeanCreationException(propertyName + " must be set and must be at least 32 bytes");
        }

        String normalized = value.trim();
        if (placeholders.contains(normalized)) {
            throw new BeanCreationException(propertyName + " must not use the example placeholder value");
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new BeanCreationException(propertyName + " must be at least 32 bytes");
        }
        return normalized;
    }

    public static void requireDifferent(
            String firstPropertyName,
            String firstValue,
            String secondPropertyName,
            String secondValue) {
        if (StringUtils.hasText(firstValue)
                && StringUtils.hasText(secondValue)
                && firstValue.trim().equals(secondValue.trim())) {
            throw new BeanCreationException(firstPropertyName + " and " + secondPropertyName + " must be different");
        }
    }
}
