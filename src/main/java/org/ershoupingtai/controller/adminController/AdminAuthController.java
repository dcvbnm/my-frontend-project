package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String login(String username, String password, Model model, HttpServletRequest request) {
        if (adminAuthService.authenticate(username, password)) {
            adminAuthService.login(request, username);
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("error", "用户名或密码错误");
        return "admin/login";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        adminAuthService.logout(request);
        return "redirect:/admin/login";
    }
}
