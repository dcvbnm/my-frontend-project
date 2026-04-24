package org.ershoupingtai.pojo;

import java.util.Date;

public class Conversation {
    private int id;
    private Integer userId1;
    private Integer userId2;
    private String lastMessage;
    private Date lastMessageTime;
    private Integer unreadCount1;
    private Integer unreadCount2;
    private Boolean isTop1;
    private Boolean isTop2;
    private Boolean isMuted1;
    private Boolean isMuted2;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    public Conversation() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId1() {
        return userId1;
    }

    public void setUserId1(Integer userId1) {
        this.userId1 = userId1;
    }

    public Integer getUserId2() {
        return userId2;
    }

    public void setUserId2(Integer userId2) {
        this.userId2 = userId2;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUnreadCount1() {
        return unreadCount1;
    }

    public void setUnreadCount1(Integer unreadCount1) {
        this.unreadCount1 = unreadCount1;
    }

    public Integer getUnreadCount2() {
        return unreadCount2;
    }

    public void setUnreadCount2(Integer unreadCount2) {
        this.unreadCount2 = unreadCount2;
    }

    public Boolean getIsTop1() {
        return isTop1;
    }

    public void setIsTop1(Boolean isTop1) {
        this.isTop1 = isTop1;
    }

    public Boolean getIsTop2() {
        return isTop2;
    }

    public void setIsTop2(Boolean isTop2) {
        this.isTop2 = isTop2;
    }

    public Boolean getIsMuted1() {
        return isMuted1;
    }

    public void setIsMuted1(Boolean isMuted1) {
        this.isMuted1 = isMuted1;
    }

    public Boolean getIsMuted2() {
        return isMuted2;
    }

    public void setIsMuted2(Boolean isMuted2) {
        this.isMuted2 = isMuted2;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
