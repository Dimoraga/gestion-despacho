package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GuiaDespachoServiceImpl implements GuiaDespachoService {

  private final GuiaQueuePublisher queuePublisher;

  public GuiaDespachoServiceImpl(GuiaQueuePublisher queuePublisher) {
    this.queuePublisher = queuePublisher;
  }

  @Override
  public GuiaResponse crear(GuiaRequest request) {
    String requestId = UUID.randomUUID().toString();
    String fingerprint = GuiaFingerprint.calcular(request);
    queuePublisher.publicarGuia(
        new GuiaColaMensaje(
            GuiaColaMensaje.CONTRACT_VERSION,
            requestId,
            fingerprint,
            request.transportista(),
            request.fecha(),
            request.destino(),
            request.pedido()));
    return new GuiaResponse(requestId);
  }
}
