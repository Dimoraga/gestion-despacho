package cl.duoc.transportista.despacho.consumidor.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GuiaControllerTest {
  private final GuiaRegistroPersistenceService registros =
      mock(GuiaRegistroPersistenceService.class);
  private final GuiaController controller =
      new GuiaController(
          registros,
          mock(GuiaPdfService.class),
          mock(EfsStorageService.class),
          mock(S3StorageService.class));

  @Test
  void devuelve202ParaGuiaEnProceso() {
    GuiaDespachoRegistro r = new GuiaDespachoRegistro();
    r.setEstado(EstadoProcesamiento.PROCESSING);
    when(registros.obtener(1L)).thenReturn(r);
    org.assertj.core.api.Assertions.assertThat(controller.obtener(1L).getStatusCode().value())
        .isEqualTo(202);
  }

  @Test
  void deniegaDescargaSinIdentidadJwt() {
    GuiaDespachoRegistro r = new GuiaDespachoRegistro();
    r.setEstado(EstadoProcesamiento.COMPLETED);
    r.setTransportista("transporte");
    when(registros.obtener(1L)).thenReturn(r);
    assertThatThrownBy(() -> controller.descargar(1L, null))
        .isInstanceOf(ResponseStatusException.class);
  }
}
