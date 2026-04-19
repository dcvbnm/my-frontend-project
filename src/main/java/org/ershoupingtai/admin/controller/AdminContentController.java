package org.ershoupingtai.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.ershoupingtai.admin.entity.Announcement;
import org.ershoupingtai.admin.entity.Goods;
import org.ershoupingtai.admin.entity.GoodsCategory;
import org.ershoupingtai.admin.mapper.AnnouncementMapper;
import org.ershoupingtai.admin.mapper.GoodsCategoryMapper;
import org.ershoupingtai.admin.mapper.GoodsMapper;
import org.ershoupingtai.admin.mapper.OrdersMapper;
import org.ershoupingtai.admin.mapper.UserLoginMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminContentController {

    private final AnnouncementMapper announcementMapper;
    private final GoodsCategoryMapper categoryMapper;
    private final GoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final UserLoginMapper userLoginMapper;

    public AdminContentController(AnnouncementMapper announcementMapper,
                                  GoodsCategoryMapper categoryMapper,
                                  GoodsMapper goodsMapper,
                                  OrdersMapper ordersMapper,
                                  UserLoginMapper userLoginMapper) {
        this.announcementMapper = announcementMapper;
        this.categoryMapper = categoryMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.userLoginMapper = userLoginMapper;
    }

    @GetMapping("/admin/announcements")
    public String announcements(Model model) {
        model.addAttribute("announcements", announcementMapper.selectList(null));
        return "admin/announcements";
    }

    @PostMapping("/admin/announcements/add")
    public String addAnnouncement(@RequestParam("title") String title,
                                  @RequestParam("content") String content,
                                  @RequestParam(value = "active", required = false) String active) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setCreatedAt(new java.util.Date());
        announcement.setIsActive(active != null);
        announcementMapper.insert(announcement);
        return "redirect:/admin/announcements";
    }

    @PostMapping("/admin/announcements/delete")
    public String deleteAnnouncement(@RequestParam("id") Integer id) {
        announcementMapper.deleteById(id);
        return "redirect:/admin/announcements";
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryMapper.selectList(null));
        return "admin/categories";
    }

    @PostMapping("/admin/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description) {
        GoodsCategory category = new GoodsCategory();
        category.setCategoryName(name);
        category.setDescription(description);
        categoryMapper.insert(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/delete")
    public String deleteCategory(@RequestParam("id") Integer id) {
        categoryMapper.deleteById(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/statistics")
    public String statistics(Model model) {
        model.addAttribute("activeUserCount", userLoginMapper.selectCount(null));
        model.addAttribute("goodsCount", goodsMapper.selectCount(new QueryWrapper<Goods>().eq("IsDeleted", 0)));
        model.addAttribute("orderCount", ordersMapper.selectCount(null));
        model.addAttribute("totalSales", ordersMapper.sumPaidRevenue());
        model.addAttribute("topCategories", transformTopCategories(goodsMapper.findTopCategoryStats()));
        return "admin/statistics";
    }

    private List<Map<String, Object>> transformTopCategories(List<Map<String, Object>> source) {
        return source;
    }
}
