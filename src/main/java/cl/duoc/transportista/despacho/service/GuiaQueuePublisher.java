package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuiaQueuePublisher {
  private final RabbitTemplate rabbitTemplate;
  private final String exchangeName;
  private final String routingKey;

  public GuiaQueuePublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${app.rabbitmq.exchange}") String exchangeName,
      @Value("${app.rabbitmq.routingkey.guias}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
  }

  public void publicarGuia(GuiaColaMensaje mensaje) {
    rabbitTemplate.convertAndSend(exchangeName, routingKey, mensaje);
  }
}
