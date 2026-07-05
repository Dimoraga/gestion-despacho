package cl.duoc.transportista.despacho.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.dto.MqResultadoResponse;
import cl.duoc.transportista.despacho.model.GuiaDespachoResumenMq;
import cl.duoc.transportista.despacho.service.GuiaDespachoService;
import cl.duoc.transportista.despacho.service.GuiaDespachoMqService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaDespachoService service;
    private final GuiaDespachoMqService mqService;

    public GuiaController(GuiaDespachoService service, ObjectProvider<GuiaDespachoMqService> mqService) {
        this.service = service;
        this.mqService = mqService.getIfAvailable();
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

    @PostMapping("/{id}/mq")
    public ResponseEntity<MqResultadoResponse> enviarARabbit(@PathVariable Long id) {
        return ResponseEntity.ok(mq().enviarGuiaARabbit(id));
    }

    @PostMapping("/mq/consumir")
    public ResponseEntity<GuiaDespachoResumenMq> consumirDesdeRabbit() {
        GuiaDespachoResumenMq resumen = mq().consumirGuiaDesdeRabbit();
        if (resumen == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/mq/resumenes")
    public ResponseEntity<List<GuiaDespachoResumenMq>> resumenesMq() {
        return ResponseEntity.ok(mq().listarResumenesMq());
    }

    @GetMapping("/{id}/s3")
    public ResponseEntity<byte[]> descargarDeS3(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String solicitante = jwt.getClaimAsString("preferred_username");
        if (solicitante == null || solicitante.isBlank()) {
            solicitante = jwt.getClaimAsString("name");
        }
        if (solicitante == null || solicitante.isBlank()) {
            solicitante = service.obtener(id).transportista();
        }

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

    private GuiaDespachoMqService mq() {
        if (mqService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no disponible");
        }
        return mqService;
    }
}
