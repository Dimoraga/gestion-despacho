package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.*;
import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import cl.duoc.transportista.despacho.consumidor.repository.SolicitudDespachoRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuiaRegistroPersistenceService {
  public static final int VERSION_EVENTO = 2;
  private static final String FINGERPRINT_VERSION = "1";
  private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
  private final GuiaDespachoRegistroRepository repo;
  private final SolicitudDespachoRepository solicitudes;

  GuiaRegistroPersistenceService(
      GuiaDespachoRegistroRepository repo, SolicitudDespachoRepository solicitudes) {
    this.repo = repo;
    this.solicitudes = solicitudes;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public GuiaDespachoRegistro preparar(GuiaColaMensaje m) {
    validarEvento(m);
    String hash = fingerprint(m);
    if (!m.fingerprint().equals(hash)) throw new UnsupportedEventException();
    String requestId = m.requestId();
    Optional<SolicitudDespacho> solicitud = solicitudes.findByRequestId(requestId);
    if (solicitud.isPresent()) {
      if (!hash.equals(solicitud.get().getFingerprint())) throw new PayloadConflictException();
      return repo.findByNumeroGuia(solicitud.get().getNumeroGuia()).orElseThrow();
    }
    Optional<GuiaDespachoRegistro> existing = repo.findByPayloadHash(hash);
    if (existing.isPresent()) {
      GuiaDespachoRegistro r = existing.get();
      if (!hash.equals(r.getPayloadHash())) throw new PayloadConflictException();
      enlazarSolicitud(requestId, hash, r.getNumeroGuia());
      return r;
    }
    GuiaDespachoRegistro r = new GuiaDespachoRegistro();
    r.setNumeroGuia(repo.siguienteNumeroGuia());
    r.setTransportista(normalizarTransportista(m.transportista()));
    r.setFecha(m.fecha());
    r.setDestino(m.destino());
    r.setPedido(m.pedido());
    r.setPayloadHash(hash);
    r.setVersionEvento(m.version());
    r.setArchivoKey(key(r));
    r.setEstado(EstadoProcesamiento.PENDING);
    repo.saveAndFlush(r); // Unique races roll back this transaction and are retried by RabbitMQ.
    enlazarSolicitud(requestId, hash, r.getNumeroGuia());
    return r;
  }

  private void enlazarSolicitud(String requestId, String fingerprint, Long numeroGuia) {
    SolicitudDespacho s = new SolicitudDespacho();
    s.setRequestId(requestId);
    s.setFingerprint(fingerprint);
    s.setNumeroGuia(numeroGuia);
    s.setRecibidaEn(Instant.now());
    solicitudes.saveAndFlush(
        s); // Same rule: retry in a fresh transaction, never continue rollback-only.
  }

  @Transactional
  public boolean reclamar(Long id, String token, Duration lease) {
    Instant now = Instant.now();
    return repo.reclamar(id, token, now.plus(lease), now) == 1;
  }

  @Transactional
  public boolean completar(Long id, String token, Long fence, String efsPath) {
    return repo.completarConFence(id, token, fence, efsPath, Instant.now()) == 1;
  }

  @Transactional
  public boolean marcarFase(
      Long id,
      String token,
      Long fence,
      FaseProcesamiento anterior,
      FaseProcesamiento siguiente,
      String checksum) {
    return repo.marcarFase(id, token, fence, anterior, siguiente, checksum, Instant.now()) == 1;
  }

  @Transactional
  public boolean renovarLease(Long id, String token, Long fence, Duration lease) {
    Instant now = Instant.now();
    return repo.renovarLease(id, token, fence, now.plus(lease), now) == 1;
  }

  @Transactional
  public boolean reintentar(Long id, String token, Long fence, Exception error) {
    String detalle = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
    return repo.marcarRetry(
            id, token, fence, detalle.substring(0, Math.min(detalle.length(), 2000)))
        == 1;
  }

  @Transactional
  public boolean fallar(Long id, String token, Long fence, Exception error) {
    String detalle = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
    return repo.marcarFailed(
            id, token, fence, detalle.substring(0, Math.min(detalle.length(), 2000)))
        == 1;
  }

  @Transactional(readOnly = true)
  public List<GuiaDespachoRegistro> candidatos() {
    Instant now = Instant.now();
    return repo
        .findByEstadoIn(
            List.of(
                EstadoProcesamiento.PENDING,
                EstadoProcesamiento.RETRY,
                EstadoProcesamiento.PROCESSING))
        .stream()
        .filter(
            r ->
                (r.getEstado() == EstadoProcesamiento.RETRY
                        && (r.getRetryAt() == null || !r.getRetryAt().isAfter(now)))
                    || (r.getEstado() == EstadoProcesamiento.PROCESSING
                        && r.getLeaseExpiraEn() != null
                        && r.getLeaseExpiraEn().isBefore(now))
                    || r.getEstado() == EstadoProcesamiento.PENDING)
        .toList();
  }

  @Transactional(readOnly = true)
  public GuiaDespachoRegistro obtener(Long numero) {
    GuiaDespachoRegistro r =
        repo.findByNumeroGuia(numero)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guia no encontrada todavía"));
    if (r.isEliminada()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guia eliminada");
    return r;
  }

  @Transactional(readOnly = true)
  public SolicitudResponse consultarSolicitud(String requestId) {
    SolicitudDespacho solicitud =
        solicitudes
            .findByRequestId(requestId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no observada todavía"));
    GuiaDespachoRegistro guia = repo.findByNumeroGuia(solicitud.getNumeroGuia()).orElseThrow();
    return new SolicitudResponse(
        solicitud.getRequestId(),
        guia.getNumeroGuia(),
        guia.getEstado().name(),
        guia.getUltimoError());
  }

  @Transactional(readOnly = true)
  public List<GuiaDespachoRegistro> historial(String t, LocalDate f) {
    List<GuiaDespachoRegistro> xs =
        t != null && f != null
            ? repo.findByTransportistaAndFecha(normalizarTransportista(t), f)
            : t != null
                ? repo.findByTransportista(normalizarTransportista(t))
                : f != null ? repo.findByFecha(f) : repo.findAll();
    return xs.stream().filter(r -> !r.isEliminada()).toList();
  }

  @Transactional
  public GuiaDespachoRegistro actualizar(Long n, GuiaRequest q) {
    GuiaDespachoRegistro r = operable(n);
    r.setTransportista(normalizarTransportista(q.transportista()));
    r.setFecha(q.fecha());
    r.setDestino(q.destino());
    r.setPedido(q.pedido());
    r.setArchivoKey(key(r));
    return r;
  }

  @Transactional
  public GuiaDespachoRegistro eliminar(Long n) {
    GuiaDespachoRegistro r = operable(n);
    r.setEliminada(true);
    r.setEstado(EstadoProcesamiento.COMPLETED);
    return r;
  }

  @Transactional(readOnly = true)
  public GuiaDespachoRegistro operable(Long n) {
    GuiaDespachoRegistro r = obtener(n);
    if (r.getEstado() != EstadoProcesamiento.COMPLETED)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Guia en procesamiento");
    return r;
  }

  public String key(GuiaDespachoRegistro r) {
    return r.getFecha().format(KEY_DATE)
        + "/"
        + normalizarTransportista(r.getTransportista())
        + "/guia"
        + r.getNumeroGuia()
        + ".pdf";
  }

  public String normalizarTransportista(String value) {
    if (value == null) throw new IllegalArgumentException("Transportista requerido");
    String n =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}", "")
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
    if (!n.matches("[a-z0-9][a-z0-9_-]{0,62}"))
      throw new IllegalArgumentException("Transportista inválido");
    return n;
  }

  private void validarEvento(GuiaColaMensaje m) {
    if (m == null
        || m.version() == null
        || m.version() != VERSION_EVENTO
        || !uuidCanonico(m.requestId())
        || m.fingerprint() == null
        || !m.fingerprint().matches("[0-9a-f]{64}")
        || m.fecha() == null
        || fueraDeLimite(m.transportista(), 63)
        || fueraDeLimite(m.destino(), 255)
        || fueraDeLimite(m.pedido(), 255)) throw new UnsupportedEventException();
  }

  public String fingerprint(GuiaColaMensaje m) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      actualizarCampo(digest, FINGERPRINT_VERSION);
      actualizarCampo(digest, normalizarFingerprint(m.transportista(), false));
      actualizarCampo(digest, m.fecha().toString());
      actualizarCampo(digest, normalizarFingerprint(m.destino(), false));
      actualizarCampo(digest, normalizarFingerprint(m.pedido(), true));
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void actualizarCampo(MessageDigest digest, String campo) {
    byte[] bytes = campo.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }

  private String normalizarFingerprint(String value, boolean uppercase) {
    if (value == null) throw new UnsupportedEventException();
    String normalizado =
        Normalizer.normalize(value, Normalizer.Form.NFKC).strip().replaceAll("[\\s\\p{Z}]+", " ");
    return uppercase ? normalizado.toUpperCase(Locale.ROOT) : normalizado.toLowerCase(Locale.ROOT);
  }

  public static class PayloadConflictException extends RuntimeException {}

  public static class UnsupportedEventException extends RuntimeException {}

  private boolean uuidCanonico(String value) {
    try {
      return value != null && UUID.fromString(value).toString().equals(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean fueraDeLimite(String value, int maximo) {
    return value == null || value.length() > maximo;
  }
}
