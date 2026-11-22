package cl.ipss.saborgourmet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MesaSolicitud {
    @NotNull(message = "El numero de mesa es obligatorio")
    @Min(value = 1, message = "El numero de mesa debe ser positivo")
    private Integer numero;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser positiva")
    private Integer capacidad;

    @NotNull(message = "Se debe indicar si la mesa esta activa")
    private Boolean activa;

    public MesaSolicitud() { }

    public MesaSolicitud(Integer numero, Integer capacidad, Boolean activa) {
        this.numero = numero;
        this.capacidad = capacidad;
        this.activa = activa;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}
