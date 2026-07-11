package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class GuiaQueuePublisherTest {
  @Mock RabbitTemplate rabbitTemplate;
  GuiaQueuePublisher publisher;
  GuiaColaMensaje mensaje;

  @BeforeEach
  void setUp() {
    publisher = new GuiaQueuePublisher(rabbitTemplate, "guias.exchange", "guias.routingkey");
    mensaje =
        new GuiaColaMensaje(
            1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", "key.pdf");
  }

  @Test
  void publicarGuia_enviaPayloadOriginal() {
    publisher.publicarGuia(mensaje);
    verify(rabbitTemplate).convertAndSend("guias.exchange", "guias.routingkey", mensaje);
  }

  @Test
  void publicarGuia_noEnvioFallbackADlq() {
    doThrow(new AmqpException("broker caido"))
        .when(rabbitTemplate)
        .convertAndSend("guias.exchange", "guias.routingkey", mensaje);
    assertThrows(AmqpException.class, () -> publisher.publicarGuia(mensaje));
    verify(rabbitTemplate, never())
        .convertAndSend(eq("guias.exchange"), eq("guias.dlq"), any(Object.class));
  }
}
