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
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue.guias}")
    private String guiasQueueName;

    @Value("${app.rabbitmq.queue.errores}")
    private String erroresQueueName;

    @Value("${app.rabbitmq.routingkey.guias}")
    private String guiasRoutingKey;

    @Value("${app.rabbitmq.routingkey.errores}")
    private String erroresRoutingKey;

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue guiasQueue() {
        return new Queue(guiasQueueName, true);
    }

    @Bean
    public Queue guiasErroresQueue() {
        return new Queue(erroresQueueName, true);
    }

    @Bean
    public Binding guiasBinding() {
        return BindingBuilder.bind(guiasQueue()).to(guiasExchange()).with(guiasRoutingKey);
    }

    @Bean
    public Binding guiasErroresBinding() {
        return BindingBuilder.bind(guiasErroresQueue()).to(guiasExchange()).with(erroresRoutingKey);
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
