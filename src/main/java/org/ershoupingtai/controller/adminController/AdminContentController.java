package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping("/admin/announcements")
    public String announcements(Model model) {
        model.addAttribute("announcements", adminContentService.getAnnouncements());
        return "admin/announcements";
    }

    @PostMapping("/admin/announcements/add")
    public String addAnnouncement(@RequestParam("title") String title,
                                  @RequestParam("content") String content,
                                  @RequestParam(value = "active", required = false) String active) {
        adminContentService.addAnnouncement(title, content, active);
        return "redirect:/admin/announcements";
    }

    @PostMapping("/admin/announcements/delete")
    public String deleteAnnouncement(@RequestParam("id") Integer id) {
        adminContentService.deleteAnnouncement(id);
        return "redirect:/admin/announcements";
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", adminContentService.getCategories());
        return "admin/categories";
    }

    @PostMapping("/admin/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description) {
        adminContentService.addCategory(name, description);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/delete")
    public String deleteCategory(@RequestParam("id") Integer id) {
        adminContentService.deleteCategory(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/statistics")
    public String statistics(Model model) {
        model.addAllAttributes(adminContentService.getStatistics());
        return "admin/statistics";
    }
}
