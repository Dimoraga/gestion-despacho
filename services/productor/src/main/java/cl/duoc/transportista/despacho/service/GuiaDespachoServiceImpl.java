package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import org.springframework.stereotype.Service;

@Service
public class GuiaDespachoServiceImpl implements GuiaDespachoService {

  private final GuiaQueuePublisher queuePublisher;

  public GuiaDespachoServiceImpl(GuiaQueuePublisher queuePublisher) {
    this.queuePublisher = queuePublisher;
  }

  @Override
  public GuiaResponse crear(GuiaRequest request, Long numeroGuia) {
    queuePublisher.publicarGuia(
        new GuiaColaMensaje(
            GuiaColaMensaje.CONTRACT_VERSION,
            numeroGuia,
            request.transportista(),
            request.fecha(),
            request.destino(),
            request.pedido(),
            null));
    return new GuiaResponse(
        numeroGuia,
        request.transportista(),
        request.fecha(),
        request.destino(),
        request.pedido(),
        null);
  }
}
