package cl.ipss.saborgourmet.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Las referencias al cliente y a la mesa se almacenan por ID. */
public class Reserva {

    private Long id;
    private Long clienteId;
    private Long mesaId;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer cantidadPersonas;
    private EstadoReserva estado;

    public Reserva() {
    }

    public Reserva(Long id, Long clienteId, Long mesaId, LocalDate fecha,
                   LocalTime hora, Integer cantidadPersonas, EstadoReserva estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.mesaId = mesaId;
        this.fecha = fecha;
        this.hora = hora;
        this.cantidadPersonas = cantidadPersonas;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public Integer getCantidadPersonas() {
        return cantidadPersonas;
    }

    public void setCantidadPersonas(Integer cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }
}
