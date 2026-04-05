package org.ershoupingtai.controller.user;

import org.ershoupingtai.pojo.user.User;
import org.ershoupingtai.service.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Controller
public class UserCenterController {
    private static final String SESSION_LOGIN_KEY = "LOGIN_STUDENT_ID";
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    private final UserService userService;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    public UserCenterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        return currentStudentId(session) == null ? "redirect:/user/login" : "redirect:/user/center";
    }

    @GetMapping("/user/login")
    public String loginPage(HttpSession session) {
        if (currentStudentId(session) != null) {
            return "redirect:/user/center";
        }
        return "user/login";
    }

    @PostMapping("/user/login")
    public String doLogin(@RequestParam String studentId,
                          @RequestParam String password,
                          Model model,
                          HttpSession session) {
        try {
            User user = userService.login(studentId, password);
            session.setAttribute(SESSION_LOGIN_KEY, user.getStudentId());
            return "redirect:/user/center";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("studentId", studentId);
            return "user/login";
        }
    }

    @GetMapping("/user/register")
    public String registerPage(HttpSession session) {
        if (currentStudentId(session) != null) {
            return "redirect:/user/center";
        }
        return "user/register";
    }

    @PostMapping("/user/register")
    public String doRegister(@RequestParam String studentId,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String email,
                             Model model,
                             HttpSession session) {
        try {
            User user = userService.register(studentId, username, password, phone, email);
            session.setAttribute(SESSION_LOGIN_KEY, user.getStudentId());
            return "redirect:/user/center";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("studentId", studentId);
            model.addAttribute("username", username);
            model.addAttribute("phone", phone);
            model.addAttribute("email", email);
            return "user/register";
        }
    }

    @GetMapping("/user/center")
    public String userCenter(Model model, HttpSession session) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }
        return renderCenter(model, session, studentId, null, null);
    }

    @PostMapping("/user/center/profile")
    public String updateProfile(@RequestParam String username,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String bio,
                                HttpSession session,
                                Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.updateProfile(studentId, username, phone, email, bio);
            return renderCenter(model, session, studentId, "个人资料更新成功", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session,
                                 Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.changePassword(studentId, oldPassword, newPassword);
            return renderCenter(model, session, studentId, "密码修改成功", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar,
                               HttpSession session,
                               Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
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

            userService.updateAvatar(studentId, "/user/avatar/" + filename);
            return renderCenter(model, session, studentId, "头像上传成功", null);
        } catch (IllegalArgumentException | IOException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/address/add")
    public String addAddress(@RequestParam String receiverName,
                             @RequestParam String receiverPhone,
                             @RequestParam String campus,
                             @RequestParam String detail,
                             @RequestParam(required = false, defaultValue = "false") boolean defaultAddress,
                             HttpSession session,
                             Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.addAddress(studentId, receiverName, receiverPhone, campus, detail, defaultAddress);
            return renderCenter(model, session, studentId, "收货地址添加成功", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/address/default")
    public String setDefaultAddress(@RequestParam Long addressId,
                                    HttpSession session,
                                    Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.setDefaultAddress(studentId, addressId);
            return renderCenter(model, session, studentId, "默认地址已更新", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/address/delete")
    public String deleteAddress(@RequestParam Long addressId,
                                HttpSession session,
                                Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.deleteAddress(studentId, addressId);
            return renderCenter(model, session, studentId, "收货地址已删除", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @PostMapping("/user/center/notification/read")
    public String readNotification(@RequestParam Long notificationId,
                                   HttpSession session,
                                   Model model) {
        String studentId = currentStudentId(session);
        if (studentId == null) {
            return "redirect:/user/login";
        }

        try {
            userService.markNotificationRead(studentId, notificationId);
            return renderCenter(model, session, studentId, "消息已标记为已读", null);
        } catch (IllegalArgumentException ex) {
            return renderCenter(model, session, studentId, null, ex.getMessage());
        }
    }

    @GetMapping("/user/avatar/{fileName:.+}")
    public ResponseEntity<Resource> readAvatar(@PathVariable String fileName) {
        try {
            Path avatarRoot = Paths.get(avatarDir).toAbsolutePath().normalize();
            Path file = avatarRoot.resolve(fileName).normalize();
            if (!file.startsWith(avatarRoot) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            if (!StringUtils.hasText(contentType)) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/user/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

    private String currentStudentId(HttpSession session) {
        Object value = session.getAttribute(SESSION_LOGIN_KEY);
        return value == null ? null : String.valueOf(value);
    }

    private String renderCenter(Model model,
                                HttpSession session,
                                String studentId,
                                String success,
                                String error) {
        User user = userService.findByStudentId(studentId);
        if (user == null) {
            session.removeAttribute(SESSION_LOGIN_KEY);
            return "redirect:/user/login";
        }
        model.addAttribute("user", user);
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "user/center";
    }

    private String fileExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
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
}
