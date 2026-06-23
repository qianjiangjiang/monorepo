package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.controller.dto.AiProviderConfigRequest;
import com.dream.controller.dto.AiProviderConfigResponse;
import com.dream.controller.dto.AiProviderTestResponse;
import com.dream.controller.dto.AdminDreamRecordResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.controller.dto.PromptTemplateRequest;
import com.dream.controller.dto.PromptTemplateResponse;
import com.dream.controller.dto.SensitiveWordRequest;
import com.dream.controller.dto.SensitiveWordResponse;
import com.dream.service.AdminContentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAiController {

    private final AiAdminService aiAdminService;
    private final AdminContentService adminContentService;

    public AdminAiController(AiAdminService aiAdminService, AdminContentService adminContentService) {
        this.aiAdminService = aiAdminService;
        this.adminContentService = adminContentService;
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

    @GetMapping("/sensitive")
    public ApiResponse<List<SensitiveWordResponse>> listSensitiveWords() {
        return ApiResponse.ok(adminContentService.listSensitiveWords());
    }

    @PostMapping("/sensitive")
    public ApiResponse<SensitiveWordResponse> saveSensitiveWord(
            @Valid @RequestBody SensitiveWordRequest request) {
        return ApiResponse.ok(adminContentService.saveSensitiveWord(request));
    }

    @DeleteMapping("/sensitive/{id}")
    public ApiResponse<Void> deleteSensitiveWord(@PathVariable Long id) {
        adminContentService.deleteSensitiveWord(id);
        return ApiResponse.ok();
    }

    @GetMapping("/dream/records")
    public ApiResponse<PageResponse<AdminDreamRecordResponse>> listDreamRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminContentService.listDreamRecords(page, size));
    }
}
