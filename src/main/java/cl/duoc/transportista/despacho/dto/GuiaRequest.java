package cl.duoc.transportista.despacho.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GuiaRequest(
        @NotBlank String transportista,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate fecha,
        @NotBlank String destino,
        @NotBlank String pedido
) {}
