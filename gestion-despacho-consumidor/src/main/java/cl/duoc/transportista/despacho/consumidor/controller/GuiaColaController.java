package cl.duoc.transportista.despacho.consumidor.controller;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.service.GuiaColaConsumerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guias/cola")
public class GuiaColaController {

    private final GuiaColaConsumerService service;

    public GuiaColaController(GuiaColaConsumerService service) {
        this.service = service;
    }

    @PostMapping("/procesar")
    public ResponseEntity<List<GuiaDespachoRegistroResponse>> procesar() {
        return ResponseEntity.ok(service.consumirColaGuias());
    }
}
