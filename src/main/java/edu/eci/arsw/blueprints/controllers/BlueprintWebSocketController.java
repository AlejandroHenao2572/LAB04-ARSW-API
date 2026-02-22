// src/main/java/edu/eci/arsw/blueprints/controllers/BlueprintWebSocketController.java
package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class BlueprintWebSocketController {

    private final BlueprintsServices services;
    private final SimpMessagingTemplate broker;

    public BlueprintWebSocketController(BlueprintsServices services,
                                        SimpMessagingTemplate broker) {
        this.services = services;
        this.broker   = broker;
    }

    // Los clientes publican a /app/draw
    // El payload JSON que llega es: { "author": "...", "name": "...", "point": { "x": 0, "y": 0 } }
    @MessageMapping("/draw")
    public void handleDraw(DrawEvent event) throws BlueprintNotFoundException {
        // 1. Persiste el punto en la base de datos
        services.addPoint(event.author(), event.name(), event.point().x(), event.point().y());

        // 2. Lee el plano actualizado (ya con el nuevo punto)
        Blueprint updated = services.getBlueprint(event.author(), event.name());

        // 3. Distribuye a TODOS los clientes suscritos a ese topic (incluido el emisor)
        broker.convertAndSend(
            "/topic/blueprints." + event.author() + "." + event.name(),
            updated
        );
    }

    // DTO de entrada — record de Java (inmutable, sin boilerplate)
    public record DrawEvent(String author, String name, PointDTO point) {}
    public record PointDTO(int x, int y) {}
}