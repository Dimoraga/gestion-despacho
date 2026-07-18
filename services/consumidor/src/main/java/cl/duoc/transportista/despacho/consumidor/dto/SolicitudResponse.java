package cl.duoc.transportista.despacho.consumidor.dto;

public record SolicitudResponse(String requestId, Long numeroGuia, String estado, String error) {}
