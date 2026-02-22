# LAB04-ARSW-API

- David Alejandro Patacon Henao

## Implementacion completa de CRUD

La documentacion completa de este API REST se encuentra en:  

https://github.com/AlejandroHenao2572/LAB03-REST.git

## Eliminar blueprint (no implementado en el laboratorio pasado)
```http
DELETE /api/v1/blueprints/{author}/{name}
```
**Parámetros:**
- `author` (path): Nombre del autor
- `name` (path): Nombre del blueprint

**Ejemplo:**
```bash
curl -X DELETE http://localhost:8080/api/v1/blueprints/john/house
```

**Respuesta exitosa (200 OK):**
```json
{
  "success": true,
  "message": "Blueprint deleted successfully",
  "data": null
}
```

**Errores:**
- `404 Not Found`: Blueprint no encontrado

**Descripción:**  
Elimina un blueprint específico del sistema. La operación es idempotente en el sentido de que múltiples solicitudes para eliminar el mismo blueprint resultarán en un error 404 después de la primera eliminación exitosa.

## Implementación CRUD

El endpoint DELETE completa las operaciones CRUD:

| Operación | Endpoint | Método HTTP | Descripción |
|-----------|----------|-------------|-------------|
| **Create** | `/api/v1/blueprints` | POST | Crear nuevo blueprint |
| **Read** | `/api/v1/blueprints` | GET | Obtener todos |
| **Read** | `/api/v1/blueprints/{author}` | GET | Obtener por autor |
| **Read** | `/api/v1/blueprints/{author}/{name}` | GET | Obtener específico |
| **Update** | `/api/v1/blueprints/{author}/{name}/points` | PUT | Agregar punto |
| **Delete** | `/api/v1/blueprints/{author}/{name}` | DELETE | Eliminar blueprint |

### Detalles de Implementación del DELETE

La funcionalidad de eliminación fue implementada en las siguientes capas:

1. **Capa de Persistencia** (`BlueprintPersistence.java`):
   ```java
   void deleteBlueprint(String author, String name) throws BlueprintNotFoundException;
   ```

2. **Implementación InMemory** (`InMemoryBlueprintPersistence.java`):
   - Valida existencia del blueprint
   - Elimina del Map interno
   - Lanza excepción si no existe

3. **Implementación PostgreSQL** (`PostgresBlueprintPersistence.java`):
   - Busca el blueprint en la base de datos
   - Usa el repositorio JPA para eliminarlo
   - Manejo automático de transacciones

4. **Capa de Servicio** (`BlueprintsServices.java`):
   - Delega la operación a la capa de persistencia
   - Mantiene la lógica de negocio limpia

5. **Capa de Controlador** (`BlueprintsAPIController.java`):
   - Endpoint REST con anotaciones OpenAPI
   - Manejo de respuestas 200 OK y 404 Not Found
   - Documentación completa en Swagger

## STOMP — Sincronización en tiempo real

Se agregó soporte para comunicación en tiempo real usando STOMP sobre WebSocket. Esto permite que múltiples clientes colaboren en un mismo blueprint recibiendo actualizaciones inmediatas cuando se agregan puntos.

### Dependencias

Se añadió la dependencia de WebSocket de Spring Boot en `pom.xml`:

```xml
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Archivos nuevos y cambios principales


### WebSocketConfig.java
`src/main/java/edu/eci/arsw/blueprints/config/WebSocketConfig.java`
```java
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
```
   - Configura STOMP sobre WebSocket.
   - Registra el endpoint de handshake `/ws-blueprints`.
   - Define el `applicationDestinationPrefix` como `/app` y habilita un `SimpleBroker` con prefijo `/topic`.

### BlueprintWebSocketController.java
`src/main/java/edu/eci/arsw/blueprints/controllers/BlueprintWebSocketController.java`
```java
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
    // DTO de entrada
    public record DrawEvent(String author, String name, PointDTO point) {}
    public record PointDTO(int x, int y) {}
}
```
   - Controlador que maneja mensajes entrantes vía STOMP.
   - Expone un `@MessageMapping("/draw")` que recibe eventos de dibujo.
   - El evento entrante tiene la forma JSON: `{ "author": "...", "name": "...", "point": { "x": <int>, "y": <int> } }`.
   - Al recibir un `draw` se sigue este flujo:
      1. `services.addPoint(author, name, x, y)` persiste el punto en la base de datos.
      2. `services.getBlueprint(author, name)` obtiene el blueprint actualizado.
      3. `broker.convertAndSend("/topic/blueprints.{author}.{name}", updatedBlueprint)` publica el blueprint actualizado a todos los suscriptores del topic correspondiente.

### Destinos (topics) y rutas STOMP

- Punto de publicación desde cliente al servidor (destino de aplicación):
   - `/app/draw`

- Destino (topic) al que el servidor publica las actualizaciones para que los clientes se suscriban:
   - `/topic/blueprints.{author}.{name}`

Ejemplo: si el autor es `juan` y el blueprint `plano-1`, el topic será `/topic/blueprints.juan.plano-1`.

### Contrato de mensajes

- Mensaje de entrada (cliente → servidor) publicado en `/app/draw`:

```json
{
   "author": "juan",
   "name": "plano-1",
   "point": { "x": 120, "y": 80 }
}
```
- Mensaje de salida (servidor → clientes) publicado en `/topic/blueprints.{author}.{name}`:
   - El servidor envía el objeto `Blueprint` completo, es decir:
```json
{
   "id": <numeric>,
   "author": "juan",
   "name": "plano-1",
   "points": [ { "x": 10, "y": 20 }, { "x": 120, "y": 80 }, ... ]
}
```
Enviar el blueprint completo (en lugar de solo el punto) garantiza que los clientes siempre reciban la versión final y filtrada del servidor.

### CORS y WebSocket
```java
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
```
- Para los endpoints REST se añadió un `CorsFilter` global (`CorsConfig.java`) que permite orígenes de  `http://localhost:5173` 

### Cómo probar STOMP localmente

1. Iniciar la API Spring Boot (por defecto en `http://localhost:8080`).

```
mvn spring-boot:run
```
2. Usar un cliente que soporte STOMP y conectar al endpoint WebSocket:

```
ws://localhost:8080/ws-blueprints
```

3. Suscribirse al topic del blueprint deseado:

```
/topic/blueprints.{author}.{name}
```

4. Publicar un mensaje en `/app/draw` con el payload descrito anteriormente. El servidor persistirá el punto y retransmitirá el blueprint actualizado a todos los suscriptores.

