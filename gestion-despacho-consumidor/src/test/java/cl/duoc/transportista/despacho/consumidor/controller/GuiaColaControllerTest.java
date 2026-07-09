package cl.duoc.transportista.despacho.consumidor.controller;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.service.GuiaColaConsumerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuiaColaController.class)
class GuiaColaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GuiaColaConsumerService service;

    @Test
    void procesar_retornaListaDeRegistrosProcesados() throws Exception {
        GuiaDespachoRegistroResponse registro = new GuiaDespachoRegistroResponse(
                1L, 1L, "transportistaX", LocalDate.of(2021, 3, 15), "Santiago", "PED-001", "key1.pdf", Instant.now());
        when(service.consumirColaGuias()).thenReturn(List.of(registro));

        mockMvc.perform(post("/api/guias/cola/procesar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
