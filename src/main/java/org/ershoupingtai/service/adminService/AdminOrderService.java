package org.ershoupingtai.service.adminService;

import org.ershoupingtai.service.adminService.Orders;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminOrderService {

    private final OrdersMapper ordersMapper;

    public AdminOrderService(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    public List<Orders> getOrdersWithDetails() {
        return ordersMapper.findAllWithDetails();
    }
}