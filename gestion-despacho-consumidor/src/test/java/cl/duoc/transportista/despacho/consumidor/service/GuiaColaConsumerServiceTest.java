package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaColaMensaje;
import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.model.GuiaDespachoRegistro;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuiaColaConsumerServiceTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    GuiaDespachoRegistroRepository repo;

    GuiaColaConsumerService service;

    @BeforeEach
    void setUp() {
        service = new GuiaColaConsumerService(rabbitTemplate, repo, "guias.queue");
    }

    @Test
    void consumirColaGuias_colaVacia_noGuardaNada() {
        when(rabbitTemplate.receiveAndConvert(any(String.class), any(ParameterizedTypeReference.class))).thenReturn(null);

        List<GuiaDespachoRegistroResponse> resultado = service.consumirColaGuias();

        assertEquals(0, resultado.size());
        verify(repo, never()).save(any());
    }

    @Test
    void consumirColaGuias_drenaMensajesYLosPersiste() {
        GuiaColaMensaje m1 = new GuiaColaMensaje(1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", "key1.pdf");
        GuiaColaMensaje m2 = new GuiaColaMensaje(2L, "transportistaY", LocalDate.of(2021, 3, 16), "Valparaiso", "PED-002", "key2.pdf");
        when(rabbitTemplate.receiveAndConvert(any(String.class), any(ParameterizedTypeReference.class)))
                .thenReturn(m1, m2, null);
        when(repo.save(any(GuiaDespachoRegistro.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GuiaDespachoRegistroResponse> resultado = service.consumirColaGuias();

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).numeroGuia());
        assertEquals(2L, resultado.get(1).numeroGuia());
        verify(repo, org.mockito.Mockito.times(2)).save(any(GuiaDespachoRegistro.class));
    }
}
