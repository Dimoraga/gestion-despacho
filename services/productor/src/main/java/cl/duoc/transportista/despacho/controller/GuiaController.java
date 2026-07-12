package cl.duoc.transportista.despacho.controller;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {
  private final GuiaDespachoService service;

  public GuiaController(GuiaDespachoService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<GuiaResponse> crear(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody GuiaRequest req) {
    long numeroGuia;
    try {
      numeroGuia = Long.parseLong(idempotencyKey);
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Idempotency-Key debe ser un entero positivo");
    }
    if (numeroGuia <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Idempotency-Key debe ser un entero positivo");
    }
    GuiaResponse r = service.crear(req, numeroGuia);
    return ResponseEntity.accepted().location(URI.create("/api/guias/" + r.numeroGuia())).body(r);
  }
}
