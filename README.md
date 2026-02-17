# LAB04-ARSW-API

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
