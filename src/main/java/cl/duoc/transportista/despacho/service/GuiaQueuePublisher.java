package cl.duoc.transportista.despacho.service;
import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service public class GuiaQueuePublisher {
 private final RabbitTemplate template; private final String exchange; private final String key;
 public GuiaQueuePublisher(RabbitTemplate template,@Value("${app.rabbitmq.exchange}") String exchange,@Value("${app.rabbitmq.routingkey.guias}") String key){this.template=template;this.exchange=exchange;this.key=key;}
 public void publicarGuia(GuiaColaMensaje mensaje){template.convertAndSend(exchange,key,mensaje);}
}
