package com.ershoupingtai.common;

public enum ResultCode {
    // 通用状态码
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    DATA_NOT_FOUND(404, "数据不存在"),
    SYSTEM_ERROR(500, "系统异常");

    private int code;
    private String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
