package org.ershoupingtai.service.adminService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.ershoupingtai.service.adminService.Announcement;
import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.service.adminService.GoodsCategory;
import org.ershoupingtai.mapper.adminMapper.AnnouncementMapper;
import org.ershoupingtai.mapper.adminMapper.GoodsCategoryMapper;
import org.ershoupingtai.mapper.adminMapper.AdminGoodsMapper;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminContentService {

    private final AnnouncementMapper announcementMapper;
    private final GoodsCategoryMapper categoryMapper;
    private final AdminGoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final UserLoginMapper userLoginMapper;

    public AdminContentService(AnnouncementMapper announcementMapper,
                               GoodsCategoryMapper categoryMapper,
                               AdminGoodsMapper goodsMapper,
                               OrdersMapper ordersMapper,
                               UserLoginMapper userLoginMapper) {
        this.announcementMapper = announcementMapper;
        this.categoryMapper = categoryMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.userLoginMapper = userLoginMapper;
    }

    public List<Announcement> getAnnouncements() {
        return announcementMapper.selectList(null);
    }

    public void addAnnouncement(String title, String content, String active) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setCreatedAt(new java.util.Date());
        announcement.setIsActive(active != null);
        announcementMapper.insert(announcement);
    }

    public void deleteAnnouncement(Integer id) {
        announcementMapper.deleteById(id);
    }

    public List<GoodsCategory> getCategories() {
        return categoryMapper.selectList(null);
    }

    public void addCategory(String name, String description) {
        GoodsCategory category = new GoodsCategory();
        category.setCategoryName(name);
        category.setDescription(description);
        categoryMapper.insert(category);
    }

    public void deleteCategory(Integer id) {
        categoryMapper.deleteById(id);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeUserCount", userLoginMapper.selectCount(null));
        stats.put("goodsCount", goodsMapper.selectCount(new QueryWrapper<Goods>().eq("IsDeleted", 0)));
        stats.put("orderCount", ordersMapper.selectCount(null));
        stats.put("totalSales", ordersMapper.sumPaidRevenue());
        stats.put("topCategories", transformTopCategories(goodsMapper.findTopCategoryStats()));
        return stats;
    }

    private List<Map<String, Object>> transformTopCategories(List<Map<String, Object>> source) {
        return source;
    }
}