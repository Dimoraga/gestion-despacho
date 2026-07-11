package cl.duoc.transportista.despacho.consumidor.service;
import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class GuiaRegistroPersistenceServiceTest {
 @Mock GuiaDespachoRegistroRepository repo;
 @Test void existenteSeBuscaEnTransaccionNuevaSinDuplicar(){GuiaDespachoRegistro registro=new GuiaDespachoRegistro();registro.setNumeroGuia(7L);when(repo.findByNumeroGuia(7L)).thenReturn(Optional.of(registro));assertSame(registro,new GuiaRegistroPersistenceService(repo).buscar(7L).orElseThrow());verify(repo,never()).saveAndFlush(any());}
}
