package cl.ipss.saborgourmet.controller;

import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class DisponibilidadController {

    private final ReservaService service;

    public DisponibilidadController(ReservaService service) {
        this.service = service;
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Mesa>> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam Integer personas) {
        return ResponseEntity.ok(service.mesasDisponibles(fecha, hora, personas));
    }
}
