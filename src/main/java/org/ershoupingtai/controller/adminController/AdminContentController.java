package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping("/admin/statistics")
    public String statistics(Model model) {
        model.addAllAttributes(adminContentService.getStatistics());
        return "admin/statistics";
    }
}
