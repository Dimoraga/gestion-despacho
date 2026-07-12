package cl.duoc.transportista.despacho.consumidor.controller;

import cl.duoc.transportista.despacho.consumidor.dto.*;
import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {
  private final GuiaRegistroPersistenceService registros;
  private final GuiaPdfService pdf;
  private final EfsStorageService efs;
  private final S3StorageService s3;

  GuiaController(
      GuiaRegistroPersistenceService registros,
      GuiaPdfService pdf,
      EfsStorageService efs,
      S3StorageService s3) {
    this.registros = registros;
    this.pdf = pdf;
    this.efs = efs;
    this.s3 = s3;
  }

  @GetMapping
  public List<GuiaResponse> historial(
      @RequestParam(required = false) String transportista,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return registros.historial(transportista, fecha).stream().map(this::response).toList();
  }

  @GetMapping("/{id}")
  public ResponseEntity<GuiaResponse> obtener(@PathVariable Long id) {
    GuiaDespachoRegistro r = registros.obtener(id);
    return pendiente(r)
        ? ResponseEntity.accepted().body(response(r))
        : ResponseEntity.ok(response(r));
  }

  @PutMapping("/{id}")
  public GuiaResponse actualizar(@PathVariable Long id, @Valid @RequestBody GuiaRequest request) {
    GuiaDespachoRegistro r = registros.actualizar(id, request);
    almacenar(r);
    return response(r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    GuiaDespachoRegistro r = registros.eliminar(id);
    s3.borrar(r.getArchivoKey());
    efs.borrar(r.getArchivoKey());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/s3")
  public ResponseEntity<GuiaResponse> subir(@PathVariable Long id) {
    GuiaDespachoRegistro r = registros.obtener(id);
    if (pendiente(r)) return ResponseEntity.accepted().body(response(r));
    almacenar(r);
    return ResponseEntity.ok(response(r));
  }

  @GetMapping("/{id}/s3")
  public ResponseEntity<byte[]> descargar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    GuiaDespachoRegistro r = registros.obtener(id);
    if (pendiente(r)) return ResponseEntity.accepted().build();
    if (jwt == null)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Identidad de transportista requerida");
    String user = jwt.getClaimAsString("preferred_username");
    if (user == null || user.isBlank()) user = jwt.getClaimAsString("name");
    if (user == null || user.isBlank())
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Identidad de transportista requerida");
    String transportista;
    try {
      transportista = registros.normalizarTransportista(user);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transportista no autorizado");
    }
    if (!r.getTransportista().equalsIgnoreCase(transportista))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transportista no autorizado");
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia" + id + ".pdf")
        .body(s3.descargar(r.getArchivoKey()));
  }

  private boolean pendiente(GuiaDespachoRegistro r) {
    return r.getEstado() == EstadoProcesamiento.PENDING
        || r.getEstado() == EstadoProcesamiento.PROCESSING;
  }

  private void almacenar(GuiaDespachoRegistro r) {
    byte[] bytes = pdf.generar(r);
    var path = efs.guardar(r.getArchivoKey(), bytes);
    s3.subir(r.getArchivoKey(), bytes);
    registros.completar(r.getId(), path.toString());
  }

  private GuiaResponse response(GuiaDespachoRegistro r) {
    return new GuiaResponse(
        r.getNumeroGuia(),
        r.getTransportista(),
        r.getFecha(),
        r.getDestino(),
        r.getPedido(),
        r.getArchivoKey(),
        r.getEstado().name());
  }
}
