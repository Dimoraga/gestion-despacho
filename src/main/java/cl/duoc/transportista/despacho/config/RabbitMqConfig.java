package cl.duoc.transportista.despacho.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue guiaDespachoQueue(@Value("${app.rabbitmq.queue:guia-despacho.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange guiaDespachoExchange(@Value("${app.rabbitmq.exchange:guia-despacho.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Binding guiaDespachoBinding(Queue guiaDespachoQueue,
                                       DirectExchange guiaDespachoExchange,
                                       @Value("${app.rabbitmq.routing-key:guia-despacho.creada}") String routingKey) {
        return BindingBuilder.bind(guiaDespachoQueue).to(guiaDespachoExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
