package org.ershoupingtai.admin.controller;

import org.ershoupingtai.admin.mapper.OrdersMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminOrderController {

    private final OrdersMapper ordersMapper;

    public AdminOrderController(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    @GetMapping("/admin/orders")
    public String orderList(Model model) {
        model.addAttribute("orders", ordersMapper.findAllWithDetails());
        return "admin/orders";
    }
}
