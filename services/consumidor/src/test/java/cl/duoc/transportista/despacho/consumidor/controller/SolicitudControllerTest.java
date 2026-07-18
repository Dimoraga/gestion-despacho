package cl.duoc.transportista.despacho.consumidor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.dto.SolicitudResponse;
import cl.duoc.transportista.despacho.consumidor.service.GuiaRegistroPersistenceService;
import org.junit.jupiter.api.Test;

class SolicitudControllerTest {
  @Test
  void devuelveEstadoDurableDeSolicitud() {
    GuiaRegistroPersistenceService registros = mock(GuiaRegistroPersistenceService.class);
    SolicitudResponse esperada =
        new SolicitudResponse("11111111-1111-1111-1111-111111111111", 42L, "RETRY", "EFS denied");
    when(registros.consultarSolicitud(esperada.requestId())).thenReturn(esperada);

    SolicitudResponse actual = new SolicitudController(registros).obtener(esperada.requestId());

    assertThat(actual).isEqualTo(esperada);
  }
}
