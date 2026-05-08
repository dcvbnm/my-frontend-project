package org.ershoupingtai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ershoupingtai.mapper.WsMapper;
import org.ershoupingtai.pojo.Conversation;
import org.ershoupingtai.pojo.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WsService {

    @Autowired
    private WsMapper wsMapper;

    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取两个用户之间的会话ID
     */
    public int getConversationId(int userId, int sellerId) {
        Integer id = wsMapper.getConversationId(userId, sellerId);
        return id != null ? id : -1;
    }

    /**
     * 创建新会话
     */
    public int insertConversation(int userId, int authorUserId) {
        int firstUserId = Math.min(userId, authorUserId);
        int secondUserId = Math.max(userId, authorUserId);
        Conversation conversation = new Conversation();
        conversation.setUserId1(firstUserId);
        conversation.setUserId2(secondUserId);
        conversation.setLastMessage("");
        conversation.setLastMessageTime(new Date());
        conversation.setUnreadCount1(0);
        conversation.setUnreadCount2(0);
        conversation.setIsTop1(false);
        conversation.setIsTop2(false);
        conversation.setIsMuted1(false);
        conversation.setIsMuted2(false);
        conversation.setIsActive(true);
        conversation.setCreatedAt(new Date());
        conversation.setUpdatedAt(new Date());
        
        int result = wsMapper.insertConversation(conversation);
        return result > 0 ? conversation.getId() : -1;
    }

    /**
     * 获取历史消息（返回 JSON 字符串）
     */
    public String getHistoryMessages(int conversationId, int userId) {
        try {
            List<Messages> messages = wsMapper.getMessagesByConversationId(conversationId);
            
            // 标记消息为已读
            wsMapper.markMessagesAsRead(conversationId, userId);
            wsMapper.clearUnreadCount(conversationId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "history");
            result.put("conversationId", conversationId);
            result.put("messages", messages);
            
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"type\":\"history\",\"conversationId\":" + conversationId + ",\"messages\":[]}";
        }
    }

    /**
     * 检查用户是否有权限访问会话，并返回对方用户ID
     */
    public int checkConversationAccess(int conversationId, int userId) {
        Integer result = wsMapper.checkConversationAccess(conversationId, userId);
        if (result == null) {
            return -1;
        }
        Conversation conversation = getConversationById(conversationId);
        if (conversation != null) {
            return conversation.getUserId1() == userId ? conversation.getUserId2() : conversation.getUserId1();
        }
        return -1;
    }

    /**
     * 根据会话ID获取会话信息
     */
    public Conversation getConversationById(int conversationId) {
        return wsMapper.getConversationById(conversationId);
    }

    /**
     * 根据两个用户ID获取会话ID
     */
    public int getConversationIdByUserIds(int userId, int authorUserId) {
        Integer id = wsMapper.getConversationIdByUserIds(userId, authorUserId);
        return id != null ? id : -1;
    }

    /**
     * 添加消息到数据库
     */
    public int addMessage(int conversationId, int senderId, Messages msg) {
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setCreatedAt(new Date());
        msg.setTimeStr(timeFormat.format(new Date()));
        if (msg.getMessageType() == null) {
            msg.setMessageType(1);
        }
        
        // 获取接收者ID
        Conversation conversation = getConversationById(conversationId);
        if (conversation == null) {
            throw new IllegalStateException("会话不存在: " + conversationId);
        }
        int receiverId = conversation.getUserId1() == senderId ? conversation.getUserId2() : conversation.getUserId1();
        msg.setReceiverId(receiverId);
        
        // 更新会话最后消息
        updateConversationLastMessage(conversationId, buildConversationPreview(msg), senderId);
        
        wsMapper.insertMessage(msg);
        return msg.getId();
    }

    /**
     * 插入消息并返回JSON（用于转发）
     */
    public String insertMessage(Messages msg, int realId) {
        try {
            msg.setId(realId);
            Map<String, Object> result = new HashMap<>();
            result.put("type", "new_message");
            result.put("messageId", realId);
            result.put("sender", msg.getSenderId());
            result.put("content", msg.getContent());
            result.put("messageType", msg.getMessageType());
            result.put("fileUrl", msg.getFileUrl());
            result.put("timeStr", msg.getTimeStr());
            result.put("conversationId", msg.getConversationId());
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"type\":\"new_message\",\"status\":\"failed\"}";
        }
    }

    /**
     * 消息确认响应（ACK）
     */
    public String ackMessage(int msgId, int realId, boolean delivered) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "ack");
            result.put("tempId", msgId);
            result.put("realId", realId);
            result.put("isread", delivered);
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"type\":\"ack\",\"delivered\":false}";
        }
    }

    /**
     * 生成"所有消息已读"的JSON响应
     */
    public String makeAllMessagesRead() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "all_read");
            result.put("status", "success");
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"type\":\"all_read\",\"status\":\"success\"}";
        }
    }

    /**
     * 标记会话为已读（清除未读计数）
     */
    public void markConversationAsRead(int conversationId, int userId) {
        wsMapper.markMessagesAsRead(conversationId, userId);
        wsMapper.clearUnreadCount(conversationId, userId);
    }

    /**
     * 更新消息已读状态
     */
    public void updateMessageReadStatus(int messageId, int readerUserId) {
        wsMapper.markSingleMessageAsRead(messageId, readerUserId);
    }

    /**
     * 标记会话为有未读消息
     */
    public void unRead(int conversationId, int authorUserId) {
        wsMapper.incrementUnreadCount(conversationId, authorUserId);
    }

    /**
     * 更新会话最后一条消息
     */
    private void updateConversationLastMessage(int conversationId, String lastMessage, int senderId) {
        Conversation conversation = getConversationById(conversationId);
        if (conversation != null) {
            conversation.setLastMessage(lastMessage);
            conversation.setLastMessageTime(new Date());
            conversation.setUpdatedAt(new Date());
            wsMapper.updateConversation(conversation);
        }
    }

    private String buildConversationPreview(Messages msg) {
        if (msg == null) {
            return "";
        }
        if (msg.getMessageType() != null && msg.getMessageType() == 2) {
            return "[图片]";
        }
        String content = msg.getContent();
        return content == null ? "" : content;
    }
}