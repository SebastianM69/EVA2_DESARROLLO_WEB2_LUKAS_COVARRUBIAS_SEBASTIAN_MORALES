package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.MesaSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.repository.ArchivoJsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class MesaServiceTest {
    @TempDir
    Path carpetaTemporal;

    private ArchivoJsonRepository repository;
    private MesaService service;

    @BeforeEach
    void preparar() {
        repository = new ArchivoJsonRepository(JsonMapper.builder().build(),
                carpetaTemporal.resolve("datos.json").toString());
        service = new MesaService(repository);
    }

    @Test
    void crearConsultarYListarMesa() {
        Mesa mesa = service.crear(new MesaSolicitud(4, 6, true));
        assertEquals(1L, mesa.getId());
        assertEquals(4, service.buscar(mesa.getId()).getNumero());
        assertEquals(1, service.listar().size());
    }

    @Test
    void actualizarYEliminarMesaSinReservas() {
        Mesa mesa = service.crear(new MesaSolicitud(4, 6, true));
        Mesa editada = service.actualizar(mesa.getId(), new MesaSolicitud(7, 8, false));
        assertEquals(7, editada.getNumero());
        assertFalse(editada.isActiva());
        service.eliminar(mesa.getId());
        assertTrue(service.listar().isEmpty());
    }

    @Test
    void rechazarNumeroRepetido() {
        service.crear(new MesaSolicitud(4, 6, true));
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(new MesaSolicitud(4, 2, true)));
        assertEquals(1, service.listar().size());
    }

    @Test
    void rechazarCapacidadNoPositiva() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new MesaSolicitud(1, 0, true)));
    }

    @Test
    void noEliminarMesaConReservasHistoricas() {
        Mesa mesa = service.crear(new MesaSolicitud(4, 6, true));
        agregarReserva(mesa.getId(), 2, EstadoReserva.CANCELADA);
        assertThrows(ConflictoNegocioException.class, () -> service.eliminar(mesa.getId()));
    }

    @Test
    void noDesactivarMesaConReservaActiva() {
        Mesa mesa = service.crear(new MesaSolicitud(4, 6, true));
        agregarReserva(mesa.getId(), 3, EstadoReserva.ACTIVA);
        assertThrows(ConflictoNegocioException.class,
                () -> service.actualizar(mesa.getId(), new MesaSolicitud(4, 6, false)));
    }

    @Test
    void noReducirCapacidadBajoReservaActiva() {
        Mesa mesa = service.crear(new MesaSolicitud(4, 6, true));
        agregarReserva(mesa.getId(), 5, EstadoReserva.ACTIVA);
        assertThrows(ConflictoNegocioException.class,
                () -> service.actualizar(mesa.getId(), new MesaSolicitud(4, 4, true)));
    }

    private void agregarReserva(Long mesaId, int personas, EstadoReserva estado) {
        repository.modificar(datos -> {
            datos.getReservas().add(new Reserva(1L, 1L, mesaId,
                    LocalDate.of(2027, 1, 10), LocalTime.of(20, 0), personas, estado));
            return null;
        });
    }
}
