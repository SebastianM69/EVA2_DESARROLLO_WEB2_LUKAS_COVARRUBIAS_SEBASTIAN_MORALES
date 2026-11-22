# Sabor Gourmet — API REST de reservas

Proyecto de la Evaluación de Unidad 2 de Desarrollo de Software Web II.

## Objetivo

API REST para administrar clientes, mesas y reservas de un restaurante. Permite consultar disponibilidad, crear y modificar reservas y cancelarlas conservando el historial. No utiliza interfaz gráfica ni base de datos relacional.

## Tecnologías

- Java 21
- Spring Boot 4.2.0
- Spring Web MVC
- Jakarta Bean Validation
- Jackson (serialización JSON)
- Maven Wrapper
- JUnit 5
- Postman

## Arquitectura

```text
src/main/java/cl/ipss/saborgourmet/
├── controller/     Recibe HTTP y devuelve ResponseEntity
├── dto/            Recibe y valida datos de entrada
├── exception/      Convierte errores a JSON y códigos HTTP
├── model/          Clases Cliente, Mesa, Reserva y EstadoReserva
├── service/        Reglas de negocio, validaciones y disponibilidad
└── repository/     Lectura y escritura del archivo JSON
```

El recorrido habitual es `Controller → Service → Repository → datos.json`. Los Controller no contienen reglas de negocio ni leen el archivo; los Service comprueban las operaciones; el Repository realiza la persistencia.

## Requisitos de ejecución

Tener instalado Java JDK 21 y acceso a Internet para la primera descarga de dependencias. No se necesita instalar Maven globalmente: el proyecto incluye Maven Wrapper.

En Windows PowerShell, abrir una terminal en la carpeta `sabor-gourmet-api` (la que contiene `pom.xml`) y ejecutar:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Mantener abierta la terminal mientras se utiliza Postman. Para detener el servidor, presionar `Ctrl+C`.

Para ejecutar las pruebas sin iniciar manualmente la API:

```powershell
.\mvnw.cmd clean test
```

## Persistencia en `datos.json`

La ruta está configurada en `src/main/resources/application.properties`:

```properties
spring.application.name=sabor-gourmet-api
app.datos.ruta=datos.json
```

La ruta relativa se resuelve desde la carpeta desde la que se inicia Spring Boot. **Ejecutar los comandos dentro de `sabor-gourmet-api`** para que se utilice el archivo incluido en el proyecto.

El archivo contiene las colecciones `clientes`, `mesas` y `reservas`. Las referencias entre registros se guardan como `clienteId` y `mesaId`. Un ejemplo de estructura es:

```json
{
  "clientes": [{"id": 1, "nombre": "Ana", "apellido": "Rojas", "email": "ana@ejemplo.cl", "telefono": "+56911112222"}],
  "mesas": [{"id": 1, "numero": 5, "capacidad": 4, "activa": true}],
  "reservas": [{"id": 1, "clienteId": 1, "mesaId": 1, "fecha": "2031-05-14", "hora": "20:00:00", "cantidadPersonas": 3, "estado": "CANCELADA"}]
}
```

El contenido real de `datos.json` puede diferir porque las pruebas de Postman realizan operaciones de creación, modificación y cancelación. El archivo se inicializa si no existe. Si su JSON es ilegible, el repositorio no lo reemplaza automáticamente: informa el error y preserva el archivo para su revisión.

En las escrituras se utiliza un archivo temporal y se reemplaza el archivo principal después de serializarlo. No modificar manualmente `datos.json` mientras la aplicación está ejecutándose.

## Endpoints

### Clientes

| Método | Ruta | Acción | Éxito |
|---|---|---|---|
| GET | `/api/clientes` | Listar clientes | 200 |
| GET | `/api/clientes/{id}` | Obtener un cliente | 200 |
| POST | `/api/clientes` | Crear un cliente | 201 |
| PUT | `/api/clientes/{id}` | Actualizar un cliente | 200 |
| DELETE | `/api/clientes/{id}` | Eliminar cliente sin reservas asociadas | 204 |

Ejemplo de cuerpo para POST y PUT:

```json
{"nombre":"Ana","apellido":"Rojas","email":"ana@ejemplo.cl","telefono":"+56911112222"}
```

### Mesas

| Método | Ruta | Acción | Éxito |
|---|---|---|---|
| GET | `/api/mesas` | Listar mesas | 200 |
| GET | `/api/mesas/{id}` | Obtener una mesa | 200 |
| POST | `/api/mesas` | Crear una mesa | 201 |
| PUT | `/api/mesas/{id}` | Actualizar número, capacidad o estado | 200 |
| DELETE | `/api/mesas/{id}` | Eliminar mesa sin reservas asociadas | 204 |
| GET | `/api/mesas/disponibles?fecha=2031-05-14&hora=20:00&personas=3` | Consultar mesas libres | 200 |

Ejemplo de cuerpo para POST y PUT:

```json
{"numero":5,"capacidad":4,"activa":true}
```

### Reservas

| Método | Ruta | Acción | Éxito |
|---|---|---|---|
| GET | `/api/reservas` | Listar reservas activas y canceladas | 200 |
| GET | `/api/reservas/{id}` | Consultar una reserva | 200 |
| POST | `/api/reservas` | Crear reserva ACTIVA | 201 |
| PUT | `/api/reservas/{id}` | Modificar reserva ACTIVA | 200 |
| PATCH | `/api/reservas/{id}/cancelar` | Cambiar a CANCELADA sin borrar historial | 200 |

Ejemplo de cuerpo para POST y PUT:

```json
{"clienteId":1,"mesaId":1,"fecha":"2031-05-14","hora":"20:00","cantidadPersonas":3}
```

Los identificadores y el estado de una reserva los administra el servidor, no el cuerpo de entrada.

## Reglas de negocio

1. Un turno dura **2 horas**.
2. Una reserva ACTIVA ocupa su mesa durante todo el intervalo `[inicio, fin)`. Un turno que empieza exactamente cuando termina otro está permitido.
3. El cálculo de disponibilidad considera horarios que cruzan medianoche.
4. La mesa debe estar activa y su capacidad debe cubrir la cantidad de personas.
5. No se puede crear o modificar una reserva si hay otra reserva ACTIVA superpuesta para la misma mesa.
6. Al cancelar, la reserva continúa en el historial con estado `CANCELADA`, pero libera la disponibilidad.
7. Una reserva cancelada no se puede volver a cancelar ni modificar.
8. No se pueden eliminar clientes o mesas con reservas asociadas, incluidas las canceladas.
9. No se puede desactivar una mesa con reservas activas ni reducir su capacidad por debajo de una reserva activa existente.

## Estados HTTP

- **200 OK:** consultas, actualizaciones y cancelaciones con cuerpo JSON.
- **201 Created:** creaciones; incluyen cuerpo JSON y encabezado `Location`.
- **204 No Content:** eliminación válida sin cuerpo.
- **400 Bad Request:** datos obligatorios omitidos, formato inválido o cantidades no positivas.
- **404 Not Found:** ID de cliente, mesa o reserva inexistente.
- **409 Conflict:** duplicados, solapamientos, sobrecapacidad o integridad referencial.
- **500 Internal Server Error:** fallo imprevisto o de almacenamiento; la respuesta no publica trazas internas.

## Pruebas con Postman

1. Importar `postman/Sabor_Gourmet_EVA2.postman_collection.json`.
2. Verificar la variable de colección `baseUrl = http://localhost:8080`.
3. Iniciar el servidor.
4. Ejecutar la carpeta `01 - Pruebas funcionales, HTTP y errores` (39 solicitudes, en orden, 1 iteración).
5. Detener el servidor con `Ctrl+C`, **sin eliminar `datos.json`**.
6. Iniciarlo de nuevo con `mvnw.cmd spring-boot:run`.
7. Ejecutar solo la carpeta `02 - Persistencia` (3 consultas).

La primera carpeta crea y modifica registros de prueba. Los códigos 400, 404 y 409 son respuestas esperadas en los casos negativos. Las evidencias están en `docs/EVIDENCIAS_PRUEBAS.md`.

## Alcance

Esta entrega implementa una API REST para uso académico, con almacenamiento JSON en un archivo local. No incorpora autenticación, base de datos relacional ni interfaz gráfica, ya que no forman parte de la evaluación. La sincronización de escritura evita colisiones dentro de una sola instancia; no se diseñó para varias instancias compartiendo el mismo archivo.
