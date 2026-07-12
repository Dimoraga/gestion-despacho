package cl.duoc.transportista.despacho.consumidor.service;

import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EfsStorageService {
  private final Path root;

  EfsStorageService(@Value("${app.efs.dir:/app/efs}") String dir) {
    root = Paths.get(dir).toAbsolutePath().normalize();
  }

  public Path guardar(String key, byte[] bytes) {
    try {
      Path path = seguro(key);
      Files.createDirectories(path.getParent());
      Files.write(path, bytes);
      return path;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void borrar(String key) {
    try {
      Files.deleteIfExists(seguro(key));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path seguro(String key) {
    Path relativo = Paths.get(key);
    if (relativo.isAbsolute()) throw new IllegalArgumentException("La clave EFS debe ser relativa");
    Path path = root.resolve(relativo).normalize();
    if (!path.startsWith(root))
      throw new IllegalArgumentException("La clave EFS sale del directorio permitido");
    return path;
  }
}
