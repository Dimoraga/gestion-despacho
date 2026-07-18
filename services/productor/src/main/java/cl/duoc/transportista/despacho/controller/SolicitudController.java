package cl.duoc.transportista.despacho.controller;

import cl.duoc.transportista.despacho.dto.SolicitudResponse;
import cl.duoc.transportista.despacho.service.ConsumerUnavailableException;
import cl.duoc.transportista.despacho.service.SolicitudConsumerClient;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
  private final SolicitudConsumerClient solicitudConsumerClient;

  public SolicitudController(SolicitudConsumerClient solicitudConsumerClient) {
    this.solicitudConsumerClient = solicitudConsumerClient;
  }

  @GetMapping("/{requestId}")
  public ResponseEntity<SolicitudResponse> consultar(@PathVariable UUID requestId) {
    try {
      return solicitudConsumerClient
          .consultar(requestId.toString())
          .map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.notFound().header("Retry-After", "1").build());
    } catch (ConsumerUnavailableException ex) {
      return ResponseEntity.status(503).header("Retry-After", "1").build();
    }
  }
}
