package com.dream.service.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AiProviderRegistry {

    private final List<AiProvider> providers;

    public AiProviderRegistry(List<AiProvider> providers) {
        this.providers = providers;
    }

    public Optional<AiProvider> find(String providerName) {
        return providers.stream()
                .filter(provider -> provider.supports(providerName))
                .findFirst();
    }
}
