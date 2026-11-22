package cl.ipss.saborgourmet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/** El cliente no envía el ID ni el estado: ambos se controlan desde el servidor. */
public class ReservaSolicitud {

    @NotNull(message = "El cliente es obligatorio")
    @Positive(message = "El ID de cliente debe ser positivo")
    private Long clienteId;

    @NotNull(message = "La mesa es obligatoria")
    @Positive(message = "El ID de mesa debe ser positivo")
    private Long mesaId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotNull(message = "La cantidad de personas es obligatoria")
    @Positive(message = "La cantidad de personas debe ser positiva")
    private Integer cantidadPersonas;

    public ReservaSolicitud() {
    }

    public ReservaSolicitud(Long clienteId, Long mesaId, LocalDate fecha,
                            LocalTime hora, Integer cantidadPersonas) {
        this.clienteId = clienteId;
        this.mesaId = mesaId;
        this.fecha = fecha;
        this.hora = hora;
        this.cantidadPersonas = cantidadPersonas;
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
}
