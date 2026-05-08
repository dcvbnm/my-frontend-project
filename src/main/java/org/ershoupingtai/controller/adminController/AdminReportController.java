package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReportController {
    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reportService.listReports());
        return "admin/reports";
    }

    @PostMapping("/admin/reports/handled")
    public String markHandled(@RequestParam("id") Integer id) {
        reportService.markAsHandled(id);
        return "redirect:/admin/reports";
    }
}
