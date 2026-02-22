package edu.eci.arsw.blueprints.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuración CORS global para todos los endpoints REST.
 * Permite que el frontend (Vite en localhost:5173) consuma la API
 * sin que el navegador bloquee las peticiones por política de origen.
 *
 * El endpoint WebSocket (/ws-blueprints) tiene su propio CORS
 * configurado en WebSocketConfig mediante setAllowedOriginPatterns.
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Origenes permitidos
        config.addAllowedOrigin("http://localhost:5173");   // Frontend

        // Métodos HTTP permitidos
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");

        // Cabeceras permitidas en la peticion
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuración a TODOS los endpoints REST
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
