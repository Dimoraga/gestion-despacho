package cl.duoc.transportista.despacho.consumidor.service;

import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.*;
import com.rabbitmq.client.Channel;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import software.amazon.awssdk.services.s3.model.S3Exception;

class GuiaColaConsumerServiceTest {
  private final GuiaRegistroPersistenceService registros=mock(GuiaRegistroPersistenceService.class); private final GuiaPdfService pdf=mock(GuiaPdfService.class); private final EfsStorageService efs=mock(EfsStorageService.class); private final S3StorageService s3=mock(S3StorageService.class);
  private final GuiaColaConsumerService listener=new GuiaColaConsumerService(registros,pdf,efs,s3);
  private final GuiaColaMensaje event=new GuiaColaMensaje(Integer.valueOf(1),9L,"transporte",LocalDate.now(),"destino","pedido",null);
  @Test void haceAckSoloDespuesDePersistirYS3() throws Exception { GuiaDespachoRegistro r=registro(); Channel channel=mock(Channel.class); Message raw=mensaje(4); when(registros.preparar(event)).thenReturn(r); when(pdf.generar(r)).thenReturn(new byte[] {1}); when(efs.guardar("key",new byte[] {1})).thenReturn(Path.of("/efs/key")); listener.consumir(event,channel,raw); var order=inOrder(efs,s3,registros,channel); order.verify(efs).guardar("key",new byte[] {1}); order.verify(s3).subir("key",new byte[] {1}); order.verify(registros).completar(7L,"/efs/key"); order.verify(channel).basicAck(4,false); }
  @Test void haceNackSinRequeueAnteErrorTerminal() throws Exception { Channel channel=mock(Channel.class); when(registros.preparar(event)).thenThrow(new GuiaRegistroPersistenceService.UnsupportedEventException()); listener.consumir(event,channel,mensaje(5)); verify(channel).basicNack(5,false,false); verify(channel,never()).basicAck(anyLong(),anyBoolean()); }
  @Test void noReintentaErrorS3Terminal() throws Exception { GuiaDespachoRegistro r=registro(); Channel channel=mock(Channel.class); when(registros.preparar(event)).thenReturn(r); when(pdf.generar(r)).thenReturn(new byte[] {1}); when(efs.guardar("key",new byte[] {1})).thenReturn(Path.of("/efs/key")); doThrow(S3Exception.builder().statusCode(403).build()).when(s3).subir("key",new byte[] {1}); listener.consumir(event,channel,mensaje(6)); verify(s3,times(1)).subir("key",new byte[] {1}); verify(channel).basicNack(6,false,false); }
  @Test void ackDeTombstoneNoVuelveACrearGuia() throws Exception { GuiaDespachoRegistro r=registro(); r.setEliminada(true); Channel channel=mock(Channel.class); when(registros.preparar(event)).thenReturn(r); listener.consumir(event,channel,mensaje(7)); verify(channel).basicAck(7,false); verifyNoInteractions(pdf,efs,s3); }
  @Test void esListenerAutomatico() throws Exception { assert GuiaColaConsumerService.class.getMethod("consumir",GuiaColaMensaje.class,Channel.class,Message.class).isAnnotationPresent(RabbitListener.class); }
  private GuiaDespachoRegistro registro(){ GuiaDespachoRegistro r=new GuiaDespachoRegistro(); r.setId(7L); r.setArchivoKey("key"); r.setEstado(EstadoProcesamiento.PROCESSING); return r; }
  private Message mensaje(long tag) { MessageProperties p=new MessageProperties(); p.setDeliveryTag(tag); return new Message(new byte[0],p); }
}
