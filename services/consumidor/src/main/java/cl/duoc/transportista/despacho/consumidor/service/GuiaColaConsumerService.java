package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class GuiaColaConsumerService {
  private final GuiaRegistroPersistenceService registros;
  private final GuiaPdfService pdf;
  private final EfsStorageService efs;
  private final S3StorageService s3;

  GuiaColaConsumerService(
      GuiaRegistroPersistenceService registros,
      GuiaPdfService pdf,
      EfsStorageService efs,
      S3StorageService s3) {
    this.registros = registros;
    this.pdf = pdf;
    this.efs = efs;
    this.s3 = s3;
  }

  @RabbitListener(queues = "${app.rabbitmq.queue.guias}")
  public void consumir(GuiaColaMensaje mensaje, Channel channel, Message raw) throws Exception {
    long tag = raw.getMessageProperties().getDeliveryTag();
    GuiaDespachoRegistro r = null;
    try {
      r = registros.preparar(mensaje);
      if (!r.isEliminada()
          && r.getEstado()
              != cl.duoc.transportista.despacho.consumidor.model.EstadoProcesamiento.COMPLETED)
        procesar(r);
      channel.basicAck(tag, false);
    } catch (Exception e) {
      if (r != null) registros.fallar(r.getId());
      channel.basicNack(tag, false, false);
    }
  }

  private void procesar(GuiaDespachoRegistro r) {
    byte[] data = pdf.generar(r);
    var path = efs.guardar(r.getArchivoKey(), data);
    subirS3ConReintentos(r.getArchivoKey(), data);
    registros.completar(r.getId(), path.toString());
  }

  private void subirS3ConReintentos(String key, byte[] data) {
    RuntimeException last = null;
    for (int intento = 1; intento <= 3; intento++)
      try {
        s3.subir(key, data);
        return;
      } catch (RuntimeException error) {
        if (!transitorio(error)) throw error;
        last = error;
        if (intento < 3) esperar(intento);
      }
    throw last;
  }

  private boolean transitorio(RuntimeException error) {
    return error instanceof SdkClientException
        || error instanceof S3Exception s && s.statusCode() >= 500;
  }

  private void esperar(int intento) {
    try {
      Thread.sleep(100L * intento);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
