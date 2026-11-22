package cl.ipss.saborgourmet.repository;

import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;

import java.util.ArrayList;
import java.util.List;

/** Agrupa las tres colecciones que se guardan en datos.json. */
public class DatosRestaurante {
    private List<Cliente> clientes = new ArrayList<>();
    private List<Mesa> mesas = new ArrayList<>();
    private List<Reserva> reservas = new ArrayList<>();

    public DatosRestaurante() {
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(List<Cliente> clientes) {
        this.clientes = clientes;
    }

    public List<Mesa> getMesas() {
        return mesas;
    }

    public void setMesas(List<Mesa> mesas) {
        this.mesas = mesas;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }
}
