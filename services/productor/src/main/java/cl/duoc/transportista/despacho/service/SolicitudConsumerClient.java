package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.SolicitudResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

@Component
public class SolicitudConsumerClient {
  private final RestOperations restOperations;

  public SolicitudConsumerClient(RestOperations consumidorRestOperations) {
    this.restOperations = consumidorRestOperations;
  }

  public Optional<SolicitudResponse> consultar(String requestId) {
    try {
      return Optional.ofNullable(
          restOperations.getForObject("/api/solicitudes/{requestId}", SolicitudResponse.class, requestId));
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw ex;
    } catch (ResourceAccessException ex) {
      throw new ConsumerUnavailableException(ex);
    }
  }
}
