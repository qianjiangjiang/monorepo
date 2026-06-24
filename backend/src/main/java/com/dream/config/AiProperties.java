package com.dream.config;

import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dream.ai")
public class AiProperties implements InitializingBean {

    public static final Set<String> PLACEHOLDER_ENCRYPTION_KEYS = Set.of(
            "replace-with-a-local-ai-config-secret",
            "replace-with-a-32-byte-ai-encryption-key",
            "replace-with-a-local-ai-encryption-key");

    private String defaultProvider = "deepseek";
    private String encryptionKey = "";

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public void afterPropertiesSet() {
        encryptionKey = SecretKeyPolicy.requireStrongSecret(
                "dream.ai.encryption-key",
                encryptionKey,
                PLACEHOLDER_ENCRYPTION_KEYS);
    }
}
