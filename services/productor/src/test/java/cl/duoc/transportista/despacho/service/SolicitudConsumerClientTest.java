package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cl.duoc.transportista.despacho.dto.SolicitudResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.ResourceAccessException;

class SolicitudConsumerClientTest {
  private static final String REQUEST_ID = "a14bb491-06bd-4348-a2e4-6d7a8d3f121e";

  private final RestOperations restOperations = Mockito.mock(RestOperations.class);
  private final SolicitudConsumerClient client = new SolicitudConsumerClient(restOperations);

  @Test
  void consultar_404Interno_indicaQueAunNoHayObservacion() {
    when(restOperations.getForObject(
            eq("/api/solicitudes/{requestId}"), eq(SolicitudResponse.class), eq(REQUEST_ID)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

    assertTrue(client.consultar(REQUEST_ID).isEmpty());
  }

  @Test
  void consultar_respuestaInterna_laDevuelveSinAplicarOwnershipDeCliente() {
    SolicitudResponse response = new SolicitudResponse(REQUEST_ID, 12L, "PENDING", null);
    when(restOperations.getForObject(
            eq("/api/solicitudes/{requestId}"), eq(SolicitudResponse.class), eq(REQUEST_ID)))
        .thenReturn(response);

    assertEquals(response, client.consultar(REQUEST_ID).orElseThrow());
  }

  @Test
  void consultar_errorDeRed_seTraduceAConsumidorNoDisponible() {
    when(restOperations.getForObject(
            eq("/api/solicitudes/{requestId}"), eq(SolicitudResponse.class), eq(REQUEST_ID)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    assertThrows(ConsumerUnavailableException.class, () -> client.consultar(REQUEST_ID));
  }
}
