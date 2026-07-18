package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Canonical fingerprint shared with the consumer. It hashes v1, transportista, fecha, destino and
 * pedido in that order; every UTF-8 field is prefixed by its four-byte big-endian length.
 */
final class GuiaFingerprint {
  private static final String VERSION = "1";

  private GuiaFingerprint() {}

  static String calcular(GuiaRequest request) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      actualizar(digest, VERSION);
      actualizar(digest, normalizar(request.transportista()).toLowerCase(Locale.ROOT));
      actualizar(digest, DateTimeFormatter.ISO_LOCAL_DATE.format(request.fecha()));
      actualizar(digest, normalizar(request.destino()).toLowerCase(Locale.ROOT));
      actualizar(digest, normalizar(request.pedido()).toUpperCase(Locale.ROOT));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no está disponible", ex);
    }
  }

  private static void actualizar(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }

  private static String normalizar(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .strip()
        .replaceAll("[\\s\\p{Z}]+", " ");
  }
}
