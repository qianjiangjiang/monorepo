package com.dream.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream.common.auth.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;

class SecretPropertiesValidationTest {

    private static final String VALID_JWT_SECRET = "jwt-secret-0123456789abcdef012345";
    private static final String VALID_AI_ENCRYPTION_KEY = "ai-secret-0123456789abcdef0123456";

    @Test
    void rejectsPlaceholderJwtSecretAtBeanInitialization() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("local-dev-secret-change-me-at-least-32-bytes");

        assertThatThrownBy(jwtProperties::afterPropertiesSet)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("dream.jwt.secret")
                .hasMessageContaining("placeholder");
    }

    @Test
    void rejectsPlaceholderAiEncryptionKeyAtBeanInitialization() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey("replace-with-a-32-byte-ai-encryption-key");

        assertThatThrownBy(aiProperties::afterPropertiesSet)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("dream.ai.encryption-key")
                .hasMessageContaining("placeholder");
    }

    @Test
    void rejectsMatchingJwtAndAiSecrets() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(VALID_JWT_SECRET);
        jwtProperties.afterPropertiesSet();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey(VALID_JWT_SECRET);
        aiProperties.afterPropertiesSet();

        SecuritySecretValidator validator = new SecuritySecretValidator(jwtProperties, aiProperties);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    void acceptsDifferentStrongSecrets() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(VALID_JWT_SECRET);
        jwtProperties.afterPropertiesSet();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey(VALID_AI_ENCRYPTION_KEY);
        aiProperties.afterPropertiesSet();

        SecuritySecretValidator validator = new SecuritySecretValidator(jwtProperties, aiProperties);
        validator.afterPropertiesSet();
    }
}
