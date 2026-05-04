package org.ershoupingtai.service.adminService;

import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.mapper.adminMapper.AdminGoodsMapper;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final UserLoginMapper userLoginMapper;
    private final AdminGoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;

    public AdminDashboardService(UserLoginMapper userLoginMapper,
                                 AdminGoodsMapper goodsMapper,
                                 OrdersMapper ordersMapper) {
        this.userLoginMapper = userLoginMapper;
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userLoginMapper.countAllUsers());
        data.put("goodsCount", goodsMapper.countOnSaleGoods());
        data.put("orderCount", ordersMapper.countAllOrders());
        data.put("totalRevenue", ordersMapper.sumPaidRevenue());
        data.put("unpaidOrders", ordersMapper.countUnpaidOrders());
        data.put("notReceivedOrders", ordersMapper.countNotReceivedOrders());
        return data;
    }
}