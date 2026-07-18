package cl.duoc.transportista.despacho.service;

public class ConsumerUnavailableException extends RuntimeException {
  public ConsumerUnavailableException(Throwable cause) {
    super("El consumidor interno no está disponible", cause);
  }
}
