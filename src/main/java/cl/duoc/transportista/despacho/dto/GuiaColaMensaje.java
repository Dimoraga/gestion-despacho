package cl.duoc.transportista.despacho.dto;

import java.time.LocalDate;

public record GuiaColaMensaje(
    Long numeroGuia,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido,
    String archivoKey) {}
