package cl.duoc.transportista.despacho.consumidor.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.model.FaseProcesamiento;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import java.nio.file.AccessDeniedException;
import org.junit.jupiter.api.Test;

class GuiaProcessingWorkerTest {
  private final GuiaRegistroPersistenceService registros =
      mock(GuiaRegistroPersistenceService.class);
  private final GuiaPdfService pdf = mock(GuiaPdfService.class);
  private final EfsStorageService efs = mock(EfsStorageService.class);
  private final S3StorageService s3 = mock(S3StorageService.class);
  private final GuiaProcessingWorker worker =
      new GuiaProcessingWorker(registros, pdf, efs, s3, 60, 5);

  @Test
  void errorDeEfsMarcaRetryYNoEscapaDelWorker() {
    GuiaDespachoRegistro guia = guia();
    when(registros.reclamar(eq(7L), anyString(), any())).thenReturn(true);
    when(registros.obtener(42L)).thenReturn(guia);
    when(registros.renovarLease(eq(7L), anyString(), eq(4L), any())).thenReturn(true);
    when(pdf.generar(guia)).thenReturn(new byte[] {1});
    doThrow(new java.io.UncheckedIOException(new AccessDeniedException("/efs/guia42.pdf")))
        .when(efs)
        .guardarSiAusente(anyString(), any(), anyString());

    worker.processOne(guia);

    verify(registros).fallar(eq(7L), anyString(), eq(4L), isA(Exception.class));
    verify(registros, never()).completar(anyLong(), anyString(), anyLong(), anyString());
  }

  @Test
  void faseEfsWrittenLeeArtefactoYNoRegeneraPdf() throws Exception {
    GuiaDespachoRegistro guia = guia();
    guia.setFase(FaseProcesamiento.EFS_WRITTEN);
    byte[] bytes = new byte[] {1, 2, 3};
    guia.setChecksum(
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
    when(registros.reclamar(eq(7L), anyString(), any())).thenReturn(true);
    when(registros.obtener(42L)).thenReturn(guia);
    when(registros.renovarLease(eq(7L), anyString(), eq(4L), any())).thenReturn(true);
    when(efs.leer(guia.getArchivoKey())).thenReturn(bytes);
    when(registros.marcarFase(
            eq(7L),
            anyString(),
            eq(4L),
            eq(FaseProcesamiento.EFS_WRITTEN),
            eq(FaseProcesamiento.S3_WRITTEN),
            anyString()))
        .thenReturn(true);

    worker.processOne(guia);

    verify(s3).subirSiAusente(eq(guia.getArchivoKey()), eq(bytes), eq(guia.getChecksum()));
    verifyNoInteractions(pdf);
  }

  private GuiaDespachoRegistro guia() {
    GuiaDespachoRegistro guia = new GuiaDespachoRegistro();
    guia.setId(7L);
    guia.setNumeroGuia(42L);
    guia.setFence(4L);
    guia.setFase(FaseProcesamiento.PENDING);
    guia.setArchivoKey("20260102/transporte/guia42.pdf");
    return guia;
  }
}
