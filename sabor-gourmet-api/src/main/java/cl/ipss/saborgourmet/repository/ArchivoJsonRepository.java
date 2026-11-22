package cl.ipss.saborgourmet.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Function;

/**
 * Escribe primero en un archivo temporal para no truncar datos.json.
 * Cuando el sistema de archivos lo permite, reemplaza el original de forma atómica.
 */
@Repository
public class ArchivoJsonRepository {

    private final JsonMapper jsonMapper;
    private final Path rutaArchivo;

    public ArchivoJsonRepository(
            JsonMapper jsonMapper,
            @Value("${app.datos.ruta:datos.json}") String ruta) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper es obligatorio");
        this.rutaArchivo = Path.of(ruta).toAbsolutePath().normalize();
        inicializarArchivo();
    }

    public synchronized DatosRestaurante leer() {
        try {
            String contenido = Files.readString(rutaArchivo, StandardCharsets.UTF_8);
            DatosRestaurante datos = jsonMapper.readValue(contenido, DatosRestaurante.class);
            validarEstructura(datos);
            return datos;
        } catch (IOException | JacksonException ex) {
            throw new IllegalStateException(
                    "No fue posible leer el archivo JSON. Los datos existentes no se modificaron.", ex);
        }
    }

    public synchronized void guardar(DatosRestaurante datos) {
        validarEstructura(datos);

        final String contenido;
        try {
            contenido = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(datos);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No fue posible serializar los datos a JSON.", ex);
        }

        Path temporal = null;
        try {
            Path carpeta = rutaArchivo.getParent();
            Files.createDirectories(carpeta);
            temporal = Files.createTempFile(carpeta, "sabor-gourmet-", ".tmp");
            Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, rutaArchivo,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible guardar datos.json. Se conservo el archivo anterior si existia.", ex);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ex) {
                    // La limpieza no debe ocultar el fallo que originó el cierre.
                }
            }
        }
    }

    /** Evita que dos solicitudes modifiquen el mismo archivo simultáneamente. */
    public synchronized <T> T modificar(Function<DatosRestaurante, T> operacion) {
        DatosRestaurante datos = leer();
        T resultado = operacion.apply(datos);
        guardar(datos);
        return resultado;
    }

    private void inicializarArchivo() {
        if (Files.notExists(rutaArchivo)) {
            guardar(new DatosRestaurante());
        } else {
            // Un archivo dañado debe informar el error, no reiniciarse vacío.
            leer();
        }
    }

    private void validarEstructura(DatosRestaurante datos) {
        if (datos == null || datos.getClientes() == null
                || datos.getMesas() == null || datos.getReservas() == null) {
            throw new IllegalStateException(
                    "El JSON debe contener clientes, mesas y reservas como colecciones validas.");
        }
    }
}
