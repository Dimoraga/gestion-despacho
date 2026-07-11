package cl.duoc.transportista.despacho.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GuiaCreadaRabbitListener {
  private final GuiaQueuePublisher publisher;

  public GuiaCreadaRabbitListener(GuiaQueuePublisher publisher) {
    this.publisher = publisher;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publicar(GuiaCreadaEvent event) {
    publisher.publicarGuia(event.mensaje());
  }
}
