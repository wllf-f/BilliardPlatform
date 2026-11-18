package com.billiard.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一返回结果封装
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("success");
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = success();
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
