package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.ReservaSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.exception.RecursoNoEncontradoException;
import cl.ipss.saborgourmet.model.Cliente;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2027, 1, 10);
    private static final LocalTime HORA = LocalTime.of(20, 0);

    @TempDir
    Path carpeta;

    private ArchivoJsonRepository repository;
    private ReservaService service;

    @BeforeEach
    void preparar() {
        repository = new ArchivoJsonRepository(JsonMapper.builder().build(),
                carpeta.resolve("datos.json").toString());
        repository.modificar(datos -> {
            datos.getClientes().add(new Cliente(1L, "Ana", "Perez",
                    "ana@example.com", "+56911111111"));
            datos.getClientes().add(new Cliente(2L, "Luis", "Diaz",
                    "luis@example.com", "+56922222222"));
            datos.getMesas().add(new Mesa(1L, 10, 4, true));
            datos.getMesas().add(new Mesa(2L, 20, 2, true));
            datos.getMesas().add(new Mesa(3L, 30, 8, false));
            return null;
        });
        service = new ReservaService(repository);
    }

    private ReservaSolicitud solicitud(long cliente, long mesa, LocalDate fecha,
                                      LocalTime hora, int personas) {
        return new ReservaSolicitud(cliente, mesa, fecha, hora, personas);
    }

    private ReservaSolicitud habitual() {
        return solicitud(1L, 1L, FECHA, HORA, 3);
    }

    @Test
    void crearReservaActivaAsignaIdYConservaRelaciones() {
        Reserva creada = service.crear(habitual());
        assertEquals(1L, creada.getId());
        assertEquals(1L, creada.getClienteId());
        assertEquals(1L, creada.getMesaId());
        assertEquals(EstadoReserva.ACTIVA, creada.getEstado());
        assertEquals(3, creada.getCantidadPersonas());
    }

    @Test
    void listarYConsultarPorId() {
        Reserva creada = service.crear(habitual());
        assertEquals(1, service.listar().size());
        assertEquals(creada.getId(), service.buscar(creada.getId()).getId());
    }

    @Test
    void actualizarReservaActiva() {
        Reserva creada = service.crear(habitual());
        Reserva cambiada = service.actualizar(creada.getId(),
                solicitud(2L, 2L, FECHA.plusDays(1), LocalTime.of(18, 30), 2));
        assertEquals(creada.getId(), cambiada.getId());
        assertEquals(2L, cambiada.getClienteId());
        assertEquals(2L, cambiada.getMesaId());
        assertEquals(EstadoReserva.ACTIVA, cambiada.getEstado());
    }

    @Test
    void actualizarMismaReservaNoGeneraConflictoConsigoMisma() {
        Reserva creada = service.crear(habitual());
        assertDoesNotThrow(() -> service.actualizar(creada.getId(), habitual()));
    }

    @Test
    void solapamientoExactoEsConflicto() {
        service.crear(habitual());
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud(2L, 1L, FECHA, HORA, 2)));
        assertEquals(1, service.listar().size());
    }

    @Test
    void solapamientoParcialEsConflicto() {
        service.crear(habitual());
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud(2L, 1L, FECHA,
                        LocalTime.of(21, 30), 2)));
    }

    @Test
    void turnosContiguosSonPermitidos() {
        service.crear(habitual());
        assertDoesNotThrow(() -> service.crear(
                solicitud(2L, 1L, FECHA, LocalTime.of(22, 0), 2)));
        assertEquals(2, service.listar().size());
    }

    @Test
    void distintasMesasPuedenReservarseEnMismoHorario() {
        service.crear(habitual());
        assertDoesNotThrow(() -> service.crear(
                solicitud(2L, 2L, FECHA, HORA, 2)));
    }

    @Test
    void rechazarMesaInactiva() {
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud(1L, 3L, FECHA, HORA, 2)));
    }

    @Test
    void rechazarExcesoDeCapacidad() {
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud(1L, 2L, FECHA, HORA, 3)));
    }

    @Test
    void rechazarClienteInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(solicitud(999L, 1L, FECHA, HORA, 2)));
    }

    @Test
    void rechazarMesaInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(solicitud(1L, 999L, FECHA, HORA, 2)));
    }

    @Test
    void rechazarSolicitudIncompleta() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new ReservaSolicitud(1L, 1L, null, HORA, 2)));
    }

    @Test
    void rechazarCantidadNoPositiva() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(solicitud(1L, 1L, FECHA, HORA, 0)));
    }

    @Test
    void cancelarLiberaLaMesaYConservaElHistorial() {
        Reserva creada = service.crear(habitual());
        Reserva cancelada = service.cancelar(creada.getId());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(EstadoReserva.CANCELADA, service.buscar(creada.getId()).getEstado());
        assertDoesNotThrow(() -> service.crear(
                solicitud(2L, 1L, FECHA, HORA, 2)));
        assertEquals(2, service.listar().size());
    }

    @Test
    void rechazarSegundaCancelacion() {
        Reserva creada = service.crear(habitual());
        service.cancelar(creada.getId());
        assertThrows(ConflictoNegocioException.class,
                () -> service.cancelar(creada.getId()));
    }

    @Test
    void impedirModificarReservaCancelada() {
        Reserva creada = service.crear(habitual());
        service.cancelar(creada.getId());
        assertThrows(ConflictoNegocioException.class,
                () -> service.actualizar(creada.getId(), habitual()));
    }

    @Test
    void mesasDisponiblesConsideraCapacidadEstadoYReservas() {
        List<Mesa> antes = service.mesasDisponibles(FECHA, HORA, 2);
        assertEquals(2, antes.size());
        service.crear(habitual());
        List<Mesa> despues = service.mesasDisponibles(FECHA, HORA, 2);
        assertEquals(1, despues.size());
        assertEquals(2L, despues.get(0).getId());
    }

    @Test
    void consultarDisponibilidadConPersonasInvalidasDaError() {
        assertThrows(IllegalArgumentException.class,
                () -> service.mesasDisponibles(FECHA, HORA, 0));
    }

    @Test
    void turnosQueCruzanMedianocheSeControlan() {
        Reserva creada = service.crear(solicitud(1L, 1L, FECHA,
                LocalTime.of(23, 30), 2));
        assertEquals(EstadoReserva.ACTIVA, creada.getEstado());
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud(2L, 1L, FECHA.plusDays(1),
                        LocalTime.of(0, 30), 2)));
        assertDoesNotThrow(() -> service.crear(solicitud(2L, 1L, FECHA.plusDays(1),
                LocalTime.of(1, 30), 2)));
    }

    @Test
    void actualizarDetectaCruceConOtraReserva() {
        service.crear(habitual());
        Reserva segunda = service.crear(solicitud(2L, 1L, FECHA,
                LocalTime.of(22, 0), 2));
        assertThrows(ConflictoNegocioException.class,
                () -> service.actualizar(segunda.getId(), solicitud(2L, 1L, FECHA,
                        LocalTime.of(21, 0), 2)));
        assertEquals(LocalTime.of(22, 0), service.buscar(segunda.getId()).getHora());
    }

    @Test
    void cancelacionPersisteDespuesDeReconstruirRepository() {
        Reserva creada = service.crear(habitual());
        service.cancelar(creada.getId());
        ArchivoJsonRepository reiniciado = new ArchivoJsonRepository(
                JsonMapper.builder().build(), carpeta.resolve("datos.json").toString());
        ReservaService nuevoService = new ReservaService(reiniciado);
        assertEquals(EstadoReserva.CANCELADA,
                nuevoService.buscar(creada.getId()).getEstado());
        assertEquals(2, nuevoService.mesasDisponibles(FECHA, HORA, 2).size());
    }

    @Test
    void buscarReservaInexistenteDa404EnCapaHttp() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.buscar(999L));
    }
}
