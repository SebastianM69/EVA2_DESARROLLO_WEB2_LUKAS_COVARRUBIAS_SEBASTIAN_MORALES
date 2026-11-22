package cl.ipss.saborgourmet.model;

public class Mesa {

    private Long id;
    private Integer numero;
    private Integer capacidad;
    private boolean activa;

    public Mesa() {
    }

    public Mesa(Long id, Integer numero, Integer capacidad, boolean activa) {
        this.id = id;
        this.numero = numero;
        this.capacidad = capacidad;
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
