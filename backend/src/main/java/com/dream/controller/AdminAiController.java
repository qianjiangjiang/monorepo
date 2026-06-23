package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.controller.dto.AiProviderConfigRequest;
import com.dream.controller.dto.AiProviderConfigResponse;
import com.dream.controller.dto.AiProviderTestResponse;
import com.dream.controller.dto.PromptTemplateRequest;
import com.dream.controller.dto.PromptTemplateResponse;
import com.dream.service.AiAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAiController {

    private final AiAdminService aiAdminService;

    public AdminAiController(AiAdminService aiAdminService) {
        this.aiAdminService = aiAdminService;
    }

    @GetMapping("/ai/config")
    public ApiResponse<List<AiProviderConfigResponse>> listProviderConfigs() {
        return ApiResponse.ok(aiAdminService.listProviderConfigs());
    }

    @PostMapping("/ai/config")
    public ApiResponse<AiProviderConfigResponse> saveProviderConfig(
            @Valid @RequestBody AiProviderConfigRequest request) {
        return ApiResponse.ok(aiAdminService.saveProviderConfig(request));
    }

    @DeleteMapping("/ai/config/{id}")
    public ApiResponse<Void> deleteProviderConfig(@PathVariable Long id) {
        aiAdminService.deleteProviderConfig(id);
        return ApiResponse.ok();
    }

    @PostMapping("/ai/config/{id}/test")
    public ApiResponse<AiProviderTestResponse> testProviderConfig(@PathVariable Long id) {
        return ApiResponse.ok(aiAdminService.testProviderConfig(id));
    }

    @PostMapping("/ai/refresh")
    public ApiResponse<Map<String, Boolean>> refreshAiConfig() {
        aiAdminService.refreshCache();
        return ApiResponse.ok(Map.of("refreshed", true));
    }

    @GetMapping("/prompt")
    public ApiResponse<List<PromptTemplateResponse>> listPromptTemplates() {
        return ApiResponse.ok(aiAdminService.listPromptTemplates());
    }

    @PostMapping("/prompt")
    public ApiResponse<PromptTemplateResponse> savePromptTemplate(
            @Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.ok(aiAdminService.savePromptTemplate(request));
    }

    @DeleteMapping("/prompt/{id}")
    public ApiResponse<Void> deletePromptTemplate(@PathVariable Long id) {
        aiAdminService.deletePromptTemplate(id);
        return ApiResponse.ok();
    }
}
