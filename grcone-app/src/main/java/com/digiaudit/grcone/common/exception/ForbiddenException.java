package com.digiaudit.grcone.common.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String errorCode, String messageCode, String developerMessage, Object... messageArgs) {
        super(errorCode, messageCode, developerMessage, messageArgs);
    }
}
