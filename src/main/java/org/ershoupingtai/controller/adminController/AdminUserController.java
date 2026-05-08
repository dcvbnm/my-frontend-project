package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/admin/users")
    public String userList(Model model) {
        model.addAttribute("users", adminUserService.getUsersWithLogin());
        model.addAttribute("adminChatUserId", adminUserService.getAdminChatUserId());
        return "admin/users";
    }

    @PostMapping("/admin/users/delete")
    public String deleteUser(@RequestParam("id") Integer id) {
        adminUserService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
