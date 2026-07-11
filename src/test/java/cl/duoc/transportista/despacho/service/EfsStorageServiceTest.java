package cl.duoc.transportista.despacho.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class EfsStorageServiceTest {

  @TempDir Path tmp;

  private EfsStorageService service;

  @BeforeEach
  void setUp() {
    service = new EfsStorageService();
    ReflectionTestUtils.setField(service, "efsDir", tmp.toString());
  }

  @Test
  void guardar_creaArchivoYSubcarpetas() throws Exception {
    byte[] contenido = "hola efs".getBytes();
    String relPath = "2025/transportista1/guia1.pdf";

    Path resultado = service.guardar(relPath, contenido);

    assertTrue(Files.exists(resultado));
    assertArrayEquals(contenido, Files.readAllBytes(resultado));
    assertTrue(Files.isDirectory(resultado.getParent()));
  }

  @Test
  void existe_devuelveTrueCuandoArchivoPresente() {
    byte[] contenido = "datos".getBytes();
    String relPath = "sub/archivo.txt";
    service.guardar(relPath, contenido);

    assertTrue(service.existe(relPath));
  }

  @Test
  void existe_devuelveFalseCuandoArchivoAusente() {
    assertFalse(service.existe("inexistente/archivo.txt"));
  }

  @Test
  void borrar_eliminaArchivoExistente() {
    byte[] contenido = "borrar".getBytes();
    String relPath = "carpeta/borrar.pdf";
    service.guardar(relPath, contenido);
    assertTrue(service.existe(relPath));

    service.borrar(relPath);

    assertFalse(service.existe(relPath));
  }

  @Test
  void borrar_noLanzaExcepcionSiArchivoNoExiste() {
    assertDoesNotThrow(() -> service.borrar("no/existe.pdf"));
  }
}
