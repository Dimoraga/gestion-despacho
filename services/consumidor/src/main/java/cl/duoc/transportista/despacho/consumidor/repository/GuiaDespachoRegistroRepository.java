package cl.duoc.transportista.despacho.consumidor.repository;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuiaDespachoRegistroRepository extends JpaRepository<GuiaDespachoRegistro, Long> {
  Optional<GuiaDespachoRegistro> findByNumeroGuia(Long numeroGuia);

  Optional<GuiaDespachoRegistro> findByPayloadHash(String payloadHash);

  @Query(value = "select guia_numero_seq.nextval from dual", nativeQuery = true)
  Long siguienteNumeroGuia();

  @Modifying
  @Query(
      "update GuiaDespachoRegistro g set g.estado = 'PROCESSING', g.leaseToken = :token, g.leaseExpiraEn = :expires, g.fence = g.fence + 1, g.fechaInicioProcesamiento = :now where g.id = :id and g.eliminada = false and ((g.estado in ('PENDING', 'RETRY') and (g.retryAt is null or g.retryAt <= :now)) or (g.estado = 'PROCESSING' and g.leaseExpiraEn < :now))")
  int reclamar(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("expires") Instant expires,
      @Param("now") Instant now);

  @Modifying
  @Query(
      "update GuiaDespachoRegistro g set g.estado = 'COMPLETED', g.fase = 'COMPLETED', g.efsPath = :path, g.fechaProcesado = :now, g.leaseToken = null, g.leaseExpiraEn = null where g.id = :id and g.estado = 'PROCESSING' and g.fase = 'S3_WRITTEN' and g.leaseToken = :token and g.fence = :fence and g.leaseExpiraEn > :now")
  int completarConFence(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("fence") Long fence,
      @Param("path") String path,
      @Param("now") Instant now);

  @Modifying
  @Query(
      "update GuiaDespachoRegistro g set g.fase = :siguiente, g.checksum = :checksum where g.id = :id and g.estado = 'PROCESSING' and g.fase = :anterior and g.leaseToken = :token and g.fence = :fence and g.leaseExpiraEn > :now")
  int marcarFase(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("fence") Long fence,
      @Param("anterior") cl.duoc.transportista.despacho.consumidor.model.FaseProcesamiento anterior,
      @Param("siguiente")
          cl.duoc.transportista.despacho.consumidor.model.FaseProcesamiento siguiente,
      @Param("checksum") String checksum,
      @Param("now") Instant now);

  @Modifying
  @Query(
      "update GuiaDespachoRegistro g set g.leaseExpiraEn = :expires where g.id = :id and g.estado = 'PROCESSING' and g.leaseToken = :token and g.fence = :fence and g.leaseExpiraEn > :now")
  int renovarLease(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("fence") Long fence,
      @Param("expires") Instant expires,
      @Param("now") Instant now);

  @Modifying
  @Query(
      value =
          "update guia_despacho_registro set estado = 'RETRY', ultimo_error = :error, retry_count = retry_count + 1, retry_at = systimestamp + numtodsinterval(power(2, least(retry_count, 6)), 'SECOND'), lease_token = null, lease_expira_en = null where id = :id and lease_token = :token and fence = :fence",
      nativeQuery = true)
  int marcarRetry(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("fence") Long fence,
      @Param("error") String error);

  @Modifying
  @Query(
      value =
          "update guia_despacho_registro set estado = 'FAILED', ultimo_error = :error, lease_token = null, lease_expira_en = null where id = :id and estado = 'PROCESSING' and lease_token = :token and fence = :fence",
      nativeQuery = true)
  int marcarFailed(
      @Param("id") Long id,
      @Param("token") String token,
      @Param("fence") Long fence,
      @Param("error") String error);

  List<GuiaDespachoRegistro> findByEstadoAndFechaInicioProcesamientoBefore(
      cl.duoc.transportista.despacho.consumidor.model.EstadoProcesamiento estado, Instant limite);

  List<GuiaDespachoRegistro> findByEstadoIn(
      java.util.Collection<cl.duoc.transportista.despacho.consumidor.model.EstadoProcesamiento>
          estados);

  List<GuiaDespachoRegistro> findByTransportista(String transportista);

  List<GuiaDespachoRegistro> findByFecha(java.time.LocalDate fecha);

  List<GuiaDespachoRegistro> findByTransportistaAndFecha(
      String transportista, java.time.LocalDate fecha);
}
