package cl.duoc.transportista.despacho.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EfsStorageService {

  @Value("${app.efs.dir:/app/efs}")
  private String efsDir;

  public Path guardar(String relPath, byte[] contenido) {
    try {
      Path target = Paths.get(efsDir).resolve(relPath);
      Files.createDirectories(target.getParent());
      Files.write(target, contenido);
      return target;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public boolean existe(String relPath) {
    return Files.exists(Paths.get(efsDir).resolve(relPath));
  }

  public void borrar(String relPath) {
    try {
      Files.deleteIfExists(Paths.get(efsDir).resolve(relPath));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
