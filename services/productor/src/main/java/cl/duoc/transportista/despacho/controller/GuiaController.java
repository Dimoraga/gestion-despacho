package cl.duoc.transportista.despacho.controller;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import jakarta.validation.Valid;
import java.net.URI;
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
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody GuiaRequest req) {
    if (idempotencyKey != null) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Idempotency-Key no está soportado");
    }
    GuiaResponse r = service.crear(req);
    return ResponseEntity.accepted()
        .location(URI.create("/api/solicitudes/" + r.requestId()))
        .body(r);
  }

}
