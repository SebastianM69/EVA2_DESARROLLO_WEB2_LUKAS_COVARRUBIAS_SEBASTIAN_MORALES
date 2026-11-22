package cl.ipss.saborgourmet.service;

import cl.ipss.saborgourmet.dto.ClienteSolicitud;
import cl.ipss.saborgourmet.exception.ConflictoNegocioException;
import cl.ipss.saborgourmet.exception.RecursoNoEncontradoException;
import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.repository.ArchivoJsonRepository;
import cl.ipss.saborgourmet.repository.DatosRestaurante;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ClienteService {
    private final ArchivoJsonRepository repository;

    public ClienteService(ArchivoJsonRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() {
        return repository.leer().getClientes();
    }

    public Cliente buscar(Long id) {
        validarId(id);
        return encontrar(repository.leer(), id);
    }

    public Cliente crear(ClienteSolicitud solicitud) {
        DatosCliente datos = validar(solicitud);
        return repository.modificar(almacen -> {
            verificarEmail(almacen, datos.email, null);
            long nuevoId = almacen.getClientes().stream()
                    .mapToLong(cliente -> cliente.getId()).max().orElse(0L) + 1;
            Cliente cliente = new Cliente(nuevoId, datos.nombre, datos.apellido,
                    datos.email, datos.telefono);
            almacen.getClientes().add(cliente);
            return cliente;
        });
    }

    public Cliente actualizar(Long id, ClienteSolicitud solicitud) {
        validarId(id);
        DatosCliente datos = validar(solicitud);
        return repository.modificar(almacen -> {
            Cliente cliente = encontrar(almacen, id);
            verificarEmail(almacen, datos.email, id);
            cliente.setNombre(datos.nombre);
            cliente.setApellido(datos.apellido);
            cliente.setEmail(datos.email);
            cliente.setTelefono(datos.telefono);
            return cliente;
        });
    }

    public void eliminar(Long id) {
        validarId(id);
        repository.modificar(almacen -> {
            Cliente cliente = encontrar(almacen, id);
            // También se conservan las referencias de reservas canceladas.
            if (almacen.getReservas().stream().anyMatch(r -> id.equals(r.getClienteId()))) {
                throw new ConflictoNegocioException(
                        "No se puede eliminar un cliente con reservas asociadas");
            }
            almacen.getClientes().remove(cliente);
            return null;
        });
    }

    private Cliente encontrar(DatosRestaurante almacen, Long id) {
        return almacen.getClientes().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el cliente con ID " + id));
    }

    private void verificarEmail(DatosRestaurante almacen, String email, Long exceptoId) {
        boolean repetido = almacen.getClientes().stream()
                .anyMatch(c -> c.getEmail() != null
                        && c.getEmail().equalsIgnoreCase(email)
                        && (exceptoId == null || !exceptoId.equals(c.getId())));
        if (repetido) {
            throw new ConflictoNegocioException("Ya existe un cliente con ese email");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }

    private DatosCliente validar(ClienteSolicitud solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Los datos del cliente son obligatorios");
        }
        String nombre = texto(solicitud.getNombre(), "nombre", 100);
        String apellido = texto(solicitud.getApellido(), "apellido", 100);
        // La comparación de correos no distingue mayúsculas de minúsculas.
        String email = texto(solicitud.getEmail(), "email", 150).toLowerCase(Locale.ROOT);
        String telefono = texto(solicitud.getTelefono(), "telefono", 30);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El email no tiene formato valido");
        }
        return new DatosCliente(nombre, apellido, email, telefono);
    }

    private String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " supera " + maximo + " caracteres");
        }
        return limpio;
    }

    private static class DatosCliente {
        final String nombre;
        final String apellido;
        final String email;
        final String telefono;

        DatosCliente(String nombre, String apellido, String email, String telefono) {
            this.nombre = nombre;
            this.apellido = apellido;
            this.email = email;
            this.telefono = telefono;
        }
    }
}
