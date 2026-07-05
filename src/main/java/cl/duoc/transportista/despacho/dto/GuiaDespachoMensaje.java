package cl.duoc.transportista.despacho.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GuiaDespachoMensaje(
        Long numeroGuia,
        String transportista,
        LocalDate fecha,
        String destino,
        String pedido,
        String archivoKey,
        LocalDateTime fechaMensaje
) {
}
