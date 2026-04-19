package org.ershoupingtai.common;

public enum ResultCode {
    // 通用状态码
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或认证失败"),
    TOKEN_MISSING(40101, "缺少访问令牌"),
    TOKEN_EXPIRED(40102, "访问令牌已过期"),
    TOKEN_INVALID(40103, "访问令牌无效"),
    TOKEN_REVOKED(40104, "访问令牌已撤销"),
    REFRESH_TOKEN_EXPIRED(40111, "刷新令牌已过期"),
    REFRESH_TOKEN_INVALID(40112, "刷新令牌无效"),
    REFRESH_TOKEN_REVOKED(40113, "刷新令牌已失效"),
    REFRESH_TOKEN_REPLACED(40114, "刷新令牌已替换"),
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
