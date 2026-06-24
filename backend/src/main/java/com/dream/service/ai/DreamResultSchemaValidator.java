package com.dream.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DreamResultSchemaValidator {

    private static final Set<String> ROOT_FIELDS = Set.of(
            "title", "summary", "overallTone", "symbols", "emotion", "interpretations", "fortune", "suggestions", "tags");
    private static final Set<String> SYMBOL_FIELDS = Set.of("keyword", "meaning", "category");
    private static final Set<String> EMOTION_FIELDS = Set.of("primary", "description");
    private static final Set<String> INTERPRETATION_FIELDS = Set.of("school", "content");
    private static final Set<String> FORTUNE_FIELDS = Set.of("tendency", "disclaimer");
    private static final Set<String> TONES = Set.of("positive", "neutral", "negative", "mixed");
    private static final Set<String> SCHOOLS = Set.of("传统文化", "心理学", "现代象征");

    public DreamValidationResult validate(JsonNode root) {
        List<String> errors = new ArrayList<>();
        if (root == null || !root.isObject()) {
            errors.add("root must be an object");
            return DreamValidationResult.failed(errors);
        }

        rejectAdditionalFields(root, ROOT_FIELDS, "root", errors);
        requireText(root, "title", "root", errors);
        requireText(root, "summary", "root", errors);
        if (textLength(root, "title") > 40) {
            errors.add("root.title must be at most 40 characters");
        }
        if (textLength(root, "summary") > 60) {
            errors.add("root.summary must be at most 60 characters");
        }
        requireEnum(root, "overallTone", TONES, "root", errors);
        validateSymbols(root.path("symbols"), errors);
        validateEmotion(root.path("emotion"), errors);
        validateInterpretations(root.path("interpretations"), errors);
        validateFortune(root.path("fortune"), errors);
        validateStringArray(root.path("suggestions"), "root.suggestions", true, errors);
        if (root.has("tags")) {
            validateStringArray(root.path("tags"), "root.tags", false, errors);
        }
        return errors.isEmpty() ? DreamValidationResult.ok() : DreamValidationResult.failed(errors);
    }

    private void validateSymbols(JsonNode symbols, List<String> errors) {
        if (!symbols.isArray()) {
            errors.add("root.symbols must be an array");
            return;
        }
        for (int index = 0; index < symbols.size(); index++) {
            JsonNode symbol = symbols.get(index);
            String path = "root.symbols[" + index + "]";
            if (!symbol.isObject()) {
                errors.add(path + " must be an object");
                continue;
            }
            rejectAdditionalFields(symbol, SYMBOL_FIELDS, path, errors);
            requireText(symbol, "keyword", path, errors);
            requireText(symbol, "meaning", path, errors);
            if (symbol.has("category") && !symbol.path("category").isTextual()) {
                errors.add(path + ".category must be a string");
            }
        }
    }

    private void validateEmotion(JsonNode emotion, List<String> errors) {
        if (!emotion.isObject()) {
            errors.add("root.emotion must be an object");
            return;
        }
        rejectAdditionalFields(emotion, EMOTION_FIELDS, "root.emotion", errors);
        requireText(emotion, "primary", "root.emotion", errors);
        requireText(emotion, "description", "root.emotion", errors);
    }

    private void validateInterpretations(JsonNode interpretations, List<String> errors) {
        if (!interpretations.isArray() || interpretations.isEmpty()) {
            errors.add("root.interpretations must be a non-empty array");
            return;
        }
        boolean hasTraditional = false;
        boolean hasPsychology = false;
        for (int index = 0; index < interpretations.size(); index++) {
            JsonNode interpretation = interpretations.get(index);
            String path = "root.interpretations[" + index + "]";
            if (!interpretation.isObject()) {
                errors.add(path + " must be an object");
                continue;
            }
            rejectAdditionalFields(interpretation, INTERPRETATION_FIELDS, path, errors);
            requireEnum(interpretation, "school", SCHOOLS, path, errors);
            requireText(interpretation, "content", path, errors);
            hasTraditional = hasTraditional || "传统文化".equals(interpretation.path("school").asText());
            hasPsychology = hasPsychology || "心理学".equals(interpretation.path("school").asText());
        }
        if (!hasTraditional || !hasPsychology) {
            errors.add("root.interpretations must include 传统文化 and 心理学");
        }
    }

    private void validateFortune(JsonNode fortune, List<String> errors) {
        if (!fortune.isObject()) {
            errors.add("root.fortune must be an object");
            return;
        }
        rejectAdditionalFields(fortune, FORTUNE_FIELDS, "root.fortune", errors);
        requireText(fortune, "tendency", "root.fortune", errors);
    }

    private void validateStringArray(JsonNode array, String path, boolean required, List<String> errors) {
        if (required && !array.isArray()) {
            errors.add(path + " must be an array");
            return;
        }
        if (!array.isArray()) {
            errors.add(path + " must be an array");
            return;
        }
        for (int index = 0; index < array.size(); index++) {
            if (!array.get(index).isTextual()) {
                errors.add(path + "[" + index + "] must be a string");
            }
        }
    }

    private void rejectAdditionalFields(JsonNode object, Set<String> allowedFields, String path, List<String> errors) {
        Iterator<String> fieldNames = object.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!allowedFields.contains(fieldName)) {
                errors.add(path + "." + fieldName + " is not allowed");
            }
        }
    }

    private void requireText(JsonNode object, String fieldName, String path, List<String> errors) {
        JsonNode value = object.path(fieldName);
        if (!value.isTextual() || value.asText().isBlank()) {
            errors.add(path + "." + fieldName + " must be a non-empty string");
        }
    }

    private void requireEnum(JsonNode object, String fieldName, Set<String> allowedValues, String path, List<String> errors) {
        JsonNode value = object.path(fieldName);
        if (!value.isTextual() || !allowedValues.contains(value.asText())) {
            errors.add(path + "." + fieldName + " has an unsupported value");
        }
    }

    private int textLength(JsonNode object, String fieldName) {
        return object.path(fieldName).isTextual() ? object.path(fieldName).asText().length() : 0;
    }
}
