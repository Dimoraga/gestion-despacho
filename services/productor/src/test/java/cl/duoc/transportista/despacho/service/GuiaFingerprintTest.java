package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GuiaFingerprintTest {

  @Test
  void calcular_aplicaLaCanonicalizacionCompartidaConConsumidor() {
    GuiaRequest original =
        new GuiaRequest(
            "  Transporte\u00a0Norte ", LocalDate.of(2026, 7, 18), " SANTIAGO\tCENTRO ", "ped-01");
    GuiaRequest canonical =
        new GuiaRequest("transporte norte", LocalDate.of(2026, 7, 18), "santiago centro", "PED-01");

    assertEquals(GuiaFingerprint.calcular(canonical), GuiaFingerprint.calcular(original));
  }
}
