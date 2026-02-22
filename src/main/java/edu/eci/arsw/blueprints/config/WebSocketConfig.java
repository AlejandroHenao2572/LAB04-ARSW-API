package edu.eci.arsw.blueprints.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker          // Activa el broker STOMP sobre WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // El broker simple redistribuye mensajes a los clientes suscritos
        // a cualquier destino que empiece con /topic
        registry.enableSimpleBroker("/topic");

        // Los mensajes que llegan desde el cliente dirigidos al servidor
        // deben tener el prefijo /app (van al @MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Punto de entrada del WebSocket. El cliente conecta a:
        // ws://localhost:8080/ws-blueprints
        registry.addEndpoint("/ws-blueprints")
                .setAllowedOriginPatterns("*");
    }
}