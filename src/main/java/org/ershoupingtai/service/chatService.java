package org.ershoupingtai.service;

import org.ershoupingtai.mapper.chatMapper;
import org.ershoupingtai.pojo.ChatConversationItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class chatService {

    private final chatMapper chatMapper;

    public chatService(chatMapper chatMapper) {
        this.chatMapper = chatMapper;
    }

    public List<ChatConversationItem> listConversationsByUserId(int userId) {
        return chatMapper.listConversationsByUserId(userId);
    }
}
