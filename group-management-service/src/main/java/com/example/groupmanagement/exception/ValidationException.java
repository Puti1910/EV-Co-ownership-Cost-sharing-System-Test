package com.example.groupmanagement.exception;

public class ValidationException extends RuntimeException {
    private final String fieldName;
    private final String errorCode;

    public ValidationException(String message, String fieldName, String errorCode) {
        super(message);
        this.fieldName = fieldName;
        this.errorCode = errorCode;
    }

    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.errorCode = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}