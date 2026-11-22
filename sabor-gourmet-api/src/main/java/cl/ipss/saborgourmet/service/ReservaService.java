package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.ReservaSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.exception.RecursoNoEncontradoException;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.repository.ArchivoJsonRepository;
import cl.ipss.saborgourmet.repository.DatosRestaurante;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Cada reserva ocupa la mesa durante dos horas. Solo las reservas activas
 * bloquean horarios y una cancelación no elimina el registro.
 */
@Service
public class ReservaService {

    public static final int DURACION_HORAS = 2;

    private final ArchivoJsonRepository repository;

    public ReservaService(ArchivoJsonRepository repository) {
        this.repository = repository;
    }

    public List<Reserva> listar() {
        return repository.leer().getReservas();
    }

    public Reserva buscar(Long id) {
        validarId(id);
        return encontrarReserva(repository.leer(), id);
    }

    public Reserva crear(ReservaSolicitud solicitud) {
        validarSolicitud(solicitud);
        return repository.modificar(datos -> {
            validarClienteYCapacidad(datos, solicitud);
            validarSolapamiento(datos, solicitud.getMesaId(), solicitud.getFecha(),
                    solicitud.getHora(), null);

            long siguienteId = datos.getReservas().stream()
                    .mapToLong(Reserva::getId).max().orElse(0L) + 1;

            Reserva reserva = new Reserva(siguienteId, solicitud.getClienteId(),
                    solicitud.getMesaId(), solicitud.getFecha(), solicitud.getHora(),
                    solicitud.getCantidadPersonas(), EstadoReserva.ACTIVA);
            datos.getReservas().add(reserva);
            return reserva;
        });
    }

    public Reserva actualizar(Long id, ReservaSolicitud solicitud) {
        validarId(id);
        validarSolicitud(solicitud);
        return repository.modificar(datos -> {
            Reserva reserva = encontrarReserva(datos, id);
            if (reserva.getEstado() != EstadoReserva.ACTIVA) {
                throw new ConflictoNegocioException(
                        "No se puede modificar una reserva cancelada");
            }
            validarClienteYCapacidad(datos, solicitud);
            // Se excluye la misma reserva al comprobar conflictos durante una edición.
            validarSolapamiento(datos, solicitud.getMesaId(), solicitud.getFecha(),
                    solicitud.getHora(), id);

            reserva.setClienteId(solicitud.getClienteId());
            reserva.setMesaId(solicitud.getMesaId());
            reserva.setFecha(solicitud.getFecha());
            reserva.setHora(solicitud.getHora());
            reserva.setCantidadPersonas(solicitud.getCantidadPersonas());
            return reserva;
        });
    }

    public Reserva cancelar(Long id) {
        validarId(id);
        return repository.modificar(datos -> {
            Reserva reserva = encontrarReserva(datos, id);
            if (reserva.getEstado() == EstadoReserva.CANCELADA) {
                throw new ConflictoNegocioException("La reserva ya esta cancelada");
            }
            reserva.setEstado(EstadoReserva.CANCELADA);
            return reserva;
        });
    }

    public List<Mesa> mesasDisponibles(LocalDate fecha, LocalTime hora,
                                        Integer personas) {
        if (fecha == null || hora == null || personas == null || personas <= 0) {
            throw new IllegalArgumentException(
                    "Fecha, hora y cantidad positiva de personas son obligatorias");
        }
        DatosRestaurante datos = repository.leer();
        return datos.getMesas().stream()
                .filter(Mesa::isActiva)
                .filter(mesa -> mesa.getCapacidad() != null
                        && mesa.getCapacidad() >= personas)
                .filter(mesa -> !existeSolapamiento(datos, mesa.getId(), fecha,
                        hora, null))
                .toList();
    }

    private void validarClienteYCapacidad(DatosRestaurante datos,
                                           ReservaSolicitud solicitud) {
        boolean clienteExiste = datos.getClientes().stream()
                .anyMatch(c -> solicitud.getClienteId().equals(c.getId()));
        if (!clienteExiste) {
            throw new RecursoNoEncontradoException(
                    "No existe el cliente con ID " + solicitud.getClienteId());
        }

        Mesa mesa = datos.getMesas().stream()
                .filter(m -> solicitud.getMesaId().equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la mesa con ID " + solicitud.getMesaId()));

        if (!mesa.isActiva()) {
            throw new ConflictoNegocioException("La mesa esta inactiva");
        }
        if (mesa.getCapacidad() == null
                || solicitud.getCantidadPersonas() > mesa.getCapacidad()) {
            throw new ConflictoNegocioException(
                    "La cantidad de personas supera la capacidad de la mesa");
        }
    }

    private void validarSolapamiento(DatosRestaurante datos, Long mesaId,
                                      LocalDate fecha, LocalTime hora, Long exceptoId) {
        if (existeSolapamiento(datos, mesaId, fecha, hora, exceptoId)) {
            throw new ConflictoNegocioException(
                    "La mesa ya tiene una reserva activa en ese horario");
        }
    }

    /**
     * Los turnos se comparan con fecha y hora para cubrir también la medianoche.
     * Si uno empieza justo al terminar otro, ambos son válidos.
     */
    private boolean existeSolapamiento(DatosRestaurante datos, Long mesaId,
                                        LocalDate fecha, LocalTime hora, Long exceptoId) {
        LocalDateTime inicioNuevo = fecha.atTime(hora);
        LocalDateTime finNuevo = inicioNuevo.plusHours(DURACION_HORAS);

        return datos.getReservas().stream()
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
                .filter(r -> mesaId.equals(r.getMesaId()))
                .filter(r -> exceptoId == null || !exceptoId.equals(r.getId()))
                .anyMatch(r -> {
                    LocalDateTime inicioExistente = r.getFecha().atTime(r.getHora());
                    LocalDateTime finExistente = inicioExistente.plusHours(DURACION_HORAS);
                    return inicioNuevo.isBefore(finExistente)
                            && inicioExistente.isBefore(finNuevo);
                });
    }

    private Reserva encontrarReserva(DatosRestaurante datos, Long id) {
        return datos.getReservas().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la reserva con ID " + id));
    }

    private void validarSolicitud(ReservaSolicitud solicitud) {
        if (solicitud == null || solicitud.getClienteId() == null
                || solicitud.getMesaId() == null || solicitud.getFecha() == null
                || solicitud.getHora() == null || solicitud.getCantidadPersonas() == null) {
            throw new IllegalArgumentException("Todos los datos de la reserva son obligatorios");
        }
        if (solicitud.getClienteId() <= 0 || solicitud.getMesaId() <= 0
                || solicitud.getCantidadPersonas() <= 0) {
            throw new IllegalArgumentException(
                    "Cliente, mesa y cantidad de personas deben ser positivos");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
