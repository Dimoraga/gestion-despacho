package cl.duoc.transportista.despacho.controller;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaDespachoService service;

    public GuiaController(GuiaDespachoService service) {
        this.service = service;
    }

    @PostMapping("")
    public ResponseEntity<GuiaResponse> crear(@Valid @RequestBody GuiaRequest req) {
        GuiaResponse r = service.crear(req);
        return ResponseEntity.created(URI.create("/api/guias/" + r.numeroGuia())).body(r);
    }

    @PostMapping("/{id}/s3")
    public ResponseEntity<GuiaResponse> subirAS3(@PathVariable Long id) {
        service.subirAS3(id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("")
    public ResponseEntity<List<GuiaResponse>> historial(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(service.historial(transportista, fecha));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/{id}/s3")
    public ResponseEntity<byte[]> descargarDeS3(
            @PathVariable Long id,
            @RequestHeader("X-Transportista") String solicitante) {
        byte[] pdf = service.descargarDeS3(id, solicitante);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia" + id + ".pdf")
                .body(pdf);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuiaRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
