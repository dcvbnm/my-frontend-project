package org.ershoupingtai.service.user;

import org.ershoupingtai.pojo.user.CollectionEntity;
import org.ershoupingtai.pojo.user.GoodsEntity;
import org.ershoupingtai.pojo.user.OrderEntity;
import org.ershoupingtai.pojo.user.User;
import org.ershoupingtai.pojo.user.UserAddress;
import org.ershoupingtai.pojo.user.UserGoodsView;
import org.ershoupingtai.pojo.user.UserInfoEntity;
import org.ershoupingtai.pojo.user.UserLoginEntity;
import org.ershoupingtai.pojo.user.UserNotification;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private static final String DEFAULT_TEXT = "未填写";
    private static final String DEFAULT_PHONE = "13800000000";
    private static final String DEFAULT_AVATAR = "/images/default-avatar.png";
    // 保留单实例编码器，统一用于注册/登录/改密。
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, HashSet<Long>> readNotificationCache = new ConcurrentHashMap<>();

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initDemoUser() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM dbo.UserLogin WHERE UserName = ?",
                    Integer.class,
                    "20260001"
            );
            if (count == null || count == 0) {
                register("20260001", "20260001", "123456", DEFAULT_PHONE, null);
            }
        } catch (DataAccessException ex) {
            // 数据库初始化失败不阻断启动，避免影响其他模块开发。
        }
    }

    @Transactional
    public User register(String studentId, String username, String rawPassword, String phone, String email) {
        validateRequired(studentId, "学号不能为空");
        validateRequired(rawPassword, "密码不能为空");

        String loginName = trimToNull(studentId);
        if (loginName == null) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (loginName.length() > 20) {
            throw new IllegalArgumentException("学号长度不能超过 20");
        }
        if (existsLoginName(loginName)) {
            throw new IllegalArgumentException("该学号已注册");
        }

        String password = trimToNull(rawPassword);
        if (password == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() > 20) {
            throw new IllegalArgumentException("密码长度不能超过 20");
        }

        Long userId = insertUserLogin(loginName, encodePassword(password));
        jdbcTemplate.update(
                "INSERT INTO dbo.UserInfo (UserId, Avatar, College, Campus, Phone, Address, Score, CreatDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                DEFAULT_AVATAR,
                crop(username, 20, DEFAULT_TEXT),
                DEFAULT_TEXT,
                normalizePhone(phone),
                DEFAULT_TEXT,
                100,
                Date.valueOf(LocalDate.now())
        );

        return buildUserByLoginName(loginName);
    }

    public User login(String studentId, String rawPassword) {
        validateRequired(studentId, "请输入学号");
        validateRequired(rawPassword, "请输入密码");

        UserLoginEntity login = getUserLoginByName(studentId.trim());
        if (login == null || !passwordMatches(rawPassword.trim(), login.getUserPassword())) {
            throw new IllegalArgumentException("学号或密码错误");
        }
        // 历史明文密码在首次成功登录后自动升级为 BCrypt，减少一次性迁移成本。
        upgradePasswordHashIfNeeded(login.getUserId(), rawPassword.trim(), login.getUserPassword());
        return buildUserByLoginName(login.getUserName());
    }

    public User findByStudentId(String studentId) {
        return buildUserByLoginName(studentId);
    }

    @Transactional
    public User updateProfile(String studentId, String username, String phone, String email, String bio) {
        UserLoginEntity login = getUserLoginByName(studentId);
        if (login == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(username, "昵称不能为空");

        String newLoginName = trimToNull(username);
        if (newLoginName == null) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (newLoginName.length() > 20) {
            throw new IllegalArgumentException("昵称长度不能超过 20");
        }
        if (!newLoginName.equals(login.getUserName()) && existsLoginName(newLoginName)) {
            throw new IllegalArgumentException("该昵称已被占用");
        }

        jdbcTemplate.update("UPDATE dbo.UserLogin SET UserName = ? WHERE UserId = ?", newLoginName, login.getUserId());
        jdbcTemplate.update(
                "UPDATE dbo.UserInfo SET College = ?, Phone = ?, Address = ? WHERE UserId = ?",
                crop(username, 20, DEFAULT_TEXT),
                normalizePhone(phone),
                crop(bio, 50, DEFAULT_TEXT),
                login.getUserId()
        );
        return buildUserByLoginName(newLoginName);
    }

    @Transactional
    public User updateAvatar(String studentId, String avatarUrl) {
        UserLoginEntity login = getUserLoginByName(studentId);
        if (login == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(avatarUrl, "头像地址不能为空");
        jdbcTemplate.update("UPDATE dbo.UserInfo SET Avatar = ? WHERE UserId = ?", avatarUrl, login.getUserId());
        return buildUserByLoginName(login.getUserName());
    }

    @Transactional
    public User addAddress(String studentId,
                           String receiverName,
                           String receiverPhone,
                           String campus,
                           String detail,
                           boolean defaultAddress) {
        UserLoginEntity login = getUserLoginByName(studentId);
        if (login == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        validateRequired(receiverName, "收货人不能为空");
        validateRequired(receiverPhone, "手机号不能为空");
        validateRequired(campus, "校区不能为空");
        validateRequired(detail, "详细地址不能为空");

        jdbcTemplate.update(
                "UPDATE dbo.UserInfo SET Phone = ?, Campus = ?, Address = ? WHERE UserId = ?",
                normalizePhone(receiverPhone),
                crop(campus, 20, DEFAULT_TEXT),
                crop(detail, 50, DEFAULT_TEXT),
                login.getUserId()
        );

        return buildUserByLoginName(login.getUserName());
    }

    public User setDefaultAddress(String studentId, Long addressId) {
        User user = buildUserByLoginName(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    @Transactional
    public User deleteAddress(String studentId, Long addressId) {
        UserLoginEntity login = getUserLoginByName(studentId);
        if (login == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        jdbcTemplate.update("UPDATE dbo.UserInfo SET Address = ? WHERE UserId = ?", DEFAULT_TEXT, login.getUserId());
        return buildUserByLoginName(login.getUserName());
    }

    public User markNotificationRead(String studentId, Long notificationId) {
        User user = buildUserByLoginName(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        readNotificationCache.computeIfAbsent(studentId, key -> new HashSet<>()).add(notificationId);
        user.getNotifications().forEach(item -> {
            if (notificationId.equals(item.getId())) {
                item.setRead(true);
            }
        });
        return user;
    }

    @Transactional
    public void changePassword(String studentId, String oldPassword, String newPassword) {
        UserLoginEntity login = getUserLoginByName(studentId);
        if (login == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(oldPassword, "旧密码不能为空");
        validateRequired(newPassword, "新密码不能为空");

        if (!passwordMatches(oldPassword.trim(), login.getUserPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        if (newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度至少 6 位");
        }
        if (newPassword.trim().length() > 20) {
            throw new IllegalArgumentException("新密码长度不能超过 20 位");
        }

        jdbcTemplate.update("UPDATE dbo.UserLogin SET UserPassword = ? WHERE UserId = ?", encodePassword(newPassword.trim()), login.getUserId());
    }

    private User buildUserByLoginName(String loginName) {
        if (loginName == null || loginName.trim().isEmpty()) {
            return null;
        }

        UserLoginEntity login = getUserLoginByName(loginName.trim());
        if (login == null) {
            return null;
        }
        UserInfoEntity info = getUserInfoByUserId(login.getUserId());

        User user = new User();
        user.setId(login.getUserId());
        user.setStudentId(login.getUserName());
        user.setUsername(trimToDefault(info.getCollege(), login.getUserName()));
        // 避免把密码哈希暴露到接口响应中。
        user.setPasswordHash(null);
        user.setAvatarUrl(info.getAvatar());
        user.setPhone(info.getPhone());
        user.setEmail(null);
        user.setBio(info.getAddress());
        user.setCreatedAt(info.getCreatDate().atStartOfDay());
        user.setUpdatedAt(LocalDateTime.now());

        user.setAddresses(buildAddresses(user, info));
        user.setPublishedGoods(loadPublishedGoods(login.getUserId()));
        user.setFavoriteGoods(loadFavoriteGoods(login.getUserId()));
        user.setNotifications(loadNotifications(login.getUserId(), login.getUserName()));
        return user;
    }

    private UserLoginEntity getUserLoginByName(String userName) {
        List<UserLoginEntity> rows = jdbcTemplate.query(
                "SELECT TOP 1 UserId, UserName, UserPassword FROM dbo.UserLogin WHERE UserName = ?",
                (rs, rowNum) -> {
                    UserLoginEntity entity = new UserLoginEntity();
                    entity.setUserId(rs.getLong("UserId"));
                    entity.setUserName(rs.getString("UserName"));
                    entity.setUserPassword(rs.getString("UserPassword"));
                    return entity;
                },
                userName
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UserInfoEntity getUserInfoByUserId(Long userId) {
        List<UserInfoEntity> rows = jdbcTemplate.query(
                "SELECT TOP 1 UserId, Avatar, College, Campus, Phone, Address, Score, CreatDate FROM dbo.UserInfo WHERE UserId = ?",
                (rs, rowNum) -> {
                    UserInfoEntity entity = new UserInfoEntity();
                    entity.setUserId(rs.getLong("UserId"));
                    entity.setAvatar(trimToDefault(rs.getString("Avatar"), DEFAULT_AVATAR));
                    entity.setCollege(trimToDefault(rs.getString("College"), DEFAULT_TEXT));
                    entity.setCampus(trimToDefault(rs.getString("Campus"), DEFAULT_TEXT));
                    entity.setPhone(normalizePhone(rs.getString("Phone")));
                    entity.setAddress(trimToDefault(rs.getString("Address"), DEFAULT_TEXT));
                    entity.setScore(rs.getObject("Score") == null ? 100 : rs.getInt("Score"));
                    entity.setCreatDate(rs.getDate("CreatDate") == null ? LocalDate.now() : rs.getDate("CreatDate").toLocalDate());
                    return entity;
                },
                userId
        );

        if (!rows.isEmpty()) {
            return rows.get(0);
        }

        UserInfoEntity fallback = new UserInfoEntity();
        fallback.setUserId(userId);
        fallback.setAvatar(DEFAULT_AVATAR);
        fallback.setCollege(DEFAULT_TEXT);
        fallback.setCampus(DEFAULT_TEXT);
        fallback.setPhone(DEFAULT_PHONE);
        fallback.setAddress(DEFAULT_TEXT);
        fallback.setScore(100);
        fallback.setCreatDate(LocalDate.now());
        return fallback;
    }

    private List<GoodsEntity> getGoodsByOwnerId(Long userId) {
        return jdbcTemplate.query(
                "SELECT GoodsId, GoodsImage, GoodsName, GoodsType, GoodsDescription, Price, GoodsQuantity, GoodsDate, Shelflife, Views, GoodsLocation, UserId, Stock, IsDeleted FROM dbo.Goods WHERE UserId = ? AND (IsDeleted = 0 OR IsDeleted IS NULL) ORDER BY GoodsDate DESC, GoodsId DESC",
                (rs, rowNum) -> mapGoodsEntity(rs),
                userId
        );
    }

    private GoodsEntity getGoodsById(Long goodsId) {
        List<GoodsEntity> rows = jdbcTemplate.query(
                "SELECT TOP 1 GoodsId, GoodsImage, GoodsName, GoodsType, GoodsDescription, Price, GoodsQuantity, GoodsDate, Shelflife, Views, GoodsLocation, UserId, Stock, IsDeleted FROM dbo.Goods WHERE GoodsId = ? AND (IsDeleted = 0 OR IsDeleted IS NULL)",
                (rs, rowNum) -> mapGoodsEntity(rs),
                goodsId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<CollectionEntity> getCollectionsByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT CollectId, UserId, GoodsId FROM dbo.Collection WHERE UserId = ? ORDER BY CollectId DESC",
                (rs, rowNum) -> {
                    CollectionEntity entity = new CollectionEntity();
                    entity.setCollectId(rs.getLong("CollectId"));
                    entity.setUserId(rs.getLong("UserId"));
                    entity.setGoodsId(rs.getLong("GoodsId"));
                    return entity;
                },
                userId
        );
    }

    private List<OrderEntity> getOrdersByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT OrderId, BuyerId, SellerId, GoodsId, Price, PurchaseQuantity, TotalPrice, IsPaid, OrderTime, IsReceived FROM dbo.Orders WHERE BuyerId = ? OR SellerId = ? ORDER BY OrderTime DESC, OrderId DESC",
                (rs, rowNum) -> {
                    OrderEntity entity = new OrderEntity();
                    entity.setOrderId(rs.getLong("OrderId"));
                    entity.setBuyerId(rs.getLong("BuyerId"));
                    entity.setSellerId(rs.getLong("SellerId"));
                    entity.setGoodsId(rs.getLong("GoodsId"));
                    entity.setPrice(rs.getBigDecimal("Price"));
                    entity.setPurchaseQuantity(rs.getInt("PurchaseQuantity"));
                    entity.setTotalPrice(rs.getBigDecimal("TotalPrice"));
                    entity.setIsPaid(rs.getBoolean("IsPaid"));
                    Timestamp ts = rs.getTimestamp("OrderTime");
                    entity.setOrderTime(ts == null ? LocalDateTime.now() : ts.toLocalDateTime());
                    entity.setIsReceived(rs.getBoolean("IsReceived"));
                    return entity;
                },
                userId,
                userId
        );
    }

    private GoodsEntity mapGoodsEntity(java.sql.ResultSet rs) throws java.sql.SQLException {
        GoodsEntity entity = new GoodsEntity();
        entity.setGoodsId(rs.getLong("GoodsId"));
        entity.setGoodsImage(rs.getString("GoodsImage"));
        entity.setGoodsName(rs.getString("GoodsName"));
        entity.setGoodsType(rs.getInt("GoodsType"));
        entity.setGoodsDescription(rs.getString("GoodsDescription"));
        entity.setPrice(rs.getBigDecimal("Price"));
        entity.setGoodsQuantity(rs.getInt("GoodsQuantity"));
        Date goodsDate = rs.getDate("GoodsDate");
        entity.setGoodsDate(goodsDate == null ? null : goodsDate.toLocalDate());
        entity.setShelflife(rs.getInt("Shelflife"));
        entity.setViews(rs.getInt("Views"));
        entity.setGoodsLocation(rs.getString("GoodsLocation"));
        entity.setUserId(rs.getLong("UserId"));
        entity.setStock(rs.getBoolean("Stock"));
        entity.setIsDeleted(rs.getBoolean("IsDeleted"));
        return entity;
    }

    private List<UserAddress> buildAddresses(User user, UserInfoEntity info) {
        List<UserAddress> list = new ArrayList<>();
        if (!DEFAULT_TEXT.equals(info.getAddress())) {
            UserAddress address = new UserAddress();
            address.setId(1L);
            address.setReceiverName(user.getUsername());
            address.setReceiverPhone(info.getPhone());
            address.setCampus(info.getCampus());
            address.setDetail(info.getAddress());
            address.setDefaultAddress(true);
            list.add(address);
        }
        return list;
    }

    private List<UserGoodsView> loadPublishedGoods(Long userId) {
        List<UserGoodsView> list = new ArrayList<>();
        for (GoodsEntity goods : getGoodsByOwnerId(userId)) {
            UserGoodsView item = new UserGoodsView();
            item.setId(goods.getGoodsId());
            item.setTitle(trimToDefault(goods.getGoodsName(), "未命名商品"));
            BigDecimal price = goods.getPrice();
            item.setPrice(price == null ? "￥0" : "￥" + price.stripTrailingZeros().toPlainString());
            item.setStatus(Boolean.FALSE.equals(goods.getStock()) ? "下架" : "在售");
            list.add(item);
        }
        return list;
    }

    private List<UserGoodsView> loadFavoriteGoods(Long userId) {
        List<UserGoodsView> list = new ArrayList<>();
        for (CollectionEntity collection : getCollectionsByUserId(userId)) {
            GoodsEntity goods = getGoodsById(collection.getGoodsId());
            if (goods == null) {
                continue;
            }
            UserGoodsView item = new UserGoodsView();
            item.setId(goods.getGoodsId());
            item.setTitle(trimToDefault(goods.getGoodsName(), "未命名商品"));
            BigDecimal price = goods.getPrice();
            item.setPrice(price == null ? "￥0" : "￥" + price.stripTrailingZeros().toPlainString());
            item.setStatus("收藏中");
            list.add(item);
        }
        return list;
    }

    private List<UserNotification> loadNotifications(Long userId, String loginName) {
        List<UserNotification> list = new ArrayList<>();
        for (OrderEntity order : getOrdersByUser(userId)) {
            GoodsEntity goods = getGoodsById(order.getGoodsId());
            String goodsName = goods == null ? "商品" : trimToDefault(goods.getGoodsName(), "商品");

            UserNotification notification = new UserNotification();
            notification.setId(order.getOrderId());
            notification.setTitle("订单提醒");
            notification.setContent(buildOrderMessage(order, goodsName, order.getBuyerId() == userId));
            notification.setCreatedAt(order.getOrderTime());
            notification.setRead(false);
            list.add(notification);
        }

        HashSet<Long> readIds = readNotificationCache.computeIfAbsent(loginName, key -> new HashSet<>());
        list.forEach(item -> item.setRead(readIds.contains(item.getId())));

        if (list.isEmpty()) {
            UserNotification sample = new UserNotification();
            sample.setId(1L);
            sample.setTitle("系统消息");
            sample.setContent("当前暂无订单通知，开始发布或购买商品后会在这里展示消息。");
            sample.setRead(true);
            sample.setCreatedAt(LocalDateTime.now());
            list.add(sample);
        }
        return list;
    }

    private String buildOrderMessage(OrderEntity order, String goodsName, boolean isBuyer) {
        if (isBuyer) {
            if (Boolean.TRUE.equals(order.getIsReceived())) {
                return "订单#" + order.getOrderId() + "（" + goodsName + "）已确认收货。";
            }
            if (Boolean.TRUE.equals(order.getIsPaid())) {
                return "订单#" + order.getOrderId() + "（" + goodsName + "）已支付，等待收货。";
            }
            return "订单#" + order.getOrderId() + "（" + goodsName + "）已创建，请尽快完成支付。";
        }

        if (Boolean.TRUE.equals(order.getIsReceived())) {
            return "订单#" + order.getOrderId() + "（" + goodsName + "）买家已确认收货。";
        }
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            return "订单#" + order.getOrderId() + "（" + goodsName + "）买家已支付，请尽快发货。";
        }
        return "订单#" + order.getOrderId() + "（" + goodsName + "）有新的交易请求。";
    }

    private boolean existsLoginName(String loginName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dbo.UserLogin WHERE UserName = ?",
                Integer.class,
                loginName
        );
        return count != null && count > 0;
    }

    private Long insertUserLogin(String loginName, String password) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.UserLogin (UserName, UserPassword) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, loginName);
            ps.setString(2, password);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 UserId FROM dbo.UserLogin WHERE UserName = ? ORDER BY UserId DESC",
                Long.class,
                loginName
        );
        if (id == null) {
            throw new IllegalStateException("注册失败，未能获取用户ID");
        }
        return id;
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String normalizePhone(String phone) {
        String value = trimToNull(phone);
        if (value == null) {
            return DEFAULT_PHONE;
        }
        String digitsOnly = value.replaceAll("[^0-9]", "");
        if (digitsOnly.length() != 11) {
            return DEFAULT_PHONE;
        }
        return digitsOnly;
    }

    private String crop(String value, int maxLength, String defaultValue) {
        String text = trimToDefault(value, defaultValue);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String encodePassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        // 新数据走 BCrypt，比对失败才会返回 false。
        if (isBcryptHash(storedPassword)) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        // 兼容历史明文数据，后续由 upgradePasswordHashIfNeeded 升级。
        return storedPassword.equals(rawPassword);
    }

    private boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private void upgradePasswordHashIfNeeded(Long userId, String rawPassword, String storedPassword) {
        if (isBcryptHash(storedPassword)) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE dbo.UserLogin SET UserPassword = ? WHERE UserId = ?",
                encodePassword(rawPassword),
                userId
        );
    }
}
