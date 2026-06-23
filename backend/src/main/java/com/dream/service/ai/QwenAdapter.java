package com.dream.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QwenAdapter extends OpenAiCompatibleAdapter {

    public QwenAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected List<String> supportedProviderNames() {
        return List.of("qwen", "tongyi", "dashscope");
    }
}
