package org.ershoupingtai.service;

import java.util.Date;

import org.ershoupingtai.mapper.WsMapper;
import org.ershoupingtai.pojo.Conversation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WsService {
    @Autowired
    private WsMapper wsMapper;

    public int getConversationId(int userId, int sellerId) {
        Integer conversationId = wsMapper.getConversationId(userId, sellerId);
        return conversationId != null ? conversationId : -1;
    }

    public int checkConversationAccess(int conversationId, int userId) {
        Integer accessId = wsMapper.checkConversationAccess(conversationId, userId);
        return accessId != null ? accessId : -1;
    }

    public int getConversationIdByUserIds(int userId, int authorUserId) {
        Integer conversationId = wsMapper.getConversationIdByUserIds(userId, authorUserId);
        return conversationId != null ? conversationId : -1;
    }

    public int insertConversation(int userId1, int userId2) {
        Conversation conversation = new Conversation();
        conversation.setUserId1(userId1);
        conversation.setUserId2(userId2);
        conversation.setLastMessage(null);
        conversation.setLastMessageTime(null);
        conversation.setUnreadCount1(0);
        conversation.setUnreadCount2(0);
        conversation.setIsTop1(false);
        conversation.setIsTop2(false);
        conversation.setIsMuted1(false);
        conversation.setIsMuted2(false);
        conversation.setIsActive(true);
        conversation.setCreatedAt(new Date());
        if (wsMapper.insertConversation(conversation) > 0) {
            return conversation.getId().intValue();
        }
        return -1;
    }
}
