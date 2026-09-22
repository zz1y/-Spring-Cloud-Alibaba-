package com.example.mall.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;      // 状态码：200 成功，500 失败
    private String message;    // 提示信息
    private T data;            // 数据（类型由调用方决定）

    // 成功时的快捷构造方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    // 失败时的快捷构造方法
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}

