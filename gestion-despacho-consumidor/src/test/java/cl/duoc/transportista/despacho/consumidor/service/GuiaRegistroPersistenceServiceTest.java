package cl.duoc.transportista.despacho.consumidor.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiaRegistroPersistenceServiceTest {
  @Mock GuiaDespachoRegistroRepository repo;

  @Test
  void existenteSeBuscaEnTransaccionNuevaSinDuplicar() {
    GuiaDespachoRegistro registro = new GuiaDespachoRegistro();
    registro.setNumeroGuia(7L);
    when(repo.findByNumeroGuia(7L)).thenReturn(Optional.of(registro));
    assertSame(
        registro, new GuiaRegistroPersistenceService(repo).buscarPorNumeroGuia(7L).orElseThrow());
    verify(repo, never()).saveAndFlush(any());
  }
}
