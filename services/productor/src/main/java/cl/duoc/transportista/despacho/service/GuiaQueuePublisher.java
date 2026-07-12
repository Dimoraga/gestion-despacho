package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuiaQueuePublisher {
  private final RabbitTemplate rabbitTemplate;
  private final String exchangeName;
  private final String routingKey;
  private final Duration confirmTimeout;

  public GuiaQueuePublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${app.rabbitmq.exchange}") String exchangeName,
      @Value("${app.rabbitmq.routingkey.guias}") String routingKey,
      @Value("${app.rabbitmq.confirm-timeout:5s}") Duration confirmTimeout) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
    this.confirmTimeout = confirmTimeout;
  }

  public void publicarGuia(GuiaColaMensaje mensaje) {
    if (!GuiaColaMensaje.CONTRACT_VERSION.equals(mensaje.version())) {
      throw new IllegalArgumentException("Versión de contrato no soportada: " + mensaje.version());
    }
    CorrelationData correlation = new CorrelationData(mensaje.numeroGuia().toString());
    rabbitTemplate.convertAndSend(
        exchangeName,
        routingKey,
        mensaje,
        message -> {
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          message.getMessageProperties().setMessageId(mensaje.numeroGuia().toString());
          message.getMessageProperties().setType("guia.creada");
          message.getMessageProperties().setHeader("x-contract-version", mensaje.version());
          return message;
        },
        correlation);
    try {
      CorrelationData.Confirm confirm =
          correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!confirm.isAck()) {
        throw new AmqpException("RabbitMQ rechazó la publicación de guía " + mensaje.numeroGuia());
      }
      if (correlation.getReturned() != null) {
        throw new AmqpException("RabbitMQ devolvió la publicación de guía " + mensaje.numeroGuia());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AmqpException("Interrumpido esperando confirmación RabbitMQ", ex);
    } catch (java.util.concurrent.TimeoutException ex) {
      throw new AmqpException("Timeout esperando confirmación RabbitMQ", ex);
    } catch (java.util.concurrent.ExecutionException ex) {
      throw new AmqpException("Error esperando confirmación RabbitMQ", ex);
    }
  }
}
