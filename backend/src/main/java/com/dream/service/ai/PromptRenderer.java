package com.dream.service.ai;

import com.dream.domain.PromptTemplate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(dreamText|school)}}");

    public RenderedPrompt render(PromptTemplate template, String dreamText, String school) {
        String requestedSchool = StringUtils.hasText(school)
                ? "全部，必须包含传统文化与心理学；用户展示偏好：" + school
                : "全部，必须包含传统文化与心理学";
        Map<String, String> values = Map.of(
                "dreamText", dreamText,
                "school", requestedSchool);
        return new RenderedPrompt(
                template.getSystemPrompt(),
                renderTemplate(template.getUserPromptTemplate(), values),
                template.getVersion(),
                template.getSchemaJson());
    }

    private String renderTemplate(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(values.get(matcher.group(1))));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    public record RenderedPrompt(
            String systemPrompt,
            String userPrompt,
            String version,
            String schemaJson
    ) {
    }
}
