package cl.duoc.transportista.despacho.consumidor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GuiaRegistroPersistenceServiceTest {
  private final GuiaDespachoRegistroRepository repo=mock(GuiaDespachoRegistroRepository.class);
  private final GuiaRegistroPersistenceService service=new GuiaRegistroPersistenceService(repo);
  private final GuiaColaMensaje message=new GuiaColaMensaje(Integer.valueOf(1),42L,"transporte",LocalDate.of(2026,1,2),"Santiago","PED-1",null);
  @Test void creaPendienteConClaveDeterminista() { when(repo.findByNumeroGuia(42L)).thenReturn(Optional.empty()); when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0)); GuiaDespachoRegistro r=service.preparar(message); assertThat(r.getEstado()).isEqualTo(EstadoProcesamiento.PROCESSING); assertThat(r.getArchivoKey()).isEqualTo("20260102/transporte/guia42.pdf"); }
  @Test void rechazaMismoNumeroConPayloadDistinto() { GuiaDespachoRegistro r=new GuiaDespachoRegistro(); r.setNumeroGuia(42L); r.setPayloadHash("otro"); when(repo.findByNumeroGuia(42L)).thenReturn(Optional.of(r)); assertThatThrownBy(() -> service.preparar(message)).isInstanceOf(GuiaRegistroPersistenceService.PayloadConflictException.class); }
  @Test void normalizaTransportistaParaKeySegura() { assertThat(service.normalizarTransportista(" Transportes/Ñuble@CL ")).isEqualTo("transportes-nuble-cl"); }
  @Test void rechazaVersionDeEventoNoSoportada() { GuiaColaMensaje incompatible=new GuiaColaMensaje(Integer.valueOf(2),42L,"transporte",LocalDate.of(2026,1,2),"Santiago","PED-1",null); assertThatThrownBy(() -> service.preparar(incompatible)).isInstanceOf(GuiaRegistroPersistenceService.UnsupportedEventException.class); }
}
