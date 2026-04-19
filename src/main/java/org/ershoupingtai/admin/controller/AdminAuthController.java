package org.ershoupingtai.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class AdminAuthController {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String login(String username, String password, Model model, HttpServletRequest request) {
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("adminUser", username);
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("error", "用户名或密码错误");
        return "admin/login";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/admin/login";
    }
}
