package cl.ipss.saborgourmet.repository;

import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ArchivoJsonRepositoryTest {

    @TempDir
    Path carpetaTemporal;

    private ArchivoJsonRepository crearRepositorio(Path ruta) {
        return new ArchivoJsonRepository(JsonMapper.builder().build(), ruta.toString());
    }

    @Test
    void creaArchivoJsonValidoSiNoExiste() throws Exception {
        Path archivo = carpetaTemporal.resolve("datos.json");
        ArchivoJsonRepository repository = crearRepositorio(archivo);

        assertTrue(Files.exists(archivo));
        assertTrue(repository.leer().getClientes().isEmpty());
        assertTrue(repository.leer().getMesas().isEmpty());
        assertTrue(repository.leer().getReservas().isEmpty());
    }

    @Test
    void guardaYRecuperaColeccionesTrasReinicio() {
        Path archivo = carpetaTemporal.resolve("datos.json");
        ArchivoJsonRepository primero = crearRepositorio(archivo);
        primero.modificar(datos -> {
            datos.getClientes().add(new Cliente(1L, "Ana", "Perez", "ana@example.com", "+56912345678"));
            datos.getMesas().add(new Mesa(1L, 10, 4, true));
            datos.getReservas().add(new Reserva(1L, 1L, 1L,
                    LocalDate.of(2027, 1, 10), LocalTime.of(20, 0), 3, EstadoReserva.ACTIVA));
            return null;
        });

        // Una segunda instancia comprueba que la lectura depende del archivo guardado.
        ArchivoJsonRepository despuesDeReiniciar = crearRepositorio(archivo);
        DatosRestaurante recuperados = despuesDeReiniciar.leer();
        assertEquals(1, recuperados.getClientes().size());
        assertEquals("Ana", recuperados.getClientes().get(0).getNombre());
        assertEquals(10, recuperados.getMesas().get(0).getNumero());
        assertEquals(LocalTime.of(20, 0), recuperados.getReservas().get(0).getHora());
        assertEquals(EstadoReserva.ACTIVA, recuperados.getReservas().get(0).getEstado());
    }

    @Test
    void noBorraElArchivoSiContieneJsonIncorrecto() throws Exception {
        Path archivo = carpetaTemporal.resolve("datos.json");
        String corrupto = "{ JSON INVALIDO";
        Files.writeString(archivo, corrupto, StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> crearRepositorio(archivo));
        assertEquals(corrupto, Files.readString(archivo, StandardCharsets.UTF_8));
    }

    @Test
    void excepcionEnOperacionNoGuardaCambios() {
        Path archivo = carpetaTemporal.resolve("datos.json");
        ArchivoJsonRepository repository = crearRepositorio(archivo);

        assertThrows(IllegalArgumentException.class, () -> repository.modificar(datos -> {
            datos.getMesas().add(new Mesa(1L, 5, 2, true));
            throw new IllegalArgumentException("Operación invalida");
        }));
        assertTrue(repository.leer().getMesas().isEmpty());
    }
}
