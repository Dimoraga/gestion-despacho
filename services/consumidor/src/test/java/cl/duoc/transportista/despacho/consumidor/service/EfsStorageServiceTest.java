package cl.duoc.transportista.despacho.consumidor.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class EfsStorageServiceTest {
  @Test
  void rechazaTraversalFueraDelRoot() throws Exception {
    EfsStorageService efs = new EfsStorageService(Files.createTempDirectory("efs").toString());
    assertThatThrownBy(() -> efs.guardar("../../escape.pdf", new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
