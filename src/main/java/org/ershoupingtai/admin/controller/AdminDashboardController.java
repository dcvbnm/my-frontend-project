package org.ershoupingtai.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.ershoupingtai.admin.entity.Goods;
import org.ershoupingtai.admin.mapper.GoodsCategoryMapper;
import org.ershoupingtai.admin.mapper.GoodsMapper;
import org.ershoupingtai.admin.mapper.OrdersMapper;
import org.ershoupingtai.admin.mapper.UserLoginMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final UserLoginMapper userLoginMapper;
    private final GoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final GoodsCategoryMapper categoryMapper;

    public AdminDashboardController(UserLoginMapper userLoginMapper,
                                    GoodsMapper goodsMapper,
                                    OrdersMapper ordersMapper,
                                    GoodsCategoryMapper categoryMapper) {
        this.userLoginMapper = userLoginMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userLoginMapper.selectCount(null));
        model.addAttribute("goodsCount", goodsMapper.selectCount(new QueryWrapper<Goods>().eq("IsDeleted", 0)));
        model.addAttribute("orderCount", ordersMapper.selectCount(null));
        model.addAttribute("categoryCount", categoryMapper.selectCount(null));
        model.addAttribute("totalRevenue", ordersMapper.sumPaidRevenue());
        model.addAttribute("unpaidOrders", ordersMapper.countUnpaidOrders());
        model.addAttribute("notReceivedOrders", ordersMapper.countNotReceivedOrders());
        return "admin/dashboard";
    }
}
