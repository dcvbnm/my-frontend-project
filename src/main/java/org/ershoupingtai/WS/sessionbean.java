package org.ershoupingtai.WS;

import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class sessionbean {
    private WebSocketSession session;
    private Integer UserId;
    private Integer authorUserId;
    private Integer ConversationId;
}

