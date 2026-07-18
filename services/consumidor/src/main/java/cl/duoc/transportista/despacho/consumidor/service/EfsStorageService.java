package cl.duoc.transportista.despacho.consumidor.service;

import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
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
    return guardarSiAusente(key, bytes, checksum(bytes));
  }

  public Path guardarSiAusente(String key, byte[] bytes, String checksum) {
    try {
      Path path = seguro(key);
      Files.createDirectories(path.getParent());
      Path temporal = Files.createTempFile(path.getParent(), ".guia-", ".tmp");
      try {
        Files.write(temporal, bytes, StandardOpenOption.TRUNCATE_EXISTING);
        try {
          // A hard link publishes the completed temp inode atomically and never replaces a target.
          Files.createLink(path, temporal);
        } catch (FileAlreadyExistsException exists) {
          adoptar(path, checksum);
        }
      } finally {
        Files.deleteIfExists(temporal);
      }
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

  public Path ruta(String key) {
    return seguro(key);
  }

  public byte[] leer(String key) {
    try { return Files.readAllBytes(seguro(key)); }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  private void adoptar(Path path, String esperado) throws IOException {
    if (!checksum(Files.readAllBytes(path)).equals(esperado))
      throw new IOException("Artefacto EFS existente con checksum incompatible: " + path);
  }

  private String checksum(byte[] bytes) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch (Exception e) { throw new IllegalStateException(e); }
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
