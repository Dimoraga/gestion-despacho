package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaRequest;
import cl.duoc.transportista.despacho.dto.GuiaResponse;
import cl.duoc.transportista.despacho.exception.AccesoDenegadoException;
import cl.duoc.transportista.despacho.model.GuiaDespacho;
import cl.duoc.transportista.despacho.repository.GuiaDespachoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceImplTest {

    @Mock
    GuiaDespachoRepository repo;

    @Mock
    GuiaPdfService pdfService;

    @Mock
    S3StorageService s3;

    @TempDir
    Path tmpEfs;

    EfsStorageService efs;
    GuiaDespachoServiceImpl service;
    GuiaDespacho guia;

    @BeforeEach
    void setUp() {
        efs = new EfsStorageService();
        ReflectionTestUtils.setField(efs, "efsDir", tmpEfs.toString());
        service = new GuiaDespachoServiceImpl(repo, pdfService, s3, efs);

        guia = new GuiaDespacho();
        guia.setNumeroGuia(123L);
        guia.setTransportista("transportistaX");
        guia.setFecha(LocalDate.of(2021, 3, 15));
        guia.setDestino("Santiago");
        guia.setPedido("P-001");
    }

    @Test
    void subirAS3_guardaEnEfsYSubeAS3YDevuelveKey() throws Exception {
        when(repo.findById(123L)).thenReturn(Optional.of(guia));
        when(pdfService.generar(guia)).thenReturn(new byte[]{1, 2, 3});
        when(repo.save(any(GuiaDespacho.class))).thenReturn(guia);

        String key = service.subirAS3(123L);

        assertEquals("20210315/transportistaX/guia123.pdf", key);
        assertTrue(Files.exists(tmpEfs.resolve(key)));
        verify(s3).subir(eq(key), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void descargarDeS3_transportistaAjeno_lanzaAccesoDenegadoException() {
        guia.setArchivoKey("20210315/transportistaX/guia123.pdf");
        when(repo.findById(123L)).thenReturn(Optional.of(guia));

        assertThrows(AccesoDenegadoException.class, () ->
                service.descargarDeS3(123L, "otroTransportista"));
    }

    @Test
    void historial_ambosFiltros_usaFindByTransportistaAndFechaYDevuelveUno() {
        LocalDate fecha = LocalDate.of(2021, 3, 15);
        when(repo.findByTransportistaAndFecha("transportistaX", fecha)).thenReturn(List.of(guia));

        List<GuiaResponse> resultado = service.historial("transportistaX", fecha);

        assertEquals(1, resultado.size());
        verify(repo).findByTransportistaAndFecha("transportistaX", fecha);
        verify(repo, never()).findByTransportista(any());
        verify(repo, never()).findByFecha(any());
    }
}
