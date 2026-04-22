package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAllAttributes(adminDashboardService.getDashboardData());
        return "admin/dashboard";
    }
}
