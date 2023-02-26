package com.lol.match.common.exception;

public enum ExceptionCode {
    BAD_REQUEST(400, "잘못된 요청입니다."),
    DUPLICATION_REQUEST(400, "중복된 요청입니다"),
    SERVER_ERROR(500, "서버에러 입니다. 빠른시일 내에 수정하겠습니다.");

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
