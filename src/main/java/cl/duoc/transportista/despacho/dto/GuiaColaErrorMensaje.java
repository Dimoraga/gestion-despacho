package cl.duoc.transportista.despacho.dto;

import java.time.Instant;

public record GuiaColaErrorMensaje(
        GuiaColaMensaje guia,
        String motivoError,
        Instant fechaError
) {}
