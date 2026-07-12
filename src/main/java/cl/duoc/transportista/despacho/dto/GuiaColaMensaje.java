package cl.duoc.transportista.despacho.dto;

import java.time.LocalDate;

public record GuiaColaMensaje(
    Integer version,
    Long numeroGuia,
    String transportista,
    LocalDate fecha,
    String destino,
    String pedido,
    String archivoKey) {

  public static final Integer CONTRACT_VERSION = 1;

  public GuiaColaMensaje {
    if (!CONTRACT_VERSION.equals(version)) {
      throw new IllegalArgumentException("Versión de contrato no soportada: " + version);
    }
  }
}
