package com.dream.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SafeDreamResultFactory {

    private static final String DISCLAIMER = "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。";

    private final ObjectMapper objectMapper;

    public SafeDreamResultFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode create(String dreamText, List<String> tags) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", "关于梦境的温和解读");
        root.put("summary", "梦境可能反映近期感受，建议结合现实处境理解。");
        root.put("overallTone", "neutral");

        ArrayNode symbols = root.putArray("symbols");
        ObjectNode symbol = symbols.addObject();
        symbol.put("keyword", keywordFrom(dreamText));
        symbol.put("meaning", "代表你近期较关注的情绪、关系或压力线索。");
        symbol.put("category", "综合");

        ObjectNode emotion = root.putObject("emotion");
        emotion.put("primary", "复杂");
        emotion.put("description", "梦境信息暂无法完整解析，可先记录醒来后的主要感受。");

        ArrayNode interpretations = root.putArray("interpretations");
        ObjectNode traditional = interpretations.addObject();
        traditional.put("school", "传统文化");
        traditional.put("content", "传统文化视角会把梦境看作心境与处境的象征提示，宜谨慎观察，不作绝对吉凶判断。");
        ObjectNode psychology = interpretations.addObject();
        psychology.put("school", "心理学");
        psychology.put("content", "心理学视角更关注梦中情绪与现实压力的连接，可把它作为自我觉察的线索。");

        ObjectNode fortune = root.putObject("fortune");
        fortune.put("tendency", "宜静观");
        fortune.put("disclaimer", DISCLAIMER);

        ArrayNode suggestions = root.putArray("suggestions");
        suggestions.add("记录梦中最强烈的画面和情绪。");
        suggestions.add("回看最近是否存在相似的压力或期待。");
        suggestions.add("若持续困扰睡眠或情绪，建议寻求专业支持。");

        if (tags != null && !tags.isEmpty()) {
            ArrayNode tagArray = root.putArray("tags");
            tags.stream().filter(tag -> tag != null && !tag.isBlank()).limit(8).forEach(tagArray::add);
        }
        return root;
    }

    private String keywordFrom(String dreamText) {
        if (dreamText == null || dreamText.isBlank()) {
            return "梦境";
        }
        String normalized = dreamText.replaceAll("\\s+", "");
        return normalized.length() <= 8 ? normalized : normalized.substring(0, 8);
    }
}
