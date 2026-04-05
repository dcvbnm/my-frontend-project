package org.ershoupingtai.service.user;

import org.ershoupingtai.pojo.user.UserAddress;
import org.ershoupingtai.pojo.user.UserGoodsView;
import org.ershoupingtai.pojo.user.UserNotification;
import org.ershoupingtai.pojo.user.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicLong addressIdGenerator = new AtomicLong(1);
    private final AtomicLong goodsIdGenerator = new AtomicLong(1000);
    private final AtomicLong notificationIdGenerator = new AtomicLong(5000);
    private final Map<String, User> usersByStudentId = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDemoUser() {
        User demo = register("20260001", "演示用户", "123456", "13800000000", "demo@campus.edu");
        addAddress(demo.getStudentId(), "张同学", "13800000000", "北校区", "12栋 305", true);
        addAddress(demo.getStudentId(), "张同学", "13800000000", "南校区", "1号楼 108", false);
        appendDemoGoodsAndMessages(demo.getStudentId());
    }

    public User register(String studentId, String username, String rawPassword, String phone, String email) {
        validateRequired(studentId, "学号不能为空");
        validateRequired(username, "昵称不能为空");
        validateRequired(rawPassword, "密码不能为空");

        String normalizedStudentId = studentId.trim();
        if (usersByStudentId.containsKey(normalizedStudentId)) {
            throw new IllegalArgumentException("该学号已注册");
        }

        User user = new User();
        user.setId(idGenerator.getAndIncrement());
        user.setStudentId(normalizedStudentId);
        user.setUsername(username.trim());
        user.setPasswordHash(hashPassword(rawPassword));
        user.setAvatarUrl("/images/default-avatar.png");
        user.setPhone(trimToNull(phone));
        user.setEmail(trimToNull(email));
        user.setBio("这是我的个人简介，欢迎在校园二手平台交流。");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        usersByStudentId.put(normalizedStudentId, user);
        pushNotification(normalizedStudentId, "欢迎加入平台", "你的账号已创建成功，快去完善头像和收货地址吧。", false);
        return cloneUser(user);
    }

    public User login(String studentId, String rawPassword) {
        validateRequired(studentId, "请输入学号");
        validateRequired(rawPassword, "请输入密码");

        User user = usersByStudentId.get(studentId.trim());
        if (user == null || !user.getPasswordHash().equals(hashPassword(rawPassword))) {
            throw new IllegalArgumentException("学号或密码错误");
        }

        return cloneUser(user);
    }

    public User findByStudentId(String studentId) {
        User user = usersByStudentId.get(studentId);
        return user == null ? null : cloneUser(user);
    }

    public User updateProfile(String studentId, String username, String phone, String email, String bio) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(username, "昵称不能为空");

        user.setUsername(username.trim());
        user.setPhone(trimToNull(phone));
        user.setEmail(trimToNull(email));
        user.setBio(trimToNull(bio));
        user.setUpdatedAt(LocalDateTime.now());
        return cloneUser(user);
    }

    public User updateAvatar(String studentId, String avatarUrl) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(avatarUrl, "头像地址不能为空");

        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        pushNotification(studentId, "头像已更新", "你的个人头像已成功更新。", true);
        return cloneUser(user);
    }

    public User addAddress(String studentId,
                           String receiverName,
                           String receiverPhone,
                           String campus,
                           String detail,
                           boolean defaultAddress) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(receiverName, "收货人不能为空");
        validateRequired(receiverPhone, "手机号不能为空");
        validateRequired(campus, "校区不能为空");
        validateRequired(detail, "详细地址不能为空");

        UserAddress address = new UserAddress();
        address.setId(addressIdGenerator.getAndIncrement());
        address.setReceiverName(receiverName.trim());
        address.setReceiverPhone(receiverPhone.trim());
        address.setCampus(campus.trim());
        address.setDetail(detail.trim());
        address.setDefaultAddress(defaultAddress || user.getAddresses().isEmpty());

        if (address.isDefaultAddress()) {
            user.getAddresses().forEach(it -> it.setDefaultAddress(false));
        }
        user.getAddresses().add(address);
        user.setUpdatedAt(LocalDateTime.now());
        return cloneUser(user);
    }

    public User setDefaultAddress(String studentId, Long addressId) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserAddress target = user.getAddresses().stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("地址不存在"));

        user.getAddresses().forEach(it -> it.setDefaultAddress(false));
        target.setDefaultAddress(true);
        user.setUpdatedAt(LocalDateTime.now());
        return cloneUser(user);
    }

    public User deleteAddress(String studentId, Long addressId) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        boolean removed = user.getAddresses().removeIf(address -> address.getId().equals(addressId));
        if (!removed) {
            throw new IllegalArgumentException("地址不存在");
        }

        if (!user.getAddresses().isEmpty() && user.getAddresses().stream().noneMatch(UserAddress::isDefaultAddress)) {
            user.getAddresses().get(0).setDefaultAddress(true);
        }
        user.setUpdatedAt(LocalDateTime.now());
        return cloneUser(user);
    }

    public User markNotificationRead(String studentId, Long notificationId) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserNotification target = user.getNotifications().stream()
                .filter(item -> item.getId().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        target.setRead(true);
        user.setUpdatedAt(LocalDateTime.now());
        return cloneUser(user);
    }

    public void changePassword(String studentId, String oldPassword, String newPassword) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateRequired(oldPassword, "旧密码不能为空");
        validateRequired(newPassword, "新密码不能为空");

        if (!user.getPasswordHash().equals(hashPassword(oldPassword))) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        if (newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("新密码长度至少 6 位");
        }

        user.setPasswordHash(hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
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

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    private User cloneUser(User source) {
        User clone = new User();
        clone.setId(source.getId());
        clone.setStudentId(source.getStudentId());
        clone.setUsername(source.getUsername());
        clone.setPasswordHash(source.getPasswordHash());
        clone.setAvatarUrl(source.getAvatarUrl());
        clone.setPhone(source.getPhone());
        clone.setEmail(source.getEmail());
        clone.setBio(source.getBio());
        clone.setAddresses(cloneAddressList(source.getAddresses()));
        clone.setPublishedGoods(cloneGoodsList(source.getPublishedGoods()));
        clone.setFavoriteGoods(cloneGoodsList(source.getFavoriteGoods()));
        clone.setNotifications(cloneNotificationList(source.getNotifications()));
        clone.setCreatedAt(source.getCreatedAt());
        clone.setUpdatedAt(source.getUpdatedAt());
        return clone;
    }

    private List<UserAddress> cloneAddressList(List<UserAddress> source) {
        List<UserAddress> result = new ArrayList<>();
        for (UserAddress address : source) {
            UserAddress copy = new UserAddress();
            copy.setId(address.getId());
            copy.setReceiverName(address.getReceiverName());
            copy.setReceiverPhone(address.getReceiverPhone());
            copy.setCampus(address.getCampus());
            copy.setDetail(address.getDetail());
            copy.setDefaultAddress(address.isDefaultAddress());
            result.add(copy);
        }
        result.sort(Comparator.comparing(UserAddress::isDefaultAddress).reversed());
        return result;
    }

    private List<UserGoodsView> cloneGoodsList(List<UserGoodsView> source) {
        List<UserGoodsView> result = new ArrayList<>();
        for (UserGoodsView item : source) {
            UserGoodsView copy = new UserGoodsView();
            copy.setId(item.getId());
            copy.setTitle(item.getTitle());
            copy.setPrice(item.getPrice());
            copy.setStatus(item.getStatus());
            result.add(copy);
        }
        return result;
    }

    private List<UserNotification> cloneNotificationList(List<UserNotification> source) {
        List<UserNotification> result = new ArrayList<>();
        for (UserNotification item : source) {
            UserNotification copy = new UserNotification();
            copy.setId(item.getId());
            copy.setTitle(item.getTitle());
            copy.setContent(item.getContent());
            copy.setRead(item.isRead());
            copy.setCreatedAt(item.getCreatedAt());
            result.add(copy);
        }
        result.sort(Comparator.comparing(UserNotification::getCreatedAt).reversed());
        return result;
    }

    private void appendDemoGoodsAndMessages(String studentId) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            return;
        }

        user.getPublishedGoods().add(newGoods("九成新机械键盘", "￥119", "在售"));
        user.getPublishedGoods().add(newGoods("线性代数教材", "￥25", "已预定"));

        user.getFavoriteGoods().add(newGoods("二手显示器 24寸", "￥260", "收藏中"));
        user.getFavoriteGoods().add(newGoods("宿舍台灯", "￥30", "收藏中"));

        pushNotification(studentId, "系统消息", "你收藏的“宿舍台灯”刚刚降价 5 元。", false);
        pushNotification(studentId, "交易提醒", "你发布的“线性代数教材”收到 1 条新留言。", false);
    }

    private UserGoodsView newGoods(String title, String price, String status) {
        UserGoodsView goods = new UserGoodsView();
        goods.setId(goodsIdGenerator.getAndIncrement());
        goods.setTitle(title);
        goods.setPrice(price);
        goods.setStatus(status);
        return goods;
    }

    private void pushNotification(String studentId, String title, String content, boolean read) {
        User user = usersByStudentId.get(studentId);
        if (user == null) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setId(notificationIdGenerator.getAndIncrement());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());

        user.getNotifications().add(notification);
    }
}
