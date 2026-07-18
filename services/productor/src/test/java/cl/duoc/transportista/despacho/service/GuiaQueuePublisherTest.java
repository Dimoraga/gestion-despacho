package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class GuiaQueuePublisherTest {
  @Mock RabbitTemplate rabbitTemplate;
  GuiaQueuePublisher publisher;
  GuiaColaMensaje mensaje;

  @BeforeEach
  void setUp() {
    publisher =
        new GuiaQueuePublisher(
            rabbitTemplate, "guias.exchange", "guias.routingkey", Duration.ofSeconds(1));
    mensaje =
        new GuiaColaMensaje(
            GuiaColaMensaje.CONTRACT_VERSION,
            "a14bb491-06bd-4348-a2e4-6d7a8d3f121e",
            "08d815d3e42a15c6d1d55cbaf5d28fbbdff5d8c555677a8af6cd2e1c715e809c",
            "transportistaX",
            LocalDate.of(2021, 3, 15),
            "Santiago",
            "PED-001");
  }

  @Test
  void publicarGuia_enviaPayloadPersistenteYEsperaConfirmacion() {
    doAnswer(
            invocation -> {
              CorrelationData correlation = invocation.getArgument(4);
              correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(rabbitTemplate)
        .convertAndSend(eq("guias.exchange"), eq("guias.routingkey"), eq(mensaje), any(), any());

    publisher.publicarGuia(mensaje);

    ArgumentCaptor<MessagePostProcessor> processor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("guias.exchange"),
            eq("guias.routingkey"),
            eq(mensaje),
            processor.capture(),
            any(CorrelationData.class));
    Message published =
        processor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
    assertEquals(
        MessageDeliveryMode.PERSISTENT, published.getMessageProperties().getDeliveryMode());
    assertEquals("a14bb491-06bd-4348-a2e4-6d7a8d3f121e", published.getMessageProperties().getMessageId());
    assertEquals(
        GuiaColaMensaje.CONTRACT_VERSION,
        published.getMessageProperties().getHeader("x-contract-version"));
  }

  @Test
  void publicarGuia_confirmacionRechazada_lanzaExcepcion() {
    doAnswer(
            invocation -> {
              CorrelationData correlation = invocation.getArgument(4);
              correlation.getFuture().complete(new CorrelationData.Confirm(false, "rechazado"));
              return null;
            })
        .when(rabbitTemplate)
        .convertAndSend(eq("guias.exchange"), eq("guias.routingkey"), eq(mensaje), any(), any());

    assertThrows(AmqpException.class, () -> publisher.publicarGuia(mensaje));
    verify(rabbitTemplate, never())
        .convertAndSend(eq("guias.exchange"), eq("guias.dlq"), any(Object.class));
  }

  @Test
  void contrato_rechazaVersionNoSoportada() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GuiaColaMensaje(
                3,
                "a14bb491-06bd-4348-a2e4-6d7a8d3f121e",
                "08d815d3e42a15c6d1d55cbaf5d28fbbdff5d8c555677a8af6cd2e1c715e809c",
                "transportistaX",
                LocalDate.of(2021, 3, 15),
                "Santiago",
                "PED-001"));
  }
}
