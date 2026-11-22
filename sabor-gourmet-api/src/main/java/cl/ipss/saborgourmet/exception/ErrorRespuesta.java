package cl.ipss.saborgourmet.exception;

import java.time.OffsetDateTime;

public class ErrorRespuesta {
    private String fechaHora;
    private int estado;
    private String error;
    private String mensaje;
    private String ruta;

    public ErrorRespuesta() { }

    public ErrorRespuesta(int estado, String error, String mensaje, String ruta) {
        this.fechaHora = OffsetDateTime.now().toString();
        this.estado = estado;
        this.error = error;
        this.mensaje = mensaje;
        this.ruta = ruta;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }
}
