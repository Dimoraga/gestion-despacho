package cl.duoc.transportista.despacho.consumidor.config;

import cl.duoc.transportista.despacho.consumidor.service.GuiaRegistroPersistenceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcessingRecovery {
  private final GuiaRegistroPersistenceService service;

  ProcessingRecovery(GuiaRegistroPersistenceService service) {
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${app.processing-recovery-ms:60000}")
  public void recover() {
    service.recuperarProcesandoAbandonado();
  }
}
