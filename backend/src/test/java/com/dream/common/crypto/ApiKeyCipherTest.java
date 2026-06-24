package com.dream.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;

class ApiKeyCipherTest {

    private static final String VALID_AI_ENCRYPTION_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void encryptsDecryptsAndMasksApiKeys() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey(VALID_AI_ENCRYPTION_KEY);
        ApiKeyCipher cipher = new ApiKeyCipher(aiProperties);

        String encrypted = cipher.encrypt("sk-1234567890abcdef");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("sk-1234567890abcdef");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("sk-1234567890abcdef");
        assertThat(cipher.mask("sk-1234567890abcdef")).isEqualTo("sk-1****cdef");
    }

    @Test
    void rejectsBlankOrPlaceholderEncryptionKey() {
        assertThatThrownBy(() -> new ApiKeyCipher(new AiProperties()))
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("dream.ai.encryption-key");

        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey("replace-with-a-32-byte-ai-encryption-key");

        assertThatThrownBy(() -> new ApiKeyCipher(aiProperties))
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("placeholder");
    }
}
