package cl.duoc.transportista.despacho.consumidor.dto;

import java.time.LocalDate;

public record GuiaColaMensaje(
    Integer version,
    String requestId,
    String fingerprint,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido) {}
