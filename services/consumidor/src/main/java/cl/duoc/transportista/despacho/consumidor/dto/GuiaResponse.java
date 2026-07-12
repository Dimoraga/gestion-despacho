package cl.duoc.transportista.despacho.consumidor.dto;

import java.time.LocalDate;

public record GuiaResponse(
    Long numeroGuia,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido,
    String archivoKey,
    String estado) {}
