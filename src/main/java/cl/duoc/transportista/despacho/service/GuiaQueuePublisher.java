package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaErrorMensaje;
import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GuiaQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(GuiaQueuePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String guiasRoutingKey;
    private final String erroresRoutingKey;

    public GuiaQueuePublisher(RabbitTemplate rabbitTemplate,
                               @Value("${app.rabbitmq.exchange}") String exchangeName,
                               @Value("${app.rabbitmq.routingkey.guias}") String guiasRoutingKey,
                               @Value("${app.rabbitmq.routingkey.errores}") String erroresRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.guiasRoutingKey = guiasRoutingKey;
        this.erroresRoutingKey = erroresRoutingKey;
    }

    public void publicarGuia(GuiaColaMensaje mensaje) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, guiasRoutingKey, mensaje);
        } catch (AmqpException ex) {
            log.warn("Fallo al publicar guia {} en la cola principal, se envia a la cola de errores", mensaje.numeroGuia(), ex);
            publicarError(mensaje, ex.getMessage());
        }
    }

    private void publicarError(GuiaColaMensaje mensaje, String motivo) {
        GuiaColaErrorMensaje error = new GuiaColaErrorMensaje(mensaje, motivo, Instant.now());
        rabbitTemplate.convertAndSend(exchangeName, erroresRoutingKey, error);
    }
}
