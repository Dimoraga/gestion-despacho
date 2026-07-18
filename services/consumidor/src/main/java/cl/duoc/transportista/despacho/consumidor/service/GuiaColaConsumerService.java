package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class GuiaColaConsumerService {
  private final GuiaRegistroPersistenceService registros;

  GuiaColaConsumerService(GuiaRegistroPersistenceService registros) {
    this.registros = registros;
  }

  @RabbitListener(queues = "${app.rabbitmq.queue.guias}")
  public void consumir(GuiaColaMensaje mensaje, Channel channel, Message raw) throws Exception {
    long tag = raw.getMessageProperties().getDeliveryTag();
    try {
      registros.preparar(mensaje); // transaction commits before returning, then ACK is safe.
      channel.basicAck(tag, false);
    } catch (GuiaRegistroPersistenceService.UnsupportedEventException
        | GuiaRegistroPersistenceService.PayloadConflictException e) {
      channel.basicNack(tag, false, false); // corrupt/incompatible messages go to DLQ.
    } catch (Exception e) {
      channel.basicNack(tag, false, true); // Oracle and transient infrastructure errors requeue.
    }
  }
}
