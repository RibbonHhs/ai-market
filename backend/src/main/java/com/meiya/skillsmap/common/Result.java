package com.meiya.skillsmap.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public Result() {}

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() { return new Result<>(0, "ok", null); }
    public static <T> Result<T> ok(T data) { return new Result<>(0, "ok", data); }
    public static <T> Result<T> ok(T data, String message) { return new Result<>(0, message, data); }
    public static <T> Result<T> fail(int code, String message) { return new Result<>(code, message, null); }
    public static <T> Result<T> fail(BizCode bizCode) { return new Result<>(bizCode.getCode(), bizCode.getMessage(), null); }
    public static <T> Result<T> fail(BizCode bizCode, String message) { return new Result<>(bizCode.getCode(), message, null); }
    public boolean isSuccess() { return code == 0; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
