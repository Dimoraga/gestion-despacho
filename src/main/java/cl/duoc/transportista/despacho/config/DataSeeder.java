package cl.duoc.transportista.despacho.config;

import cl.duoc.transportista.despacho.model.GuiaDespacho;
import cl.duoc.transportista.despacho.repository.GuiaDespachoRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final GuiaDespachoRepository repository;

  public DataSeeder(GuiaDespachoRepository repository) {
    this.repository = repository;
  }

  @Override
  public void run(String... args) {
    if (repository.existsByPedido("PED-1001")) {
      log.info("Guias semilla ya existen, saltando seed");
      return;
    }
    repository.save(crear("transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-1001"));
    repository.save(crear("transportistaX", LocalDate.of(2021, 3, 16), "Valparaiso", "PED-1002"));
    repository.save(crear("transportistaY", LocalDate.of(2021, 3, 15), "Concepcion", "PED-2001"));
    log.info("Seed completo: {} guias insertadas", repository.count());
  }

  private GuiaDespacho crear(String transportista, LocalDate fecha, String destino, String pedido) {
    GuiaDespacho g = new GuiaDespacho();
    g.setTransportista(transportista);
    g.setFecha(fecha);
    g.setDestino(destino);
    g.setPedido(pedido);
    return g;
  }
}
