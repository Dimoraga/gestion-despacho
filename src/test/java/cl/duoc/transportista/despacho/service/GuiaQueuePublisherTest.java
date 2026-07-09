package cl.duoc.transportista.despacho.service;

import cl.duoc.transportista.despacho.dto.GuiaColaErrorMensaje;
import cl.duoc.transportista.despacho.dto.GuiaColaMensaje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GuiaQueuePublisherTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    GuiaQueuePublisher publisher;
    GuiaColaMensaje mensaje;

    @BeforeEach
    void setUp() {
        publisher = new GuiaQueuePublisher(rabbitTemplate, "guias.exchange", "guias.routingkey", "guias.errores.routingkey");
        mensaje = new GuiaColaMensaje(1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", "key.pdf");
    }

    @Test
    void publicarGuia_envioExitoso_publicaSoloEnColaPrincipal() {
        publisher.publicarGuia(mensaje);

        verify(rabbitTemplate).convertAndSend("guias.exchange", "guias.routingkey", mensaje);
        verify(rabbitTemplate, never()).convertAndSend(eq("guias.exchange"), eq("guias.errores.routingkey"), any(Object.class));
    }

    @Test
    void publicarGuia_fallaColaPrincipal_publicaEnColaErrores() {
        doThrow(new AmqpException("broker caido"))
                .when(rabbitTemplate).convertAndSend("guias.exchange", "guias.routingkey", mensaje);

        publisher.publicarGuia(mensaje);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("guias.exchange"), eq("guias.errores.routingkey"), captor.capture());
        GuiaColaErrorMensaje error = (GuiaColaErrorMensaje) captor.getValue();
        assertEquals(mensaje, error.guia());
        assertEquals("broker caido", error.motivoError());
    }
}
