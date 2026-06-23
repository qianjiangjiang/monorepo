package com.dream.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream.common.auth.JwtProperties;
import com.dream.config.AiProperties;
import org.junit.jupiter.api.Test;

class ApiKeyCipherTest {

    @Test
    void encryptsDecryptsAndMasksApiKeys() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEncryptionKey("test-ai-config-secret");
        JwtProperties jwtProperties = new JwtProperties();
        ApiKeyCipher cipher = new ApiKeyCipher(aiProperties, jwtProperties);

        String encrypted = cipher.encrypt("sk-1234567890abcdef");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("sk-1234567890abcdef");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("sk-1234567890abcdef");
        assertThat(cipher.mask("sk-1234567890abcdef")).isEqualTo("sk-1****cdef");
    }
}
