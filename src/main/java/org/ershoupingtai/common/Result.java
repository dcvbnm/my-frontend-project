package org.ershoupingtai.common;

public class Result<T> {
    private int code;
    private String msg;
    private T data;
    private Result() {};

    public void setCode(int code) {
        this.code = code;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public void setData(T data) {
        this.data = data;
    }
    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }

    //成功，带数据
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    //成功，无数据
    public static <T> Result<T> success() {
        return success(null);
    }

    //失败
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<T>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        result.setData(null);
        return result;
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<T>();
        result.setCode(ResultCode.PARAM_ERROR.getCode());
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}
