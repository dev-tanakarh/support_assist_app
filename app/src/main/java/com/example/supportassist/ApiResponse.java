package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName(value = "success", alternate = {"status"})
    private Object success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName(value = "data", alternate = {"result", "payload"})
    private T data;
    
    @SerializedName("errors")
    private Object errors;

    public boolean isSuccess() { 
        if (success instanceof Boolean) {
            return (Boolean) success;
        } else if (success instanceof String) {
            return "success".equalsIgnoreCase((String) success) || "true".equalsIgnoreCase((String) success);
        }
        return false;
    }

    public String getMessage() { return message; }
    public T getData() { return data; }
    public Object getErrors() { return errors; }
}