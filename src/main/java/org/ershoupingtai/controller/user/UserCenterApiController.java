package org.ershoupingtai.controller.user;

import org.ershoupingtai.common.Result;
import org.ershoupingtai.common.ResultCode;
import org.ershoupingtai.common.security.AuthTokenService;
import org.ershoupingtai.common.security.TokenBundle;
import org.ershoupingtai.common.security.UserContext;
import org.ershoupingtai.pojo.user.User;
import org.ershoupingtai.service.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserCenterApiController {
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final AuthTokenService authTokenService;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    public UserCenterApiController(UserService userService,
                                   AuthTokenService authTokenService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String studentId,
                                             @RequestParam String password,
                                             @RequestParam(required = false) String deviceId) {
        try {
            User user = userService.login(studentId, password);
            return Result.success(buildAuthPayload(user, deviceId));
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam String studentId,
                                                @RequestParam String username,
                                                @RequestParam String password,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String email,
                                                @RequestParam(required = false) String deviceId) {
        try {
            User user = userService.register(studentId, username, password, phone, email);
            return Result.success(buildAuthPayload(user, deviceId));
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestParam String refreshToken,
                                               @RequestParam(required = false) String deviceId) {
        try {
            // 刷新时执行 refresh token 轮换，旧 token 会进入黑名单。
            TokenBundle tokenBundle = authTokenService.refreshTokens(refreshToken, deviceId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("accessToken", tokenBundle.getAccessToken());
            payload.put("refreshToken", tokenBundle.getRefreshToken());
            payload.put("accessExpiresIn", tokenBundle.getAccessExpiresIn());
            payload.put("refreshExpiresIn", tokenBundle.getRefreshExpiresIn());
            return Result.success(payload);
        } catch (IllegalArgumentException ex) {
            return Result.fail(mapAuthError(ex.getMessage()));
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String userId = UserContext.getUserId();
        String accessToken = extractAccessToken(request);
        // 按“所有设备退出”策略撤销当前用户全部登录态。
        authTokenService.revokeAllDevices(userId, accessToken);
        return Result.success();
    }

    @GetMapping("/center/summary")
    public Result<User> getSummary() {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        User user = userService.findByStudentId(studentId);
        if (user == null) {
            return Result.fail(ResultCode.DATA_NOT_FOUND);
        }
        return Result.success(user);
    }

    @PostMapping("/center/profile")
    public Result<Map<String, Object>> updateProfile(@RequestParam String username,
                                                     @RequestParam(required = false) String phone,
                                                     @RequestParam(required = false) String email,
                                                     @RequestParam(required = false) String bio,
                                                     @RequestParam(required = false) String deviceId,
                                                     HttpServletRequest request) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            User updated = userService.updateProfile(studentId, username, phone, email, bio);
            // 用户标识可能变化，先撤销旧会话，再签发新双令牌避免旧 token 继续可用。
            authTokenService.revokeAllDevices(studentId, extractAccessToken(request));
            Map<String, Object> payload = buildAuthPayload(updated, deviceId);
            payload.put("user", updated);
            return Result.success(payload);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/password")
    public Result<Map<String, Object>> updatePassword(@RequestParam String oldPassword,
                                                      @RequestParam String newPassword,
                                                      HttpServletRequest request) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            userService.changePassword(studentId, oldPassword, newPassword);
            // 改密后强制全端失效，防止旧设备继续访问。
            authTokenService.revokeAllDevices(studentId, extractAccessToken(request));
            Map<String, Object> payload = new HashMap<>();
            payload.put("forceRelogin", true);
            return Result.success(payload);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/address")
    public Result<User> addAddress(@RequestParam String receiverName,
                                   @RequestParam String receiverPhone,
                                   @RequestParam String campus,
                                   @RequestParam String detail,
                                   @RequestParam(required = false, defaultValue = "false") boolean defaultAddress) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            User user = userService.addAddress(studentId, receiverName, receiverPhone, campus, detail, defaultAddress);
            return Result.success(user);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/address/default")
    public Result<User> setDefaultAddress(@RequestParam Long addressId) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            User user = userService.setDefaultAddress(studentId, addressId);
            return Result.success(user);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/address/delete")
    public Result<User> deleteAddress(@RequestParam Long addressId) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            User user = userService.deleteAddress(studentId, addressId);
            return Result.success(user);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/notification/read")
    public Result<User> readNotification(@RequestParam Long notificationId) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            User user = userService.markNotificationRead(studentId, notificationId);
            return Result.success(user);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    @PostMapping("/center/avatar")
    public Result<User> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        String studentId = UserContext.getUserId();
        if (studentId == null) {
            return Result.fail("请先登录");
        }
        try {
            if (avatar == null || avatar.isEmpty()) {
                throw new IllegalArgumentException("请选择要上传的头像文件");
            }
            if (avatar.getSize() > MAX_AVATAR_SIZE) {
                throw new IllegalArgumentException("头像大小不能超过 2MB");
            }

            String extension = fileExtension(avatar.getOriginalFilename());
            if (!isAllowedAvatarExtension(extension)) {
                throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 格式");
            }

            Path avatarRoot = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(avatarRoot);

            String filename = studentId + "-" + UUID.randomUUID() + "." + extension;
            Path target = avatarRoot.resolve(filename);
            Files.copy(avatar.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            User user = userService.updateAvatar(studentId, "/user/avatar/" + filename);
            return Result.success(user);
        } catch (IllegalArgumentException | IOException ex) {
            return Result.fail(ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    private Map<String, Object> buildAuthPayload(User user, String deviceId) {
        TokenBundle tokenBundle = authTokenService.issueTokens(user.getStudentId(), user.getUsername(), deviceId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("accessToken", tokenBundle.getAccessToken());
        payload.put("refreshToken", tokenBundle.getRefreshToken());
        payload.put("accessExpiresIn", tokenBundle.getAccessExpiresIn());
        payload.put("refreshExpiresIn", tokenBundle.getRefreshExpiresIn());
        payload.put("userId", user.getStudentId());
        payload.put("userName", user.getUsername());
        return payload;
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private String fileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedAvatarExtension(String extension) {
        return "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)
                || "webp".equals(extension);
    }

    private ResultCode mapAuthError(String error) {
        if ("token_expired".equals(error)) {
            return ResultCode.TOKEN_EXPIRED;
        }
        if ("token_invalid".equals(error)) {
            return ResultCode.TOKEN_INVALID;
        }
        if ("token_revoked".equals(error)) {
            return ResultCode.TOKEN_REVOKED;
        }
        if ("refresh_token_expired".equals(error)) {
            return ResultCode.REFRESH_TOKEN_EXPIRED;
        }
        if ("refresh_token_invalid".equals(error)) {
            return ResultCode.REFRESH_TOKEN_INVALID;
        }
        if ("refresh_token_revoked".equals(error)) {
            return ResultCode.REFRESH_TOKEN_REVOKED;
        }
        if ("refresh_token_replaced".equals(error)) {
            return ResultCode.REFRESH_TOKEN_REPLACED;
        }
        return ResultCode.UNAUTHORIZED;
    }
}
