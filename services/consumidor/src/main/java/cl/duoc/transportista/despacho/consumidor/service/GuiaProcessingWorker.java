package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.model.FaseProcesamiento;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Performs side effects outside the AMQP transaction; the lease fence owns completion. */
@Component
public class GuiaProcessingWorker {
  private final GuiaRegistroPersistenceService registros;
  private final GuiaPdfService pdf;
  private final EfsStorageService efs;
  private final S3StorageService s3;
  private final Duration lease;
  private final int maxRetries;

  GuiaProcessingWorker(
      GuiaRegistroPersistenceService registros,
      GuiaPdfService pdf,
      EfsStorageService efs,
      S3StorageService s3,
      @Value("${app.processing-lease-seconds:60}") long leaseSeconds,
      @Value("${app.processing-max-retries:5}") int maxRetries) {
    this.registros = registros;
    this.pdf = pdf;
    this.efs = efs;
    this.s3 = s3;
    this.lease = Duration.ofSeconds(leaseSeconds);
    this.maxRetries = maxRetries;
  }

  @Scheduled(fixedDelayString = "${app.processing-worker-ms:5000}")
  public void process() {
    registros
        .candidatos()
        .forEach(
            candidate -> {
              try {
                processOne(candidate);
              } catch (Exception ignored) {
                // A malformed candidate cannot terminate the scheduler loop.
              }
            });
  }

  void processOne(GuiaDespachoRegistro candidate) {
    String token = UUID.randomUUID().toString();
    if (!registros.reclamar(candidate.getId(), token, lease)) return;
    GuiaDespachoRegistro r = null;
    try {
      r = registros.obtener(candidate.getNumeroGuia());
      FaseProcesamiento fase = r.getFase();
      String checksum = r.getChecksum();
      var path = efs.ruta(r.getArchivoKey());
      if (fase == FaseProcesamiento.PENDING) {
        if (!registros.renovarLease(r.getId(), token, r.getFence(), lease)) return;
        byte[] bytes = pdf.generar(r);
        checksum = checksum(bytes);
        path = efs.guardarSiAusente(r.getArchivoKey(), bytes, checksum);
        if (!registros.marcarFase(
            r.getId(),
            token,
            r.getFence(),
            FaseProcesamiento.PENDING,
            FaseProcesamiento.EFS_WRITTEN,
            checksum)) return;
        fase = FaseProcesamiento.EFS_WRITTEN;
      }
      if (fase == FaseProcesamiento.EFS_WRITTEN) {
        if (!registros.renovarLease(r.getId(), token, r.getFence(), lease)) return;
        byte[] bytes = efs.leer(r.getArchivoKey());
        if (!checksum(bytes).equals(checksum))
          throw new IllegalStateException("Checksum EFS incompatible");
        s3.subirSiAusente(r.getArchivoKey(), bytes, checksum);
        if (!registros.marcarFase(
            r.getId(),
            token,
            r.getFence(),
            FaseProcesamiento.EFS_WRITTEN,
            FaseProcesamiento.S3_WRITTEN,
            checksum)) return;
      }
      registros.completar(r.getId(), token, r.getFence(), path.toString());
    } catch (Exception error) {
      Long fence = r == null ? candidate.getFence() : r.getFence();
      if (terminal(error) || (r != null && r.getRetryCount() >= maxRetries))
        registros.fallar(candidate.getId(), token, fence, error);
      else registros.reintentar(candidate.getId(), token, fence, error);
    }
  }

  private String checksum(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean terminal(Exception error) {
    if (error instanceof IllegalArgumentException || error instanceof java.io.UncheckedIOException)
      return true;
    return error instanceof S3Exception s && s.statusCode() >= 400 && s.statusCode() < 500;
  }
}
