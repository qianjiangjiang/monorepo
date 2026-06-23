package com.dream.service.ai;

import java.util.List;

public record DreamValidationResult(
        boolean valid,
        List<String> errors
) {

    public static DreamValidationResult ok() {
        return new DreamValidationResult(true, List.of());
    }

    public static DreamValidationResult failed(List<String> errors) {
        return new DreamValidationResult(false, List.copyOf(errors));
    }
}
