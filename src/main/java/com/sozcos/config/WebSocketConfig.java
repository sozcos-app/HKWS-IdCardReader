package com.sozcos.config;

import com.sozcos.component.IdCardWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final IdCardWebSocketHandler handler;

    public WebSocketConfig(IdCardWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/idcard").setAllowedOrigins("*");
    }
}