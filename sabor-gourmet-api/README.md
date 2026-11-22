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

## Ejecución desde cualquier ubicación (rutas relativas)

**Requisitos:** JDK 21 configurado en `PATH`, permisos para leer y escribir en la carpeta del proyecto, conexión a Internet para la primera descarga de dependencias y puerto 8080 disponible. No hace falta instalar Maven de forma independiente: están incluidos `mvnw`, `mvnw.cmd` y la carpeta `.mvn`.

El repositorio de Git contiene la subcarpeta `sabor-gourmet-api/`. Después de clonar el repositorio o extraer el archivo ZIP, hay que abrir una terminal **dentro de esa subcarpeta**, donde está `pom.xml`. La ubicación del directorio padre es indiferente.

### Windows (PowerShell)

Abre la carpeta `sabor-gourmet-api` en el Explorador, escribe `powershell` en la barra de direcciones y presiona Enter. Luego ejecuta:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

La primera orden compila y ejecuta las pruebas; la segunda inicia la API. `Ctrl+C` detiene el servidor. También puedes acceder desde el directorio del repositorio con `cd .\sabor-gourmet-api`.

### Linux o macOS

Desde la carpeta `sabor-gourmet-api`:

```bash
chmod +x mvnw  # solo si el archivo no tiene permiso de ejecución
./mvnw clean test
./mvnw spring-boot:run
```

No se requieren scripts adicionales para iniciar la aplicación. El acceso a la API es `http://localhost:8080`. Postman se configura con esta dirección mediante la variable `baseUrl`.

### Directorio de trabajo y portabilidad

La propiedad `app.datos.ruta=datos.json` es **relativa**: `ArchivoJsonRepository` la resuelve contra el directorio de trabajo con `Path.of(ruta).toAbsolutePath().normalize()`. Por eso, si se ejecuta desde `sabor-gourmet-api`, el archivo utilizado es `sabor-gourmet-api/datos.json` tanto en Windows como en Linux o macOS. No hay una ruta de usuario o de Escritorio escrita en el código.

Si la aplicación se abre desde un IDE, configura su *working directory* como la carpeta que contiene `pom.xml`. Si se ejecuta desde otra ubicación, podría crear o utilizar `datos.json` allí en lugar del que viene con el proyecto.

Antes de repetir una demostración, haz una copia de `datos.json` si necesitas conservar los registros: las pruebas funcionales de Postman **modifican ese archivo**.

## Persistencia en `datos.json`

La ruta está configurada en `src/main/resources/application.properties`:

```properties
spring.application.name=sabor-gourmet-api
app.datos.ruta=datos.json
```

La ruta se resuelve desde el directorio de trabajo. Para usar el JSON incluido en el proyecto, los comandos deben ejecutarse desde la carpeta `sabor-gourmet-api`, como se indica arriba.

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

## Informe técnico

El informe académico está en `docs/EVU2_GRUPO_COVARRUBIAS_LUKAS_MORALES_SEBASTIAN.docx`. Antes de entregarlo, completar el nombre del docente en la portada.
