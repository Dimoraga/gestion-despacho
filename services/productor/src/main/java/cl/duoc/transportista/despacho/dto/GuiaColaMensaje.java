package cl.duoc.transportista.despacho.dto;

import java.time.LocalDate;

public record GuiaColaMensaje(
    Integer version,
    String requestId,
    String fingerprint,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido) {

  public static final Integer CONTRACT_VERSION = 2;

  public GuiaColaMensaje {
    if (!CONTRACT_VERSION.equals(version)) {
      throw new IllegalArgumentException("Versión de contrato no soportada: " + version);
    }
  }
}
