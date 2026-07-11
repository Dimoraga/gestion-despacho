package cl.duoc.transportista.despacho.consumidor.dto;

import java.time.Instant;
import java.time.LocalDate;

public record GuiaDespachoRegistroResponse(
    Long id,
    Long numeroGuia,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido,
    String archivoKey,
    Instant fechaProcesado) {}
