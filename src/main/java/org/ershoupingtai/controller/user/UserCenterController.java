package org.ershoupingtai.controller.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class UserCenterController {
    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @GetMapping("/")
    public String root() {
        return "redirect:/user/login";
    }

    @GetMapping("/user/login")
    public String loginPage() {
        return "user/login";
    }

    @GetMapping("/user/register")
    public String registerPage() {
        return "user/register";
    }

    @GetMapping("/user/center")
    public String userCenter() {
        return "user/center";
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

}
