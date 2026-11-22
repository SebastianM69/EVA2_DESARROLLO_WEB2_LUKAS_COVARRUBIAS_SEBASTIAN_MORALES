package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.MesaSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.exception.RecursoNoEncontradoException;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.repository.ArchivoJsonRepository;
import cl.ipss.saborgourmet.repository.DatosRestaurante;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {
    private final ArchivoJsonRepository repository;

    public MesaService(ArchivoJsonRepository repository) {
        this.repository = repository;
    }

    public List<Mesa> listar() {
        return repository.leer().getMesas();
    }

    public Mesa buscar(Long id) {
        validarId(id);
        return encontrar(repository.leer(), id);
    }

    public Mesa crear(MesaSolicitud solicitud) {
        validar(solicitud);
        return repository.modificar(almacen -> {
            verificarNumero(almacen, solicitud.getNumero(), null);
            long nuevoId = almacen.getMesas().stream()
                    .mapToLong(mesa -> mesa.getId()).max().orElse(0L) + 1;
            Mesa mesa = new Mesa(nuevoId, solicitud.getNumero(),
                    solicitud.getCapacidad(), solicitud.getActiva());
            almacen.getMesas().add(mesa);
            return mesa;
        });
    }

    public Mesa actualizar(Long id, MesaSolicitud solicitud) {
        validarId(id);
        validar(solicitud);
        return repository.modificar(almacen -> {
            Mesa mesa = encontrar(almacen, id);
            verificarNumero(almacen, solicitud.getNumero(), id);
            boolean tieneReservasActivas = almacen.getReservas().stream()
                    .anyMatch(r -> id.equals(r.getMesaId())
                            && r.getEstado() == EstadoReserva.ACTIVA);
            if (tieneReservasActivas && !solicitud.getActiva()) {
                throw new ConflictoNegocioException(
                        "No se puede desactivar una mesa con reservas activas");
            }
            // No se reduce la capacidad por debajo de una reserva activa.
            boolean excedeCapacidad = almacen.getReservas().stream()
                    .anyMatch(r -> id.equals(r.getMesaId())
                            && r.getEstado() == EstadoReserva.ACTIVA
                            && r.getCantidadPersonas() != null
                            && r.getCantidadPersonas() > solicitud.getCapacidad());
            if (excedeCapacidad) {
                throw new ConflictoNegocioException(
                        "La capacidad no permite atender las reservas activas existentes");
            }
            mesa.setNumero(solicitud.getNumero());
            mesa.setCapacidad(solicitud.getCapacidad());
            mesa.setActiva(solicitud.getActiva());
            return mesa;
        });
    }

    public void eliminar(Long id) {
        validarId(id);
        repository.modificar(almacen -> {
            Mesa mesa = encontrar(almacen, id);
            // El historial necesita mantener la referencia a la mesa.
            if (almacen.getReservas().stream().anyMatch(r -> id.equals(r.getMesaId()))) {
                throw new ConflictoNegocioException(
                        "No se puede eliminar una mesa con reservas asociadas");
            }
            almacen.getMesas().remove(mesa);
            return null;
        });
    }

    private Mesa encontrar(DatosRestaurante almacen, Long id) {
        return almacen.getMesas().stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la mesa con ID " + id));
    }

    private void verificarNumero(DatosRestaurante almacen, Integer numero, Long exceptoId) {
        boolean repetido = almacen.getMesas().stream()
                .anyMatch(m -> numero.equals(m.getNumero())
                        && (exceptoId == null || !exceptoId.equals(m.getId())));
        if (repetido) {
            throw new ConflictoNegocioException("Ya existe una mesa con ese numero");
        }
    }

    private void validar(MesaSolicitud solicitud) {
        if (solicitud == null || solicitud.getNumero() == null
                || solicitud.getCapacidad() == null || solicitud.getActiva() == null) {
            throw new IllegalArgumentException("Numero, capacidad y estado son obligatorios");
        }
        if (solicitud.getNumero() < 1 || solicitud.getCapacidad() < 1) {
            throw new IllegalArgumentException("Numero y capacidad deben ser positivos");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
