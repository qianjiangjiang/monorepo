package com.dream.service.ai;

import com.dream.domain.AiProviderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.util.StringUtils;

public abstract class OpenAiCompatibleAdapter implements AiProvider {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    protected OpenAiCompatibleAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiCompletionResponse complete(AiProviderConfig config, String apiKey, AiCompletionRequest request) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiProviderException("api key is empty");
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMillis(timeoutMs(config)))
                .connectTimeout(Duration.ofMillis(timeoutMs(config)))
                .readTimeout(Duration.ofMillis(timeoutMs(config)))
                .build();
        try {
            String responseJson = execute(client, config, apiKey, request);
            return parseResponse(responseJson);
        } catch (IOException exception) {
            throw new AiProviderException("provider request failed", exception);
        }
    }

    protected abstract List<String> supportedProviderNames();

    @Override
    public boolean supports(String provider) {
        if (!StringUtils.hasText(provider)) {
            return false;
        }
        String normalized = provider.toLowerCase(Locale.ROOT);
        return supportedProviderNames().stream().anyMatch(normalized::equals);
    }

    private String execute(OkHttpClient client, AiProviderConfig config, String apiKey, AiCompletionRequest completionRequest)
            throws IOException {
        String payload = objectMapper.writeValueAsString(toPayload(config, completionRequest));
        Request request = new Request.Builder()
                .url(chatCompletionsUrl(config.getBaseUrl()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String bodyText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new AiProviderException("provider returned http " + response.code());
            }
            return bodyText;
        }
    }

    private Map<String, Object> toPayload(AiProviderConfig config, AiCompletionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getModel());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())));
        payload.put("temperature", config.getTemperature());
        payload.put("top_p", config.getTopP());
        payload.put("max_tokens", config.getMaxTokens());
        if ("json_object".equalsIgnoreCase(config.getResponseFormat())) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        return payload;
    }

    private AiCompletionResponse parseResponse(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new AiProviderException("provider response content is empty");
        }
        int tokenIn = root.path("usage").path("prompt_tokens").asInt(0);
        int tokenOut = root.path("usage").path("completion_tokens").asInt(0);
        return new AiCompletionResponse(content, tokenIn, tokenOut);
    }

    private String chatCompletionsUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + "/chat/completions";
    }

    private int timeoutMs(AiProviderConfig config) {
        return config.getTimeoutMs() == null ? 30000 : config.getTimeoutMs();
    }
}
