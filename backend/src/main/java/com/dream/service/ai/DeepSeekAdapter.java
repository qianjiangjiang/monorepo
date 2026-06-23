package com.dream.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekAdapter extends OpenAiCompatibleAdapter {

    public DeepSeekAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected List<String> supportedProviderNames() {
        return List.of("deepseek");
    }
}
