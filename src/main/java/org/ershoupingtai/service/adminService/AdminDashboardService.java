package org.ershoupingtai.service.adminService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.mapper.adminMapper.GoodsCategoryMapper;
import org.ershoupingtai.mapper.adminMapper.GoodsMapper;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final UserLoginMapper userLoginMapper;
    private final GoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final GoodsCategoryMapper categoryMapper;

    public AdminDashboardService(UserLoginMapper userLoginMapper,
                                 GoodsMapper goodsMapper,
                                 OrdersMapper ordersMapper,
                                 GoodsCategoryMapper categoryMapper) {
        this.userLoginMapper = userLoginMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.categoryMapper = categoryMapper;
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userLoginMapper.selectCount(null));
        data.put("goodsCount", goodsMapper.selectCount(new QueryWrapper<Goods>().eq("IsDeleted", 0)));
        data.put("orderCount", ordersMapper.selectCount(null));
        data.put("categoryCount", categoryMapper.selectCount(null));
        data.put("totalRevenue", ordersMapper.sumPaidRevenue());
        data.put("unpaidOrders", ordersMapper.countUnpaidOrders());
        data.put("notReceivedOrders", ordersMapper.countNotReceivedOrders());
        return data;
    }
}