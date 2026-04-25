package org.ershoupingtai.WS;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@Configuration
@EnableWebSocket
public class mywsconfig implements org.springframework.web.socket.config.annotation.WebSocketConfigurer {
    @Resource
    private mywshandler handler;
    @Resource
    private mywsinterceptor interceptor;

    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }

}
