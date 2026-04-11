package org.ershoupingtai.pojo;

import java.util.Date;

public class Message {
    private Long id;
    private Long conversationId;
    private Integer senderId;
    private Integer receiverId;
    private Integer messageType;
    private String content;
    private String fileUrl;
    private Long fileSize;
    private Integer duration;
    private Long replyToId;
    private Boolean isRead;
    private Boolean isRecalled;
    private Date recalledAt;
    private Boolean isDeletedBySender;
    private Boolean isDeletedByReceiver;
    private Date createdAt;

    public Message() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getIsRecalled() {
        return isRecalled;
    }

    public void setIsRecalled(Boolean isRecalled) {
        this.isRecalled = isRecalled;
    }

    public Date getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(Date recalledAt) {
        this.recalledAt = recalledAt;
    }

    public Boolean getIsDeletedBySender() {
        return isDeletedBySender;
    }

    public void setIsDeletedBySender(Boolean isDeletedBySender) {
        this.isDeletedBySender = isDeletedBySender;
    }

    public Boolean getIsDeletedByReceiver() {
        return isDeletedByReceiver;
    }

    public void setIsDeletedByReceiver(Boolean isDeletedByReceiver) {
        this.isDeletedByReceiver = isDeletedByReceiver;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
