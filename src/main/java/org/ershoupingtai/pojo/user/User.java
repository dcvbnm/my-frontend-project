package org.ershoupingtai.pojo.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private Long id;
    private String studentId;
    private String username;
    private String passwordHash;
    private String avatarUrl;
    private String phone;
    private String email;
    private String bio;
    private List<UserAddress> addresses = new ArrayList<>();
    private List<UserGoodsView> publishedGoods = new ArrayList<>();
    private List<UserGoodsView> favoriteGoods = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<UserAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<UserAddress> addresses) {
        this.addresses = addresses;
    }

    public List<UserGoodsView> getPublishedGoods() {
        return publishedGoods;
    }

    public void setPublishedGoods(List<UserGoodsView> publishedGoods) {
        this.publishedGoods = publishedGoods;
    }

    public List<UserGoodsView> getFavoriteGoods() {
        return favoriteGoods;
    }

    public void setFavoriteGoods(List<UserGoodsView> favoriteGoods) {
        this.favoriteGoods = favoriteGoods;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
