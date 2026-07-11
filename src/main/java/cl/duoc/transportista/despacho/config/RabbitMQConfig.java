package cl.duoc.transportista.despacho.config;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConfig.class);

  @Bean
  DirectExchange guiasExchange(
      @Value("${app.rabbitmq.exchange:guias.exchange}") String exchangeName) {
    return new DirectExchange(exchangeName, true, false);
  }

  @Bean
  DirectExchange guiasDlx() {
    return new DirectExchange("guias.dlx", true, false);
  }

  @Bean
  Queue guiasQueue(
      @Value("${app.rabbitmq.queue.guias:guias.queue}") String queueName,
      @Value("${app.rabbitmq.routingkey.errores:guias.dlq}") String deadLetterRoutingKey) {
    return QueueBuilder.durable(queueName)
        .withArgument("x-dead-letter-exchange", "guias.dlx")
        .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
        .build();
  }

  @Bean
  Queue guiasErroresQueue(
      @Value("${app.rabbitmq.queue.errores:guias.errores.queue}") String queueName) {
    return QueueBuilder.durable(queueName).build();
  }

  @Bean
  Binding guiasBinding(
      Queue guiasQueue,
      DirectExchange guiasExchange,
      @Value("${app.rabbitmq.routingkey.guias:guias.routingkey}") String routingKey) {
    return BindingBuilder.bind(guiasQueue).to(guiasExchange).with(routingKey);
  }

  @Bean
  Binding guiasErroresBinding(
      Queue guiasErroresQueue,
      DirectExchange guiasDlx,
      @Value("${app.rabbitmq.routingkey.errores:guias.dlq}") String routingKey) {
    return BindingBuilder.bind(guiasErroresQueue).to(guiasDlx).with(routingKey);
  }

  @Bean
  MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setIdClassMapping(Map.of("guia.creada", GuiaColaMensaje.class));
    converter.setJavaTypeMapper(typeMapper);
    return converter;
  }

  @Bean
  RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter);
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(
        (correlation, confirmed, cause) -> {
          if (!confirmed) {
            LOG.error("RabbitMQ no confirmó publicación {}: {}", correlation, cause);
          }
        });
    rabbitTemplate.setReturnsCallback(
        returned ->
            LOG.error(
                "RabbitMQ devolvió mensaje: exchange={}, routingKey={}, reply={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyText()));
    return rabbitTemplate;
  }
}
