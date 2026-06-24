package com.dream.config;

import com.dream.common.auth.JwtProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class SecuritySecretValidator implements InitializingBean {

    private final JwtProperties jwtProperties;
    private final AiProperties aiProperties;

    public SecuritySecretValidator(JwtProperties jwtProperties, AiProperties aiProperties) {
        this.jwtProperties = jwtProperties;
        this.aiProperties = aiProperties;
    }

    @Override
    public void afterPropertiesSet() {
        SecretKeyPolicy.requireDifferent(
                "dream.jwt.secret",
                jwtProperties.getSecret(),
                "dream.ai.encryption-key",
                aiProperties.getEncryptionKey());
    }
}
