package cl.duoc.transportista.despacho.config;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration public class RabbitMQConfig {
 @Bean DirectExchange guiasExchange(@Value("${app.rabbitmq.exchange:guias.exchange}") String n){return new DirectExchange(n,true,false);}
 @Bean DirectExchange guiasDlx(){return new DirectExchange("guias.dlx",true,false);}
 @Bean Queue guiasQueue(@Value("${app.rabbitmq.queue.guias:guias.queue}") String n,@Value("${app.rabbitmq.routingkey.errores:guias.dlq}") String k){return QueueBuilder.durable(n).withArgument("x-dead-letter-exchange","guias.dlx").withArgument("x-dead-letter-routing-key",k).build();}
 @Bean Queue guiasErroresQueue(@Value("${app.rabbitmq.queue.errores:guias.errores.queue}") String n){return QueueBuilder.durable(n).build();}
 @Bean Binding guiasBinding(Queue guiasQueue,DirectExchange guiasExchange,@Value("${app.rabbitmq.routingkey.guias:guias.routingkey}") String k){return BindingBuilder.bind(guiasQueue).to(guiasExchange).with(k);}
 @Bean Binding guiasErroresBinding(Queue guiasErroresQueue,DirectExchange guiasDlx,@Value("${app.rabbitmq.routingkey.errores:guias.dlq}") String k){return BindingBuilder.bind(guiasErroresQueue).to(guiasDlx).with(k);}
 @Bean MessageConverter jsonMessageConverter(){Jackson2JsonMessageConverter converter=new Jackson2JsonMessageConverter();DefaultJackson2JavaTypeMapper mapper=new DefaultJackson2JavaTypeMapper();mapper.setIdClassMapping(java.util.Map.of("guia.creada",cl.duoc.transportista.despacho.dto.GuiaColaMensaje.class));converter.setJavaTypeMapper(mapper);return converter;}
 @Bean RabbitTemplate rabbitTemplate(ConnectionFactory f,MessageConverter c){RabbitTemplate t=new RabbitTemplate(f);t.setMessageConverter(c);t.setMandatory(true);t.setConfirmCallback((x,ok,cause)->{if(!ok)LoggerFactory.getLogger(RabbitMQConfig.class).error("RabbitMQ no confirmó publicación {}: {}",x,cause);});t.setReturnsCallback(r->LoggerFactory.getLogger(RabbitMQConfig.class).error("RabbitMQ devolvió mensaje: exchange={}, routingKey={}, reply={}",r.getExchange(),r.getRoutingKey(),r.getReplyText()));return t;}
}
