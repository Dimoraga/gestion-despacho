package cl.duoc.transportista.despacho.consumidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionDespachoConsumidorApplication {

  public static void main(String[] args) {
    SpringApplication.run(GestionDespachoConsumidorApplication.class, args);
  }
}
