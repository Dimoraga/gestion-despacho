package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class GuiaColaConsumerService {
  private static final Logger LOG = LoggerFactory.getLogger(GuiaColaConsumerService.class);
  private static final int MAXIMO_LOTE = 10;
  private static final int MAXIMO_REINTENTOS = 3;

  private final RabbitTemplate rabbitTemplate;
  private final GuiaRegistroPersistenceService persistenceService;
  private final MessageConverter messageConverter;
  private final String queueName;
  private final DefaultMessagePropertiesConverter propertiesConverter =
      new DefaultMessagePropertiesConverter();

  public GuiaColaConsumerService(
      RabbitTemplate rabbitTemplate,
      GuiaRegistroPersistenceService persistenceService,
      MessageConverter messageConverter,
      @Value("${app.rabbitmq.queue.guias}") String queueName) {
    this.rabbitTemplate = rabbitTemplate;
    this.persistenceService = persistenceService;
    this.messageConverter = messageConverter;
    this.queueName = queueName;
  }

  public List<GuiaDespachoRegistroResponse> consumirColaGuias() {
    return rabbitTemplate.execute(this::procesarLote);
  }

  private List<GuiaDespachoRegistroResponse> procesarLote(Channel channel) throws Exception {
    List<GuiaDespachoRegistroResponse> procesadas = new ArrayList<>();
    for (int indice = 0; indice < MAXIMO_LOTE; indice++) {
      GetResponse entrega = obtenerSiguienteMensaje(channel);
      if (entrega == null) {
        break;
      }
      procesarEntrega(channel, entrega, procesadas);
    }
    return procesadas;
  }

  private GetResponse obtenerSiguienteMensaje(Channel channel) throws Exception {
    return channel.basicGet(queueName, false);
  }

  private void procesarEntrega(
      Channel channel, GetResponse entrega, List<GuiaDespachoRegistroResponse> procesadas)
      throws Exception {
    long deliveryTag = entrega.getEnvelope().getDeliveryTag();
    GuiaColaMensaje mensaje = null;
    try {
      mensaje = convertirMensaje(entrega);
      GuiaDespachoRegistro registro = persistirConReintentos(mensaje);
      confirmarEntrega(channel, deliveryTag);
      procesadas.add(aResponse(registro));
    } catch (Exception exception) {
      rechazarHaciaDlq(channel, deliveryTag, mensaje, exception);
    }
  }

  private GuiaColaMensaje convertirMensaje(GetResponse entrega) {
    MessageProperties properties =
        propertiesConverter.toMessageProperties(entrega.getProps(), entrega.getEnvelope(), "UTF-8");
    return (GuiaColaMensaje)
        messageConverter.fromMessage(new Message(entrega.getBody(), properties));
  }

  private GuiaDespachoRegistro persistirConReintentos(GuiaColaMensaje mensaje) {
    RuntimeException ultimoError = null;
    for (int intento = 1; intento <= MAXIMO_REINTENTOS; intento++) {
      try {
        return persistenceService
            .buscarPorNumeroGuia(mensaje.numeroGuia())
            .orElseGet(() -> crearRegistroManejandoCarrera(mensaje));
      } catch (RuntimeException exception) {
        ultimoError = exception;
        if (intento < MAXIMO_REINTENTOS) {
          esperarAntesDeReintentar(intento);
        }
      }
    }
    throw ultimoError;
  }

  private GuiaDespachoRegistro crearRegistroManejandoCarrera(GuiaColaMensaje mensaje) {
    try {
      return persistenceService.crearRegistro(mensaje);
    } catch (DataIntegrityViolationException collision) {
      return persistenceService
          .buscarPorNumeroGuia(mensaje.numeroGuia())
          .orElseThrow(() -> collision);
    }
  }

  private void confirmarEntrega(Channel channel, long deliveryTag) throws Exception {
    channel.basicAck(deliveryTag, false);
  }

  private void rechazarHaciaDlq(
      Channel channel, long deliveryTag, GuiaColaMensaje mensaje, Exception exception)
      throws Exception {
    LOG.error(
        "No se pudo procesar mensaje de queue={} deliveryTag={} numeroGuia={}; se enviará a DLQ",
        queueName,
        deliveryTag,
        mensaje == null ? null : mensaje.numeroGuia(),
        exception);
    channel.basicNack(deliveryTag, false, false);
  }

  private void esperarAntesDeReintentar(int intento) {
    try {
      Thread.sleep(25L * intento);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private GuiaDespachoRegistroResponse aResponse(GuiaDespachoRegistro registro) {
    return new GuiaDespachoRegistroResponse(
        registro.getId(),
        registro.getNumeroGuia(),
        registro.getTransportista(),
        registro.getFecha(),
        registro.getDestino(),
        registro.getPedido(),
        registro.getArchivoKey(),
        registro.getFechaProcesado());
  }
}
