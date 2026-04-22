package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminGoodsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminGoodsController {

    private final AdminGoodsService adminGoodsService;

    public AdminGoodsController(AdminGoodsService adminGoodsService) {
        this.adminGoodsService = adminGoodsService;
    }

    @GetMapping("/admin/goods")
    public String goodsList(Model model) {
        model.addAttribute("goodsList", adminGoodsService.getGoodsList());
        return "admin/goods";
    }

    @PostMapping("/admin/goods/action")
    public String goodsAction(@RequestParam("id") Integer id,
                              @RequestParam("action") String action) {
        if ("on".equals(action)) {
            adminGoodsService.updateGoodsStock(id, true);
        } else if ("off".equals(action)) {
            adminGoodsService.updateGoodsStock(id, false);
        } else if ("delete".equals(action)) {
            adminGoodsService.softDeleteGoods(id);
        }
        return "redirect:/admin/goods";
    }
}
