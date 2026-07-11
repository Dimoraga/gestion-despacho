package cl.duoc.transportista.despacho.consumidor.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.transportista.despacho.consumidor.dto.GuiaDespachoRegistroResponse;
import cl.duoc.transportista.despacho.consumidor.security.SecurityConfig;
import cl.duoc.transportista.despacho.consumidor.security.SecurityRoles;
import cl.duoc.transportista.despacho.consumidor.service.GuiaColaConsumerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GuiaColaController.class)
@Import(SecurityConfig.class)
class GuiaColaControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GuiaColaConsumerService service;

  @Test
  void procesar_retornaListaDeRegistrosProcesados() throws Exception {
    GuiaDespachoRegistroResponse registro =
        new GuiaDespachoRegistroResponse(
            1L,
            1L,
            "transportistaX",
            LocalDate.of(2021, 3, 15),
            "Santiago",
            "PED-001",
            "key1.pdf",
            Instant.now());
    when(service.consumirColaGuias()).thenReturn(List.of(registro));

    mockMvc
        .perform(
            post("/api/guias/cola/procesar")
                .with(
                    jwt()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_" + SecurityRoles.GESTION_GUIAS))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void procesar_sinAutenticacion_retorna401() throws Exception {
    mockMvc.perform(post("/api/guias/cola/procesar")).andExpect(status().isUnauthorized());
  }
}
