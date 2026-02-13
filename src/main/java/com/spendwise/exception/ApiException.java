package com.spendwise.exception;

/**
 * Base exception for API errors. Holds an error code and optional detail message.
 * Client responses use errorCode.getClientMessage(); never expose raw exception messages.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getClientMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    public ApiException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage != null ? detailMessage : errorCode.getClientMessage());
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ApiException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage != null ? detailMessage : errorCode.getClientMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Detail message for logging; not returned to clients.
     */
    public String getDetailMessage() {
        return detailMessage;
    }
}
