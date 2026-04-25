package org.ershoupingtai.WS;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ershoupingtai.pojo.Messages;
import org.ershoupingtai.service.WsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

// WebSocket处理器，负责处理WebSocket连接和消息
@Component
public class mywshandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, sessionbean> sessionMap = new ConcurrentHashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private WsService wsService;

    // 处理WebSocket连接建立事件
    @Override
    public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
        // 从 URL 参数获取
        String query = session.getUri().getQuery();
        String userIdStr = null;
        for (String param : query.split("&")) {
            if (param.startsWith("UserId=")) {
                userIdStr = param.substring(7);
                break;
            }
        }
        int userId = Integer.parseInt(userIdStr);
        sessionbean sb = new sessionbean();
        sb.setSession(session);
        sb.setUserId(userId);
        sessionMap.put(session.getId(), sb);
        System.out.println("WebSocket连接已建立: " + session.getId());
    }

    // 处理WebSocket连接关闭事件
    @Override
    public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessionMap.remove(session.getId());
        System.out.println("WebSocket连接已关闭: " + session.getId());
    }

    // 处理WebSocket文本消息
    @Override
    public void handleTextMessage(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.TextMessage message) throws Exception {
        sessionbean sb = sessionMap.get(session.getId());
        String payload = message.getPayload();        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(payload, Map.class);

        int type = (int) data.get("type");
        
        if (type == 1) {
            //场景1：与商家的对话
            int sellerId = (int) data.get("senderId");
            int userId = (int) data.get("currentUserId");
            if (wsService.getConversationId(userId, sellerId) == -1) {
                int conversationId = wsService.insertConversation(userId, sellerId);
                if (conversationId != -1) {
                    sb.setConversationId(conversationId);
                    sb.setAuthorUserId(sellerId);
                    System.out.println("新会话已创建，ID: " + conversationId);
                } else {
                    System.out.println("会话创建失败");
                }
            } else {
                sb.setConversationId(wsService.getConversationId(userId, sellerId));
                sb.setAuthorUserId(sellerId);
                session.sendMessage(new TextMessage(wsService.getHistoryMessages(wsService.getConversationId(userId, sellerId), userId)));
                wsService.markConversationAsRead(sb.getConversationId(), sb.getUserId());
                int authorUserId = sb.getAuthorUserId();
                WebSocketSession authorSession = getSessionByAuthorUserId(authorUserId);
                if (authorSession != null && authorSession.isOpen()) {
                    authorSession.sendMessage(new TextMessage(wsService.makeAllMessagesRead()));
                }
                System.out.println("会话已存在，ID: " + wsService.getConversationId(userId, sellerId));
            }
        } else if (type == 2) {
            //场景2：从消息列表进入，打开指定会话
            int userId = (int) data.get("currentUserId");
            int conversationId = (int) data.get("conversationId");
            if (wsService.checkConversationAccess(conversationId, userId) != -1) {
                sb.setConversationId(conversationId);
                sb.setAuthorUserId(wsService.checkConversationAccess(conversationId, userId));
                System.out.println("会话访问验证通过，ID: " + conversationId);
                session.sendMessage(new TextMessage(wsService.getHistoryMessages(conversationId, userId)));
                wsService.markConversationAsRead(conversationId, sb.getUserId());
                int authorUserId = sb.getAuthorUserId();
                WebSocketSession authorSession = getSessionByAuthorUserId(authorUserId);
                if (authorSession != null && authorSession.isOpen()) {
                    authorSession.sendMessage(new TextMessage(wsService.makeAllMessagesRead()));
                    wsService.markConversationAsRead(sb.getConversationId(), sb.getUserId());
                }
            } else {
                System.out.println("会话访问验证失败，用户ID: " + userId + " 会话ID: " + conversationId);
            }
        } else if (type == 3) {
            //场景3：与指定用户聊天
            int userId = (int) data.get("currentUserId");
            int authorUserId = (int) data.get("authorUserId");
            if (wsService.getConversationIdByUserIds(userId, authorUserId) == -1) {
                int conversationId = wsService.insertConversation(userId, authorUserId);
                if (conversationId != -1) {
                    sb.setConversationId(conversationId);
                    sb.setAuthorUserId(authorUserId);
                    System.out.println("新会话已创建，ID: " + conversationId);
                } else {
                    System.out.println("会话创建失败");
                }
            } else {
                sb.setConversationId(wsService.getConversationIdByUserIds(userId, authorUserId));
                sb.setAuthorUserId(authorUserId);
                session.sendMessage(new TextMessage(wsService.getHistoryMessages(wsService.getConversationIdByUserIds(userId, authorUserId), userId)));
                wsService.markConversationAsRead(sb.getConversationId(), sb.getUserId());
                WebSocketSession authorSession = getSessionByAuthorUserId(authorUserId);
                if (authorSession != null && authorSession.isOpen()) {
                    authorSession.sendMessage(new TextMessage(wsService.makeAllMessagesRead()));
                    wsService.markConversationAsRead(sb.getConversationId(), sb.getUserId());
                }
                System.out.println("会话已存在，ID: " + wsService.getConversationIdByUserIds(userId, authorUserId));
            }
        } else if(type == 4) {
            //场景4：发送消息
            System.out.println("收到消息: " + data + " 来自会话: " + session.getId());
            Messages msg = mapper.convertValue(data.get("message"), Messages.class);
            int realId = wsService.addMessage(sb.getConversationId(), sb.getAuthorUserId(), msg);
            String json = wsService.insertMessage(msg, realId);
            WebSocketSession otherSession = getSessionByAuthorUserId(sb.getAuthorUserId());
            if (otherSession != null && otherSession.isOpen()) {
                otherSession.sendMessage(new TextMessage(json));
                session.sendMessage(new TextMessage(wsService.ackMessage(msg.getId(), realId, true)));
                wsService.updateMessageReadStatus(realId, sb.getAuthorUserId());
            } else {
                session.sendMessage(new TextMessage(wsService.ackMessage(msg.getId(), realId, false)));
                wsService.unRead(sb.getConversationId(), sb.getAuthorUserId());
            }
        } else if(type == 5) {
            //场景5：正在输入
            boolean isTyping = (boolean) data.get("isTyping");
            WebSocketSession otherSession = getSessionByAuthorUserId(sb.getAuthorUserId());
            if (otherSession != null && otherSession.isOpen()) {
                Map<String, Object> typingData = Map.of(
                    "type", "typing",
                    "isTyping", isTyping
                );
                String json = mapper.writeValueAsString(typingData);
                otherSession.sendMessage(new TextMessage(json));
            }
        }
    }

    //辅助函数
    // ✅ 根据 userId 查找 sessionbean
    private WebSocketSession getSessionByAuthorUserId(Integer authorUserId) {
        if (authorUserId == null) return null;
        
        for (sessionbean sb : sessionMap.values()) {
            if (sb.getAuthorUserId() == authorUserId) {
                return sb.getSession();
            }
        }
        return null;
    }

}
