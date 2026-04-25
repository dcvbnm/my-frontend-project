package org.ershoupingtai.WS;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketSession;

public class sessionbean {
    private WebSocketSession session;
    private Integer userId;
    private Integer authorUserId;
    private Integer conversationId;
    
    // 无参构造器（必须）
    public sessionbean() {
    }
    
    // 全参构造器
    public sessionbean(WebSocketSession session, Integer userId, Integer authorUserId, Integer conversationId) {
        this.session = session;
        this.userId = userId;
        this.authorUserId = authorUserId;
        this.conversationId = conversationId;
    }
    
    // Getter 和 Setter
    public WebSocketSession getSession() {
        return session;
    }
    
    public void setSession(WebSocketSession session) {
        this.session = session;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public Integer getAuthorUserId() {
        return authorUserId;
    }
    
    public void setAuthorUserId(Integer authorUserId) {
        this.authorUserId = authorUserId;
    }
    
    public Integer getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }
}

