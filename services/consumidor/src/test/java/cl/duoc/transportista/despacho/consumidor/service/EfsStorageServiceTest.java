package cl.duoc.transportista.despacho.consumidor.service;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Test
  void adoptaArchivoExistenteSoloConChecksumCompatible() throws Exception {
    EfsStorageService efs = new EfsStorageService(Files.createTempDirectory("efs").toString());
    String checksum = "039058c6f2c0cb492c533b0a4d14ef77a0a3e2ed86dfd4ec7a3f9d15b3d2f4f7";
    // SHA-256 real de {1,2,3}; la segunda publicación no sobrescribe el artefacto.
    checksum =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(new byte[] {1, 2, 3}));
    efs.guardarSiAusente("a/guia.pdf", new byte[] {1, 2, 3}, checksum);
    assertThat(efs.guardarSiAusente("a/guia.pdf", new byte[] {1, 2, 3}, checksum)).exists();
    assertThatThrownBy(() -> efs.guardarSiAusente("a/guia.pdf", new byte[] {9}, "bad"))
        .isInstanceOf(java.io.UncheckedIOException.class);
  }
}
