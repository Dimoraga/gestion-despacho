package cl.duoc.transportista.despacho.consumidor.service;

import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

class GuiaColaConsumerServiceTest {
  private final GuiaRegistroPersistenceService registros =
      mock(GuiaRegistroPersistenceService.class);
  private final GuiaColaConsumerService listener = new GuiaColaConsumerService(registros);
  private final GuiaColaMensaje event =
      new GuiaColaMensaje(
          Integer.valueOf(2),
          "request-1",
          "fingerprint",
          "transporte",
          LocalDate.now(),
          "destino",
          "pedido");

  @Test
  void haceAckSoloDespuesDePersistir() throws Exception {
    Channel channel = mock(Channel.class);
    Message raw = mensaje(4);
    listener.consumir(event, channel, raw);
    var order = inOrder(registros, channel);
    order.verify(registros).preparar(event);
    order.verify(channel).basicAck(4, false);
  }

  @Test
  void haceNackSinRequeueAnteErrorTerminal() throws Exception {
    Channel channel = mock(Channel.class);
    when(registros.preparar(event))
        .thenThrow(new GuiaRegistroPersistenceService.UnsupportedEventException());
    listener.consumir(event, channel, mensaje(5));
    verify(channel).basicNack(5, false, false);
    verify(channel, never()).basicAck(anyLong(), anyBoolean());
  }

  @Test
  void reencolaErrorDeBaseDeDatos() throws Exception {
    Channel channel = mock(Channel.class);
    doThrow(new RuntimeException("Oracle unavailable")).when(registros).preparar(event);
    listener.consumir(event, channel, mensaje(6));
    verify(channel).basicNack(6, false, true);
  }

  @Test
  void esListenerAutomatico() throws Exception {
    assert GuiaColaConsumerService.class
        .getMethod("consumir", GuiaColaMensaje.class, Channel.class, Message.class)
        .isAnnotationPresent(RabbitListener.class);
  }

  @Test
  void deserializaContratoProductorV2SinNumeroGuiaNiArchivoKey() throws Exception {
    String json =
        """
        {"version":2,"requestId":"req-1","fingerprint":"abc","transportista":"transporte",
        "fecha":"2026-01-02","destino":"destino","pedido":"pedido"}
        """;
    GuiaColaMensaje mensaje =
        new ObjectMapper().findAndRegisterModules().readValue(json, GuiaColaMensaje.class);
    org.assertj.core.api.Assertions.assertThat(mensaje.requestId()).isEqualTo("req-1");
    org.assertj.core.api.Assertions.assertThat(mensaje.pedido()).isEqualTo("pedido");
  }

  private Message mensaje(long tag) {
    MessageProperties p = new MessageProperties();
    p.setDeliveryTag(tag);
    return new Message(new byte[0], p);
  }
}
