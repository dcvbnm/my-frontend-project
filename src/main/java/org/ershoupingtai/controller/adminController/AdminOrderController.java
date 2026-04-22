package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/admin/orders")
    public String orderList(Model model) {
        model.addAttribute("orders", adminOrderService.getOrdersWithDetails());
        return "admin/orders";
    }
}
