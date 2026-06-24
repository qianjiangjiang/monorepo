package com.dream.service.ai;

import com.dream.common.crypto.ApiKeyCipher;
import com.dream.config.AiProperties;
import com.dream.domain.AiProviderConfig;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);

    private final AiConfigurationCache aiConfigurationCache;
    private final AiProviderRegistry aiProviderRegistry;
    private final ApiKeyCipher apiKeyCipher;
    private final AiProperties aiProperties;
    private final SecureRandom random = new SecureRandom();

    public AiProviderRouter(
            AiConfigurationCache aiConfigurationCache,
            AiProviderRegistry aiProviderRegistry,
            ApiKeyCipher apiKeyCipher,
            AiProperties aiProperties) {
        this.aiConfigurationCache = aiConfigurationCache;
        this.aiProviderRegistry = aiProviderRegistry;
        this.apiKeyCipher = apiKeyCipher;
        this.aiProperties = aiProperties;
    }

    public AiRouteResult complete(AiCompletionRequest request) {
        List<AiProviderConfig> candidates = routeOrder(aiConfigurationCache.getEnabledProviderConfigs());
        List<String> failures = new ArrayList<>();
        int attempts = 0;
        for (AiProviderConfig config : candidates) {
            AiProvider provider = aiProviderRegistry.find(config.getProvider()).orElse(null);
            if (provider == null) {
                failures.add(config.getProvider() + ":unsupported");
                continue;
            }
            attempts++;
            try {
                AiCompletionResponse response = provider.complete(config, apiKeyCipher.decrypt(config.getApiKey()), request);
                return new AiRouteResult(config, response, attempts > 1, attempts);
            } catch (RuntimeException exception) {
                failures.add(config.getProvider() + ":" + exception.getMessage());
                log.warn("AI provider failed provider={} model={} attempt={}", config.getProvider(), config.getModel(), attempts);
            }
        }
        throw new AiProviderException("no available ai provider: " + String.join("; ", failures));
    }

    private List<AiProviderConfig> routeOrder(List<AiProviderConfig> configs) {
        Map<Integer, List<AiProviderConfig>> byPriority = configs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .sorted(Comparator.comparing(AiProviderConfig::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(
                        config -> config.getPriority() == null ? 100 : config.getPriority(),
                        TreeMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<AiProviderConfig> ordered = new ArrayList<>();
        for (List<AiProviderConfig> group : byPriority.values()) {
            ordered.addAll(weightedOrder(group));
        }
        return ordered;
    }

    private List<AiProviderConfig> weightedOrder(List<AiProviderConfig> source) {
        List<AiProviderConfig> remaining = new ArrayList<>(source);
        List<AiProviderConfig> ordered = new ArrayList<>();
        while (!remaining.isEmpty()) {
            int totalWeight = remaining.stream().mapToInt(this::effectiveWeight).sum();
            if (totalWeight <= 0) {
                ordered.addAll(remaining);
                break;
            }
            int pick = random.nextInt(totalWeight);
            int cursor = 0;
            for (int index = 0; index < remaining.size(); index++) {
                AiProviderConfig config = remaining.get(index);
                cursor += effectiveWeight(config);
                if (pick < cursor) {
                    ordered.add(remaining.remove(index));
                    break;
                }
            }
        }
        return ordered;
    }

    private int effectiveWeight(AiProviderConfig config) {
        int weight = Math.max(0, config.getWeight() == null ? 100 : config.getWeight());
        if (weight == 0 || aiProperties.getDefaultProvider() == null || config.getProvider() == null) {
            return weight;
        }
        return aiProperties.getDefaultProvider().equalsIgnoreCase(config.getProvider()) ? weight * 2 : weight;
    }
}
