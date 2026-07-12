package cl.duoc.transportista.despacho.dto;

public record ErrorResponse(String timestamp, int status, String error, String message) {}
