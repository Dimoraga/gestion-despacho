package cl.duoc.transportista.despacho.consumidor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GuiaRequest(
    @NotBlank String transportista,
    @NotNull LocalDate fecha,
    @NotBlank String destino,
    @NotBlank String pedido) {}
