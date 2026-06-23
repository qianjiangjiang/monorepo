package com.dream.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(400, "bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "unauthorized", HttpStatus.UNAUTHORIZED),
    RATE_LIMITED(429, "too many requests", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(500, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
