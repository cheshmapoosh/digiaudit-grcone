package com.digiaudit.grcone.common.exception;

public class GoneException extends BusinessException {
    public GoneException(String errorCode, String messageCode, String developerMessage, Object... messageArgs) {
        super(errorCode, messageCode, developerMessage, messageArgs);
    }
}
