package cl.duoc.transportista.despacho.consumidor.service;
import cl.duoc.transportista.despacho.consumidor.dto.*;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class GuiaColaConsumerService {
 private static final int MAX_LOTE=10,MAX_REINTENTOS=3; private final RabbitTemplate rabbit; private final GuiaRegistroPersistenceService persistence; private final MessageConverter converter; private final String queue; private final DefaultMessagePropertiesConverter properties=new DefaultMessagePropertiesConverter();
 public GuiaColaConsumerService(RabbitTemplate rabbit,GuiaRegistroPersistenceService persistence,MessageConverter converter,@Value("${app.rabbitmq.queue.guias}") String queue){this.rabbit=rabbit;this.persistence=persistence;this.converter=converter;this.queue=queue;}
 public List<GuiaDespachoRegistroResponse> consumirColaGuias(){return rabbit.execute(this::consumir);}
 private List<GuiaDespachoRegistroResponse> consumir(Channel ch)throws Exception{List<GuiaDespachoRegistroResponse> out=new ArrayList<>();for(int i=0;i<MAX_LOTE;i++){var get=ch.basicGet(queue,false);if(get==null)break;long tag=get.getEnvelope().getDeliveryTag();try{var props=properties.toMessageProperties(get.getProps(),get.getEnvelope(),"UTF-8");GuiaColaMensaje m=(GuiaColaMensaje)converter.fromMessage(new Message(get.getBody(),props));GuiaDespachoRegistro r=persistirConRetry(m);ch.basicAck(tag,false);out.add(toResponse(r));}catch(Exception e){ch.basicNack(tag,false,false);}}return out;}
 private GuiaDespachoRegistro persistirConRetry(GuiaColaMensaje m){RuntimeException failure=null;for(int i=1;i<=MAX_REINTENTOS;i++)try{return persistence.buscar(m.numeroGuia()).orElseGet(()->insertarTrasCarrera(m));}catch(RuntimeException e){failure=e;if(i<MAX_REINTENTOS)esperar(i);}throw failure;}
 private GuiaDespachoRegistro insertarTrasCarrera(GuiaColaMensaje m){try{return persistence.insertar(m);}catch(DataIntegrityViolationException collision){return persistence.buscar(m.numeroGuia()).orElseThrow(()->collision);}}
 private void esperar(int intento){try{Thread.sleep(25L*intento);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
 private GuiaDespachoRegistroResponse toResponse(GuiaDespachoRegistro r){return new GuiaDespachoRegistroResponse(r.getId(),r.getNumeroGuia(),r.getTransportista(),r.getFecha(),r.getDestino(),r.getPedido(),r.getArchivoKey(),r.getFechaProcesado());}
}
