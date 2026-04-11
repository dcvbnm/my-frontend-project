package org.ershoupingtai.common.security;

public final class UserContext {
    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_NAME = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String userId, String userName) {
        CURRENT_USER_ID.set(userId);
        CURRENT_USER_NAME.set(userName);
    }

    public static String getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static String getUserName() {
        return CURRENT_USER_NAME.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_NAME.remove();
    }
}
