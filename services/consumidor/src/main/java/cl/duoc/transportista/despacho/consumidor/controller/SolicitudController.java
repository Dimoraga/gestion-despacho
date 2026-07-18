package cl.duoc.transportista.despacho.consumidor.controller;

import cl.duoc.transportista.despacho.consumidor.dto.SolicitudResponse;
import cl.duoc.transportista.despacho.consumidor.service.GuiaRegistroPersistenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
  private final GuiaRegistroPersistenceService registros;

  SolicitudController(GuiaRegistroPersistenceService registros) {
    this.registros = registros;
  }

  @GetMapping("/{requestId}")
  public SolicitudResponse obtener(@PathVariable String requestId) {
    return registros.consultarSolicitud(requestId);
  }
}
