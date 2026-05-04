package org.ershoupingtai.controller.adminController;

import org.ershoupingtai.service.adminService.AdminGoodsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class AdminGoodsController {

    private final AdminGoodsService adminGoodsService;

    public AdminGoodsController(AdminGoodsService adminGoodsService) {
        this.adminGoodsService = adminGoodsService;
    }

    @GetMapping("/admin/goods")
    public String goodsList(@RequestParam(value = "searchType", required = false) String searchType,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("goodsList", resolveGoods(searchType, keyword));
        return "admin/goods";
    }

    private List<org.ershoupingtai.pojo.Goods> resolveGoods(String searchType, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return adminGoodsService.getGoodsList();
        }
        String normalizedType = searchType == null ? "goodsName" : searchType.trim();
        if ("goodsId".equals(normalizedType)) {
            try {
                return adminGoodsService.searchGoods(Integer.valueOf(keyword.trim()), null, null);
            } catch (NumberFormatException ex) {
                return Collections.emptyList();
            }
        }
        if ("sellerId".equals(normalizedType)) {
            try {
                return adminGoodsService.searchGoods(null, null, Integer.valueOf(keyword.trim()));
            } catch (NumberFormatException ex) {
                return Collections.emptyList();
            }
        }
        return adminGoodsService.searchGoods(null, keyword.trim(), null);
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
