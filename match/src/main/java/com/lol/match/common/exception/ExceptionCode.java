package com.lol.match.common.exception;

public enum ExceptionCode {
    BAD_REQUEST(400, "잘못된 요청입니다."),
    DUPLICATION_REQUEST(400, "중복된 요청입니다");

    private int status;
    private String message;

    ExceptionCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }
}
