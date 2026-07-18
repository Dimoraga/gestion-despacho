package cl.duoc.transportista.despacho.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@JsonDeserialize(using = GuiaRequestDeserializer.class)
public record GuiaRequest(
    @NotBlank @Size(max = 255) String transportista,
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate fecha,
    @NotBlank @Size(max = 255) String destino,
    @NotBlank @Size(max = 255) String pedido) {}
