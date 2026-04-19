package org.ershoupingtai.admin.controller;

import org.ershoupingtai.admin.entity.Goods;
import org.ershoupingtai.admin.mapper.GoodsMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminGoodsController {

    private final GoodsMapper goodsMapper;

    public AdminGoodsController(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    @GetMapping("/admin/goods")
    public String goodsList(Model model) {
        List<Goods> goodsList = goodsMapper.findAllWithSeller();
        model.addAttribute("goodsList", goodsList);
        return "admin/goods";
    }

    @PostMapping("/admin/goods/action")
    public String goodsAction(@RequestParam("id") Integer id,
                              @RequestParam("action") String action) {
        if ("on".equals(action)) {
            goodsMapper.updateStock(id, true);
        } else if ("off".equals(action)) {
            goodsMapper.updateStock(id, false);
        } else if ("delete".equals(action)) {
            goodsMapper.softDelete(id);
        }
        return "redirect:/admin/goods";
    }
}
