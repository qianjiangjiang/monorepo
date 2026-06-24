package com.dream.common.auth;

import com.dream.config.SecretKeyPolicy;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dream.jwt")
public class JwtProperties implements InitializingBean {

    public static final Set<String> PLACEHOLDER_SECRETS = Set.of(
            "local-dev-secret-change-me-at-least-32-bytes",
            "replace-with-a-local-256-bit-secret",
            "replace-with-a-32-byte-jwt-secret",
            "replace-with-a-local-jwt-secret");

    private String issuer = "dream-miniapp";
    private String secret = "";
    private Duration expiration = Duration.ofDays(30);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    @Override
    public void afterPropertiesSet() {
        secret = SecretKeyPolicy.requireStrongSecret("dream.jwt.secret", secret, PLACEHOLDER_SECRETS);
    }
}
