package cl.duoc.transportista.despacho.consumidor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import cl.duoc.transportista.despacho.consumidor.repository.SolicitudDespachoRepository;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GuiaRegistroPersistenceServiceTest {
  private final GuiaDespachoRegistroRepository repo = mock(GuiaDespachoRegistroRepository.class);
  private final SolicitudDespachoRepository solicitudes = mock(SolicitudDespachoRepository.class);
  private final GuiaRegistroPersistenceService service = new GuiaRegistroPersistenceService(repo, solicitudes);
  private final GuiaColaMensaje message = mensaje("11111111-1111-1111-1111-111111111111", null);

  @Test
  void creaPendienteConClaveDeterminista() {
    when(repo.findByPayloadHash(anyString())).thenReturn(Optional.empty());
    when(repo.siguienteNumeroGuia()).thenReturn(42L);
    when(solicitudes.findByRequestId(anyString())).thenReturn(Optional.empty());
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    GuiaDespachoRegistro r = service.preparar(message);
    assertThat(r.getEstado()).isEqualTo(EstadoProcesamiento.PENDING);
    assertThat(r.getArchivoKey()).isEqualTo("20260102/transporte/guia42.pdf");
  }

  @Test
  void rechazaMismoNumeroConPayloadDistinto() {
    GuiaDespachoRegistro r = new GuiaDespachoRegistro();
    r.setNumeroGuia(42L);
    r.setPayloadHash("otro");
    when(solicitudes.findByRequestId(anyString())).thenReturn(Optional.empty());
    when(repo.findByPayloadHash(anyString())).thenReturn(Optional.of(r));
    assertThatThrownBy(() -> service.preparar(message))
        .isInstanceOf(GuiaRegistroPersistenceService.PayloadConflictException.class);
  }

  @Test
  void normalizaTransportistaParaKeySegura() {
    assertThat(service.normalizarTransportista(" Transportes/Ñuble@CL "))
        .isEqualTo("transportes-nuble-cl");
  }

  @Test
  void rechazaVersionDeEventoNoSoportada() {
    GuiaColaMensaje incompatible =
        new GuiaColaMensaje(1, "11111111-1111-1111-1111-111111111111", "invalid", "transporte", LocalDate.of(2026, 1, 2),
            "Santiago", "PED-1");
    assertThatThrownBy(() -> service.preparar(incompatible))
        .isInstanceOf(GuiaRegistroPersistenceService.UnsupportedEventException.class);
  }

  @Test
  void rechazaFingerprintProvistoQueNoCoincide() {
    GuiaColaMensaje corrupto = mensaje("req-1", "no-es-el-hash");
    assertThatThrownBy(() -> service.preparar(corrupto))
        .isInstanceOf(GuiaRegistroPersistenceService.UnsupportedEventException.class);
  }

  @Test
  void duplicadoPorRequestIdDevuelveLaGuiaEnlazada() {
    SolicitudDespacho solicitud = new SolicitudDespacho();
    solicitud.setFingerprint(service.fingerprint(message));
    solicitud.setNumeroGuia(42L);
    GuiaDespachoRegistro guia = new GuiaDespachoRegistro();
    guia.setNumeroGuia(42L);
    when(solicitudes.findByRequestId(anyString())).thenReturn(Optional.of(solicitud));
    when(repo.findByNumeroGuia(42L)).thenReturn(Optional.of(guia));
    assertThat(service.preparar(message)).isSameAs(guia);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void completaSoloConElLeaseYFenceVigentes() {
    when(repo.reclamar(eq(42L), eq("lease-a"), any(), any())).thenReturn(1);
    when(repo.completarConFence(eq(42L), eq("lease-a"), eq(3L), eq("/efs/guia.pdf"), any()))
        .thenReturn(1);
    assertThat(service.reclamar(42L, "lease-a", Duration.ofSeconds(30))).isTrue();
    assertThat(service.completar(42L, "lease-a", 3L, "/efs/guia.pdf")).isTrue();
    when(repo.completarConFence(eq(42L), eq("lease-vencido"), eq(2L), any(), any())).thenReturn(0);
    assertThat(service.completar(42L, "lease-vencido", 2L, "/efs/guia.pdf")).isFalse();
  }

  @Test
  void registraRetrySoloParaElFenceQuePoseeElLease() {
    when(repo.marcarRetry(eq(42L), eq("lease-a"), eq(3L), contains("AccessDeniedException")))
        .thenReturn(1);
    assertThat(service.reintentar(42L, "lease-a", 3L, new java.nio.file.AccessDeniedException("/efs")))
        .isTrue();
    when(repo.marcarRetry(eq(42L), eq("lease-vencido"), eq(2L), anyString())).thenReturn(0);
    assertThat(service.reintentar(42L, "lease-vencido", 2L, new IllegalStateException("old")))
        .isFalse();
  }

  @Test
  void fingerprintCanonicoCoincideConVectorDoradoProductorV2() {
    GuiaColaMensaje evento =
        new GuiaColaMensaje(
            2,
            "req-unicode",
            "ignorado",
            "  TrＡnSpOrTe\u00a0  ÑuBle  ",
            LocalDate.of(2026, 1, 2),
            "  SANTIAGO\tCentro  ",
            " pe\u0301d-  42 ");
    assertThat(service.fingerprint(evento))
        .isEqualTo("4a510a090ea2784c2d8994986cb7e525898e93fe925c19cd0852d7f119c2ed0c");
  }

  private GuiaColaMensaje mensaje(String requestId, String providedFingerprint) {
    GuiaColaMensaje provisional = new GuiaColaMensaje(2, requestId, "x", "transporte",
        LocalDate.of(2026, 1, 2), "Santiago", "PED-1");
    return new GuiaColaMensaje(2, requestId,
        providedFingerprint == null ? service.fingerprint(provisional) : providedFingerprint,
        "transporte", LocalDate.of(2026, 1, 2), "Santiago", "PED-1");
  }
}
