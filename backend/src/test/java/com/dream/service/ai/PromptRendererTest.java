package com.dream.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream.domain.PromptTemplate;
import org.junit.jupiter.api.Test;

class PromptRendererTest {

    private final PromptRenderer renderer = new PromptRenderer();

    @Test
    void rendersRequestedSchoolAsDisplayPreference() {
        PromptRenderer.RenderedPrompt rendered = renderer.render(template(), "梦见飞翔", "心理学");

        assertThat(rendered.userPrompt()).contains("梦见飞翔");
        assertThat(rendered.userPrompt()).contains("全部，必须包含传统文化与心理学；用户展示偏好：心理学");
    }

    @Test
    void rendersEmptySchoolAsAllPerspectives() {
        PromptRenderer.RenderedPrompt rendered = renderer.render(template(), "梦见飞翔", "");

        assertThat(rendered.userPrompt()).contains("全部，必须包含传统文化与心理学");
        assertThat(rendered.userPrompt()).doesNotContain("用户展示偏好");
    }

    @Test
    void doesNotReplacePlaceholderTextInsideDreamText() {
        PromptRenderer.RenderedPrompt rendered = renderer.render(template(), "梦里看见 {{school}} 字样", "心理学");

        assertThat(rendered.userPrompt()).contains("梦境：梦里看见 {{school}} 字样");
        assertThat(rendered.userPrompt()).contains("流派：全部，必须包含传统文化与心理学；用户展示偏好：心理学");
    }

    private PromptTemplate template() {
        PromptTemplate template = new PromptTemplate();
        template.setSystemPrompt("system");
        template.setUserPromptTemplate("梦境：{{dreamText}}\n流派：{{school}}");
        template.setVersion("test");
        template.setSchemaJson("{}");
        return template;
    }
}
