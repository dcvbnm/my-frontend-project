package org.ershoupingtai.service.adminService;

import org.ershoupingtai.mapper.adminMapper.AnnouncementMapper;
import org.ershoupingtai.mapper.adminMapper.AdminGoodsMapper;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminContentService {

    private final AnnouncementMapper announcementMapper;
    private final AdminGoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final UserLoginMapper userLoginMapper;

    public AdminContentService(AnnouncementMapper announcementMapper,
                               AdminGoodsMapper goodsMapper,
                               OrdersMapper ordersMapper,
                               UserLoginMapper userLoginMapper) {
        this.announcementMapper = announcementMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.userLoginMapper = userLoginMapper;
    }

    public List<Announcement> getAnnouncements() {
        try {
            return announcementMapper.selectList(null);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
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

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeUserCount", userLoginMapper.countAllUsers());
        stats.put("goodsCount", goodsMapper.countOnSaleGoods());
        stats.put("orderCount", ordersMapper.countAllOrders());
        stats.put("totalSales", ordersMapper.sumPaidRevenue());
        return stats;
    }
}