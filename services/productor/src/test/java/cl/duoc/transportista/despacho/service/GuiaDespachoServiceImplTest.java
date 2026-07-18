package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceImplTest {

  @Mock GuiaQueuePublisher queuePublisher;

  @Test
  void crear_solicitudesIguales_generanRequestIdsDistintosYMismoFingerprint() {
    GuiaDespachoServiceImpl service = new GuiaDespachoServiceImpl(queuePublisher);
    GuiaRequest request =
        new GuiaRequest("transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "P-001");

    GuiaResponse firstResponse = service.crear(request);
    GuiaResponse secondResponse = service.crear(request);

    ArgumentCaptor<GuiaColaMensaje> event = ArgumentCaptor.forClass(GuiaColaMensaje.class);
    verify(queuePublisher, times(2)).publicarGuia(event.capture());
    List<GuiaColaMensaje> events = event.getAllValues();
    assertEquals(GuiaColaMensaje.CONTRACT_VERSION, events.get(0).version());
    assertEquals(events.get(0).requestId(), firstResponse.requestId());
    assertEquals(events.get(1).requestId(), secondResponse.requestId());
    assertEquals(events.get(0).fingerprint(), events.get(1).fingerprint());
    assertNotEquals(events.get(0).requestId(), events.get(1).requestId());
  }
}
