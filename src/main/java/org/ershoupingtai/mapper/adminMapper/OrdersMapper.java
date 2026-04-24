package org.ershoupingtai.mapper.adminMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.pojo.Orders;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT o.*, b.UserName AS buyerName, s.UserName AS sellerName, g.GoodsName AS goodsName " +
            "FROM Orders o " +
            "LEFT JOIN UserLogin b ON o.BuyerId = b.UserId " +
            "LEFT JOIN UserLogin s ON o.SellerId = s.UserId " +
            "LEFT JOIN Goods g ON o.GoodsId = g.GoodsId " +
            "ORDER BY o.OrderTime DESC")
    List<Orders> findAllWithDetails();

    @Select("SELECT ISNULL(SUM(TotalPrice), 0) FROM Orders WHERE IsPaid = 1")
    BigDecimal sumPaidRevenue();

    @Select("SELECT COUNT(*) FROM Orders WHERE IsPaid = 0")
    Integer countUnpaidOrders();

    @Select("SELECT COUNT(*) FROM Orders WHERE IsReceived = 0")
    Integer countNotReceivedOrders();
}
