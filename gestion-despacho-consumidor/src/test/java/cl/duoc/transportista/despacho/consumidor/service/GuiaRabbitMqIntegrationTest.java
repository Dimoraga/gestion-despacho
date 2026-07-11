package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class GuiaRabbitMqIntegrationTest {
 @Container static final RabbitMQContainer rabbit=new RabbitMQContainer("rabbitmq:3.13-management");
 @Autowired GuiaColaConsumerService service;
 @Autowired GuiaDespachoRegistroRepository repo;
 @DynamicPropertySource static void rabbitProperties(DynamicPropertyRegistry r){r.add("spring.rabbitmq.host",rabbit::getHost);r.add("spring.rabbitmq.port",rabbit::getAmqpPort);r.add("spring.rabbitmq.username",rabbit::getAdminUsername);r.add("spring.rabbitmq.password",rabbit::getAdminPassword);}
 @BeforeEach void setup() throws Exception {repo.deleteAll();try(var c=connection();var ch=c.createChannel()){ch.exchangeDeclare("guias.exchange","direct",true);ch.exchangeDeclare("guias.dlx","direct",true);ch.queueDeclare("guias.queue",true,false,false,Map.of("x-dead-letter-exchange","guias.dlx","x-dead-letter-routing-key","guias.dlq"));ch.queueDeclare("guias.errores.queue",true,false,false,null);ch.queueBind("guias.queue","guias.exchange","guias.routingkey");ch.queueBind("guias.errores.queue","guias.dlx","guias.dlq");ch.queuePurge("guias.queue");ch.queuePurge("guias.errores.queue");}}
 @Test void mensajeRealDelProductorSeConsumeYReentregaNoDuplica() throws Exception {publishProducerMessage(9L);assertEquals(1,service.consumirColaGuias().size());publishProducerMessage(9L);assertEquals(1,service.consumirColaGuias().size());assertEquals(1,repo.count());}
 @Test void mensajeInvalidoTerminaEnDlq() throws Exception {try(var c=connection();var ch=c.createChannel()){ch.basicPublish("guias.exchange","guias.routingkey",new AMQP.BasicProperties.Builder().contentType("application/json").headers(Map.of("__TypeId__","guia.creada")).build(),"not-json".getBytes(StandardCharsets.UTF_8));}service.consumirColaGuias();try(var c=connection();var ch=c.createChannel()){assertNotNull(ch.basicGet("guias.errores.queue",true));}}
 private void publishProducerMessage(long id) throws Exception {String json="{\"numeroGuia\":"+id+",\"transportista\":\"transportista\",\"fecha\":\"2025-01-01\",\"destino\":\"Santiago\",\"pedido\":\"P-"+id+"\",\"archivoKey\":\"key\"}";try(var c=connection();var ch=c.createChannel()){ch.basicPublish("guias.exchange","guias.routingkey",new AMQP.BasicProperties.Builder().contentType("application/json").contentEncoding("UTF-8").headers(Map.of("__TypeId__","guia.creada")).build(),json.getBytes(StandardCharsets.UTF_8));}}
 private Connection connection() throws Exception {ConnectionFactory f=new ConnectionFactory();f.setHost(rabbit.getHost());f.setPort(rabbit.getAmqpPort());f.setUsername(rabbit.getAdminUsername());f.setPassword(rabbit.getAdminPassword());return f.newConnection();}
}
