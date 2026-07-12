package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceImplTest {

  @Mock GuiaQueuePublisher queuePublisher;

  @Test
  void crear_publicaEventoVersionadoYNoGeneraArchivos() {
    GuiaDespachoServiceImpl service = new GuiaDespachoServiceImpl(queuePublisher);
    GuiaRequest request =
        new GuiaRequest("transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "P-001");

    GuiaResponse response = service.crear(request, 99L);

    ArgumentCaptor<GuiaColaMensaje> event = ArgumentCaptor.forClass(GuiaColaMensaje.class);
    verify(queuePublisher).publicarGuia(event.capture());
    assertEquals(GuiaColaMensaje.CONTRACT_VERSION, event.getValue().version());
    assertEquals(99L, event.getValue().numeroGuia());
    assertNull(event.getValue().archivoKey());
    assertEquals(99L, response.numeroGuia());
    assertNull(response.archivoKey());
  }
}
