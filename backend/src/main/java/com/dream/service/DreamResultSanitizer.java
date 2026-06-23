package com.dream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

@Service
public class DreamResultSanitizer {

    public static final String DISCLAIMER = "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。";

    private final ObjectMapper objectMapper;

    public DreamResultSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode withDisclaimer(JsonNode result) {
        ObjectNode root = result == null || !result.isObject()
                ? objectMapper.createObjectNode()
                : result.deepCopy();
        ObjectNode fortune;
        JsonNode fortuneNode = root.get("fortune");
        if (fortuneNode != null && fortuneNode.isObject()) {
            fortune = (ObjectNode) fortuneNode.deepCopy();
        } else {
            fortune = objectMapper.createObjectNode();
        }
        fortune.put("disclaimer", DISCLAIMER);
        root.set("fortune", fortune);
        return root;
    }
}
