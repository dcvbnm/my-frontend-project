package org.ershoupingtai.pojo.user;

// 对应 UserLogin 表，负责登录标识与密码哈希数据承载。
public class UserLoginEntity {
    private Long userId;
    private String userName;
    private String userPassword;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
}
