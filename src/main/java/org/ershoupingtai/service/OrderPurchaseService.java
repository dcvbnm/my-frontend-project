package org.ershoupingtai.service;

import org.ershoupingtai.mapper.GoodsMapper;
import org.ershoupingtai.mapper.adminMapper.OrdersMapper;
import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.pojo.Messages;
import org.ershoupingtai.pojo.Orders;
import org.ershoupingtai.pojo.PurchaseResult;
import org.ershoupingtai.pojo.user.User;
import org.ershoupingtai.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Service
public class OrderPurchaseService {
    private static final String ADMIN_STUDENT_ID = "1111111111";

    private final GoodsMapper goodsMapper;
    private final OrdersMapper ordersMapper;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    public OrderPurchaseService(GoodsMapper goodsMapper,
                                OrdersMapper ordersMapper,
                                UserService userService,
                                JdbcTemplate jdbcTemplate) {
        this.goodsMapper = goodsMapper;
        this.ordersMapper = ordersMapper;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseResult purchase(String studentId, Long goodsId, Integer quantity) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("请先登录");
        }
        if (goodsId == null || goodsId <= 0) {
            throw new IllegalArgumentException("商品ID无效");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        User buyer = userService.findByStudentId(studentId);
        if (buyer == null || buyer.getId() == null) {
            throw new IllegalArgumentException("请先登录");
        }

        Goods goods = goodsMapper.selectByIdWithSellerStudentId(goodsId);
        if (goods == null || Boolean.TRUE.equals(goods.getIsDeleted())) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!Boolean.TRUE.equals(goods.getStock()) || goods.getGoodsQuantity() == null || goods.getGoodsQuantity() < 1) {
            throw new IllegalArgumentException("商品已下架或库存不足");
        }
        if (goods.getUserId() == null) {
            throw new IllegalArgumentException("商品卖家信息缺失");
        }
        if (buyer.getId().intValue() == goods.getUserId()) {
            throw new IllegalArgumentException("不能购买自己的商品");
        }

        int currentStock = goods.getGoodsQuantity();
        if (quantity > currentStock) {
            throw new IllegalArgumentException("购买数量不能大于库存数");
        }

        BigDecimal unitPrice = goods.getGoodsPrice();
        if (unitPrice == null) {
            throw new IllegalArgumentException("商品价格不能为空");
        }

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
        int remaining = currentStock - quantity;
        boolean soldOut = remaining < 1;

        Orders order = new Orders();
        order.setBuyerId(buyer.getId().intValue());
        order.setSellerId(goods.getUserId());
        order.setGoodsId(goods.getGoodsId());
        order.setPrice(unitPrice);
        order.setPurchaseQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setIsPaid(true);
        order.setOrderTime(new Date());
        order.setIsReceived(false);
        ordersMapper.insert(order);

        Goods update = new Goods();
        update.setGoodsId(goods.getGoodsId());
        update.setGoodsQuantity(remaining);
        update.setStock(!soldOut);
        goodsMapper.updateById(update);

        try {
            sendOrderNoticeToBuyer(order, goods, buyer, remaining, soldOut);
            sendOrderNoticeToSeller(order, goods, buyer, remaining, soldOut);
        } catch (Exception ex) {
            // 通知属于附加能力，不能影响订单主流程。
            System.err.println("订单通知发送失败: " + ex.getMessage());
            ex.printStackTrace();
        }

        PurchaseResult result = new PurchaseResult();
        result.setOrderId(order.getOrderId());
        result.setGoodsId(goods.getGoodsId());
        result.setRemainingQuantity(remaining);
        result.setSoldOut(soldOut);
        result.setTotalPrice(totalPrice);
        return result;
    }

    private void sendOrderNoticeToBuyer(Orders order, Goods goods, User buyer, int remaining, boolean soldOut) {
        String sellerName = goods.getSellerStudentId() == null ? String.valueOf(goods.getUserId()) : goods.getSellerStudentId();
        String orderNo = order.getOrderId() == null ? "-" : String.valueOf(order.getOrderId());
        String content = String.format(
            "【订单通知】你已成功购买商品《%s》x%d，总价 ¥%s，订单号 %s。卖家账号：%s，商品地址：%s%s。",
                safeText(goods.getGoodsName()),
                order.getPurchaseQuantity(),
                order.getTotalPrice().toPlainString(),
                orderNo,
            sellerName,
                safeText(goods.getGoodsLocation()),
                soldOut ? "，商品已下架" : ""
        );
        sendAdminMessage(buyer.getId().intValue(), content);
    }

    private void sendOrderNoticeToSeller(Orders order, Goods goods, User buyer, int remaining, boolean soldOut) {
        String buyerStudentId = buyer.getStudentId() == null ? String.valueOf(buyer.getId()) : buyer.getStudentId();
        String orderNo = order.getOrderId() == null ? "-" : String.valueOf(order.getOrderId());
        String content = String.format(
            "【订单通知】你的商品《%s》已被购买 x%d，买家学号：%s，订单号 %s，总价 ¥%s。商品地址：%s%s。",
                safeText(goods.getGoodsName()),
                order.getPurchaseQuantity(),
                buyerStudentId,
                orderNo,
                order.getTotalPrice().toPlainString(),
                safeText(goods.getGoodsLocation()),
                soldOut ? "，商品已下架" : ""
        );
        sendAdminMessage(goods.getUserId(), content);
    }

    private void sendAdminMessage(int receiverUserId, String content) {
        int adminId = resolveAdminUserId();
        Integer conversationId = findAdminConversationId(adminId, receiverUserId);
        if (conversationId == null) {
            conversationId = insertAdminConversation(adminId, receiverUserId);
        }
        if (conversationId == null) {
            throw new IllegalStateException("创建订单通知会话失败");
        }

        insertAdminMessage(conversationId, adminId, receiverUserId, content);
    }

    private int resolveAdminUserId() {
        try {
            Integer id = jdbcTemplate.queryForObject(
                    "SELECT TOP 1 UserId FROM dbo.UserLogin WHERE UserName = ?",
                    Integer.class, ADMIN_STUDENT_ID);
            if (id != null) return id;
        } catch (Exception ignored) {
        }

        // 如果没有找到该学号对应的用户，尝试创建一个
        try {
            jdbcTemplate.update("INSERT INTO dbo.UserLogin (UserName, UserPassword) VALUES (?, ?)", ADMIN_STUDENT_ID, "");
            Integer id = jdbcTemplate.queryForObject(
                    "SELECT TOP 1 UserId FROM dbo.UserLogin WHERE UserName = ? ORDER BY UserId DESC",
                    Integer.class, ADMIN_STUDENT_ID);
            if (id != null) return id;
        } catch (Exception ex) {
            // 无法创建 admin 用户则抛出，调用方会捕获并记录
            throw new IllegalStateException("无法创建或定位学号 1111111111 的管理员账户: " + ex.getMessage(), ex);
        }

        throw new IllegalStateException("定位学号 1111111111 的管理员账户失败");
    }

    private Integer findAdminConversationId(int adminId, int receiverUserId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT TOP 1 id FROM dbo.conversation WHERE (user_id1 = ? AND user_id2 = ?) OR (user_id1 = ? AND user_id2 = ?)",
                    Integer.class,
                    adminId, receiverUserId, receiverUserId, adminId
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer insertAdminConversation(int adminId, int receiverUserId) {
        int firstUserId = Math.min(adminId, receiverUserId);
        int secondUserId = Math.max(adminId, receiverUserId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.conversation (user_id1, user_id2, last_message, last_message_time, unread_count1, unread_count2, is_top1, is_top2, is_muted1, is_muted2, is_active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, firstUserId);
            ps.setInt(2, secondUserId);
            ps.setString(3, "");
            ps.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(5, 0);
            ps.setInt(6, 0);
            ps.setBoolean(7, false);
            ps.setBoolean(8, false);
            ps.setBoolean(9, false);
            ps.setBoolean(10, false);
            ps.setBoolean(11, true);
            ps.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(13, new java.sql.Timestamp(System.currentTimeMillis()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            return key.intValue();
        }

        return findAdminConversationId(firstUserId, secondUserId);
    }

    private void insertAdminMessage(int conversationId, int adminId, int receiverUserId, String content) {
        final String safeContent = normalizeContent(content);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.message (conversation_id, sender_id, receiver_id, message_type, content, created_at, time_str) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, conversationId);
            ps.setInt(2, adminId);
            ps.setInt(3, receiverUserId);
            ps.setInt(4, 1);
            ps.setString(5, safeContent);
            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(6, now);
            ps.setString(7, new java.text.SimpleDateFormat("HH:mm").format(new Date()));
            return ps;
        }, keyHolder);

        jdbcTemplate.update(
                "UPDATE dbo.conversation SET last_message = ?, last_message_time = ?, updated_at = ? WHERE id = ?",
                safeContent,
                new java.sql.Timestamp(System.currentTimeMillis()),
                new java.sql.Timestamp(System.currentTimeMillis()),
                conversationId
        );
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() > 500) {
            return normalized.substring(0, 500);
        }
        return normalized;
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "未命名商品" : value.trim();
    }
}