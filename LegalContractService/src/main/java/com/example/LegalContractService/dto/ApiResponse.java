package com.example.LegalContractService.dto;

import com.example.LegalContractService.model.Legalcontract;
import java.util.List;

/**
 * Generic API Response wrapper class
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    // Default constructor for Jackson
    public ApiResponse() {
    }

    // Private constructors
    private ApiResponse(boolean success, String message, T data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    // Static factory methods for success
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    // Static factory methods for error
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, message);
    }

    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error);
    }

    // Getters and Setters for Jackson
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Convenience methods for common types
    public static ApiResponse<Legalcontract> contractSuccess(Legalcontract contract) {
        return success(contract);
    }

    public static ApiResponse<List<Legalcontract>> contractsSuccess(List<Legalcontract> contracts) {
        return success(contracts);
    }
}

