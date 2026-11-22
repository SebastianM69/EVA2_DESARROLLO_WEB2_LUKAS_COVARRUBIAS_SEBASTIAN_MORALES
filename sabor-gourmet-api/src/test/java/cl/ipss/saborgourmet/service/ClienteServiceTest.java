package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.ClienteSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.exception.RecursoNoEncontradoException;
import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.EstadoReserva;
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

class ClienteServiceTest {
    @TempDir
    Path carpetaTemporal;

    private ArchivoJsonRepository repository;
    private ClienteService service;

    @BeforeEach
    void preparar() {
        repository = new ArchivoJsonRepository(JsonMapper.builder().build(),
                carpetaTemporal.resolve("datos.json").toString());
        service = new ClienteService(repository);
    }

    private ClienteSolicitud solicitud(String email) {
        return new ClienteSolicitud("Ana", "Perez", email, "+56912345678");
    }

    @Test
    void crearYListarCliente() {
        Cliente cliente = service.crear(solicitud("ana@example.com"));
        assertEquals(1L, cliente.getId());
        assertEquals(1, service.listar().size());
        assertEquals("Ana", service.buscar(cliente.getId()).getNombre());
    }

    @Test
    void actualizarYEliminarCliente() {
        Cliente cliente = service.crear(solicitud("ana@example.com"));
        Cliente actualizado = service.actualizar(cliente.getId(),
                new ClienteSolicitud("Andrea", "Perez", "andrea@example.com", "12345"));
        assertEquals("Andrea", actualizado.getNombre());
        service.eliminar(cliente.getId());
        assertTrue(service.listar().isEmpty());
    }

    @Test
    void rechazarEmailDuplicadoSinModificarJson() {
        service.crear(solicitud("ana@example.com"));
        assertThrows(ConflictoNegocioException.class,
                () -> service.crear(solicitud("ANA@EXAMPLE.COM")));
        assertEquals(1, repository.leer().getClientes().size());
    }

    @Test
    void rechazarDatosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(solicitud("correo-invalido")));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new ClienteSolicitud(" ", "Perez", "a@b.cl", "123")));
    }

    @Test
    void clienteInexistenteDevuelveError() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.buscar(55L));
    }

    @Test
    void prohibirEliminarClienteConReservaInclusoCancelada() {
        Cliente cliente = service.crear(solicitud("ana@example.com"));
        repository.modificar(datos -> {
            datos.getReservas().add(new Reserva(1L, cliente.getId(), 1L,
                    LocalDate.of(2027, 1, 10), LocalTime.of(20, 0), 2,
                    EstadoReserva.CANCELADA));
            return null;
        });
        assertThrows(ConflictoNegocioException.class, () -> service.eliminar(cliente.getId()));
        assertEquals(1, service.listar().size());
    }
}
